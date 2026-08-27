package com.arka.moodflix.data.local

import com.arka.moodflix.domain.model.AiProviderType

/**
 * API keys are encrypted at rest and never leave the device. There is
 * deliberately no backend and no analytics on this - other people's
 * credentials are not our liability to hold.
 *
 * Android implements this with EncryptedSharedPreferences (Android Keystore
 * backed); iOS implements it with the Keychain. The constructors differ per
 * platform (Android needs a Context, iOS doesn't), so this is a plain common
 * interface rather than an expect/actual class - each platform's own DI
 * wires up the right implementation.
 */
interface SecureKeyStore {
    fun put(type: AiProviderType, key: String)
    fun get(type: AiProviderType): String?
    fun remove(type: AiProviderType)
    fun configuredTypes(): Set<AiProviderType>
}
