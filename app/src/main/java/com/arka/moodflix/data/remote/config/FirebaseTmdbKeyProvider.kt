package com.arka.moodflix.data.remote.config

import com.arka.moodflix.BuildConfig
import com.arka.moodflix.domain.repository.TmdbKeyProvider
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await

/**
 * The TMDB key lives in Firebase Remote Config (parameter [KEY]) instead of
 * being hardcoded in the APK. [BuildConfig.TMDB_API_KEY] (from local.properties)
 * is only the in-app default used until the first successful fetch, and as a
 * safety net if Remote Config is ever unreachable.
 *
 * [getKey] is called on every TMDB request, so the resolved value is cached
 * in memory for [CACHE_TTL_MILLIS] - Remote Config's own throttling already
 * limits *network* fetches, but there's no reason to pay a Task/suspend hop
 * on every single request on top of that. The TTL mirrors the release build's
 * `minimumFetchIntervalInSeconds`, so a console-side key rotation is still
 * picked up on roughly the same cadence as before this cache was added.
 */
class FirebaseTmdbKeyProvider(
    private val remoteConfig: FirebaseRemoteConfig
) : TmdbKeyProvider {

    private val cacheMutex = Mutex()
    private var cachedKey: String? = null
    private var cachedAtMillis: Long = 0

    /** Call right after login so the key is warm before the first TMDB request. */
    suspend fun refresh() {
        runCatching { remoteConfig.fetchAndActivate().await() }
        cacheMutex.withLock {
            cachedKey = remoteConfig.getString(KEY).ifBlank { BuildConfig.TMDB_API_KEY }
            cachedAtMillis = System.currentTimeMillis()
        }
    }

    override suspend fun getKey(): String {
        cacheMutex.withLock {
            if (cachedKey != null && System.currentTimeMillis() - cachedAtMillis < CACHE_TTL_MILLIS) {
                return cachedKey!!
            }
        }
        refresh()
        return cacheMutex.withLock { cachedKey } ?: BuildConfig.TMDB_API_KEY
    }

    companion object {
        const val KEY = "tmdb_api_key"
        private const val CACHE_TTL_MILLIS = 3_600_000L
    }
}
