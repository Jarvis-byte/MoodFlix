package com.arka.moodflix.data.local

import com.arka.moodflix.domain.model.AiProviderType
import kotlinx.coroutines.flow.Flow

/**
 * Small, non-sensitive user settings. Android implements this with
 * DataStore; iOS implements it with NSUserDefaults.
 */
interface UserPreferences {
    val fallbackOrder: Flow<List<AiProviderType>>

    /** Drives the "where to watch" lookup. Defaults to the device region. */
    val watchCountry: Flow<String>

    /** Whether the Discover screen's one-time coach-mark tooltip has been shown. */
    val discoverIntroSeen: Flow<Boolean>

    /**
     * App-wide theme toggle, overriding the system setting. Defaults to
     * whatever the device's dark/light mode is at first read, so there's no
     * flash of the "wrong" theme before the user has ever touched the toggle.
     */
    val darkThemeEnabled: Flow<Boolean>

    suspend fun setFallbackOrder(order: List<AiProviderType>)
    suspend fun setWatchCountry(code: String)
    suspend fun markDiscoverIntroSeen()
    suspend fun setDarkThemeEnabled(enabled: Boolean)
}
