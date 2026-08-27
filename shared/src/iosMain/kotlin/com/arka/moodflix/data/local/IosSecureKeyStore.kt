package com.arka.moodflix.data.local

import com.arka.moodflix.domain.model.AiProviderType
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFDictionarySetValue
import platform.CoreFoundation.CFMutableDictionaryRef
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFTypeRef
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFBooleanTrue
import platform.CoreFoundation.kCFTypeDictionaryKeyCallBacks
import platform.CoreFoundation.kCFTypeDictionaryValueCallBacks
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.SecItemUpdate
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleAfterFirstUnlock
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData

/**
 * iOS implementation backed by the Keychain - the counterpart to Android's
 * EncryptedSharedPreferences/Android Keystore. Each key is stored as a
 * generic-password item scoped to this app's service name, with account =
 * the provider type name (mirrors AndroidSecureKeyStore's per-type entries).
 *
 * Builds queries with genuine CoreFoundation dictionaries (CFDictionaryCreateMutable)
 * rather than Foundation's NSMutableDictionary. A Kotlin-constructed
 * NSMutableDictionary is not reliably castable to a raw CFDictionaryRef
 * pointer at runtime (confirmed the hard way: it crashes with a
 * ClassCastException on kotlin.native.internal.NSDictionaryAsKMap) - staying
 * in the CF world end-to-end sidesteps that entirely.
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class IosSecureKeyStore : SecureKeyStore {

    private val service = "com.arka.moodflix.aikeys"

    override fun put(type: AiProviderType, key: String) {
        val data = stringToData(key.trim())

        if (get(type) != null) {
            withCFDictionary { query ->
                fillBaseQuery(query, type)
                withCFDictionary { attributesToUpdate ->
                    setValue(attributesToUpdate, kSecValueData, data)
                    SecItemUpdate(query, attributesToUpdate)
                }
            }
        } else {
            withCFDictionary { attributes ->
                fillBaseQuery(attributes, type)
                setValue(attributes, kSecValueData, data)
                setValue(attributes, kSecAttrAccessible, kSecAttrAccessibleAfterFirstUnlock)
                SecItemAdd(attributes, null)
            }
        }
    }

    override fun get(type: AiProviderType): String? = memScoped {
        val result = alloc<CFTypeRefVar>()
        val status = withCFDictionary { lookup ->
            fillBaseQuery(lookup, type)
            setValue(lookup, kSecReturnData, kCFBooleanTrue)
            setValue(lookup, kSecMatchLimit, kSecMatchLimitOne)
            SecItemCopyMatching(lookup, result.ptr)
        }
        if (status != errSecSuccess) return@memScoped null

        // SecItemCopyMatching follows the Copy/Create rule - we own a +1
        // reference to the result, so CFBridgingRelease both bridges it into
        // a real Kotlin NSData wrapper and correctly transfers that
        // ownership into ARC (unlike a raw `as? NSData` cast, which fails on
        // the bare CFTypeRef pointer for the same reason the kSecXxx key
        // constants needed CFBridgingRelease above).
        val data = CFBridgingRelease(result.value) as? NSData ?: return@memScoped null
        NSString.create(data = data, encoding = NSUTF8StringEncoding) as String?
    }

    override fun remove(type: AiProviderType) {
        withCFDictionary { query ->
            fillBaseQuery(query, type)
            SecItemDelete(query)
        }
    }

    override fun configuredTypes(): Set<AiProviderType> =
        AiProviderType.entries.filter { !get(it).isNullOrBlank() }.toSet()

    private fun fillBaseQuery(dict: CFMutableDictionaryRef, type: AiProviderType) {
        setValue(dict, kSecClass, kSecClassGenericPassword)
        setValue(dict, kSecAttrService, service)
        setValue(dict, kSecAttrAccount, type.name)
    }

    private fun createMutableDictionary(): CFMutableDictionaryRef = memScoped {
        CFDictionaryCreateMutable(
            null,
            0,
            kCFTypeDictionaryKeyCallBacks.ptr,
            kCFTypeDictionaryValueCallBacks.ptr
        )!!
    }

    /** Builds a fresh mutable CF dictionary, hands it to [block], then releases it. */
    private inline fun <R> withCFDictionary(block: (CFMutableDictionaryRef) -> R): R {
        val dict = createMutableDictionary()
        try {
            return block(dict)
        } finally {
            CFRelease(dict)
        }
    }

    /** Inserts a Kotlin String value, bridged to a retained CFStringRef the dictionary then owns. */
    private fun setValue(dict: CFMutableDictionaryRef, key: CFTypeRef?, value: String) {
        val bridged = CFBridgingRetain(value as NSString)
        CFDictionarySetValue(dict, key, bridged)
        CFRelease(bridged)
    }

    /**
     * Inserts a raw CFTypeRef constant as-is (e.g. kSecClassGenericPassword,
     * kCFBooleanTrue) - these are immortal globals. Security's kSecReturnData
     * et al. specifically require the real CFBoolean singleton here, not a
     * truthy NSNumber - it rejects that with an opaque OSStatus -50
     * ("add_return: value 1 is not CFBoolean") that only shows up in the
     * system log, not as a Kotlin/Swift-visible error.
     */
    private fun setValue(dict: CFMutableDictionaryRef, key: CFTypeRef?, value: CFTypeRef?) {
        CFDictionarySetValue(dict, key, value)
    }

    /** Inserts NSData - same ownership pattern, no bridging needed since it's already an ObjC object. */
    private fun setValue(dict: CFMutableDictionaryRef, key: CFTypeRef?, value: NSData) {
        val bridged = CFBridgingRetain(value)
        CFDictionarySetValue(dict, key, bridged)
        CFRelease(bridged)
    }

    private fun stringToData(value: String): NSData =
        (value as NSString).dataUsingEncoding(NSUTF8StringEncoding)!!
}
