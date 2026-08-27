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

    suspend fun setFallbackOrder(order: List<AiProviderType>)
    suspend fun setWatchCountry(code: String)
    suspend fun markDiscoverIntroSeen()
}
