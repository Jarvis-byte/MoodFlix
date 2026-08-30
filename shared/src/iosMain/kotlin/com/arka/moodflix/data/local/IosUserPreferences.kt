package com.arka.moodflix.data.local

import com.arka.moodflix.domain.model.AiProviderType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow
import platform.Foundation.NSUserDefaults

/**
 * iOS implementation backed by NSUserDefaults - the Android counterpart to
 * DataStore. NSUserDefaults has no native Kotlin Flow, so writes are nudged
 * through a MutableStateFlow the same way AiKeyRepositoryImpl already nudges
 * around EncryptedSharedPreferences not being observable either.
 */
class IosUserPreferences : UserPreferences {

    private val defaults = NSUserDefaults.standardUserDefaults
    private val orderKey = "fallback_order"
    private val countryKey = "watch_country"
    private val introSeenKey = "discover_intro_seen"
    private val darkThemeKey = "dark_theme_enabled"

    private val orderFlow = MutableStateFlow(readOrder())
    private val countryFlow = MutableStateFlow(readCountry())
    private val introSeenFlow = MutableStateFlow(defaults.boolForKey(introSeenKey))
    private val darkThemeFlow = MutableStateFlow(defaults.objectForKey(darkThemeKey)?.let { defaults.boolForKey(darkThemeKey) } ?: true)

    override val fallbackOrder: Flow<List<AiProviderType>> = orderFlow
    override val watchCountry: Flow<String> = countryFlow
    override val discoverIntroSeen: Flow<Boolean> = introSeenFlow
    override val darkThemeEnabled: Flow<Boolean> = darkThemeFlow

    override suspend fun setFallbackOrder(order: List<AiProviderType>) {
        defaults.setObject(order.joinToString(",") { it.name }, orderKey)
        orderFlow.value = order
    }

    override suspend fun setWatchCountry(code: String) {
        val upper = code.uppercase()
        defaults.setObject(upper, countryKey)
        countryFlow.value = upper
    }

    override suspend fun markDiscoverIntroSeen() {
        defaults.setBool(true, introSeenKey)
        introSeenFlow.value = true
    }

    override suspend fun setDarkThemeEnabled(enabled: Boolean) {
        defaults.setBool(enabled, darkThemeKey)
        darkThemeFlow.value = enabled
    }

    private fun readOrder(): List<AiProviderType> =
        (defaults.stringForKey(orderKey))
            ?.split(",")
            ?.mapNotNull { name -> AiProviderType.entries.firstOrNull { it.name == name } }
            ?.takeIf { it.isNotEmpty() }
            ?: AiProviderType.entries.toList()

    private fun readCountry(): String =
        defaults.stringForKey(countryKey) ?: "IN"
}
