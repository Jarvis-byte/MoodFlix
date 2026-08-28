package com.arka.moodflix.data.remote.config

import com.arka.moodflix.BuildConfig
import com.arka.moodflix.domain.repository.TmdbKeyProvider
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import kotlinx.coroutines.tasks.await

/**
 * The TMDB key lives in Firebase Remote Config (parameter [KEY]) instead of
 * being hardcoded in the APK. [BuildConfig.TMDB_API_KEY] (from local.properties)
 * is only the in-app default used until the first successful fetch, and as a
 * safety net if Remote Config is ever unreachable.
 */
class FirebaseTmdbKeyProvider(
    private val remoteConfig: FirebaseRemoteConfig
) : TmdbKeyProvider {

    /** Call right after login so the key is warm before the first TMDB request. */
    suspend fun refresh() {
        runCatching { remoteConfig.fetchAndActivate().await() }
    }

    override suspend fun getKey(): String {
        refresh()
        return remoteConfig.getString(KEY).ifBlank { BuildConfig.TMDB_API_KEY }
    }

    companion object {
        const val KEY = "tmdb_api_key"
    }
}
