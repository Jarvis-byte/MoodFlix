package com.arka.moodflix.data.repository

import com.arka.moodflix.domain.repository.AdsPreferenceRepository
import com.arka.moodflix.domain.repository.AuthRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await

/**
 * Per-user ads on/off flag, stored at users/{uid}.adsEnabled in Firestore.
 * Missing document, missing field, no signed-in user, or a read failure all
 * fall back to ads-on - Firestore is a kill switch you opt users *out* with,
 * not a gate they need to be explicitly opted into.
 *
 * Cached in memory per uid: [RewardedAdManager] checks this on every preload
 * and every show, so re-reading Firestore each time would multiply reads
 * across a large user base for a value that only the console ever changes.
 */
class FirestoreAdsPreferenceRepository(
    private val firestore: FirebaseFirestore,
    private val authRepository: AuthRepository
) : AdsPreferenceRepository {

    private val cacheMutex = Mutex()
    private var cachedUid: String? = null
    private var cachedValue: Boolean = true

    override suspend fun isAdsEnabledForCurrentUser(): Boolean {
        val uid = authRepository.currentUser?.uid ?: return true

        cacheMutex.withLock {
            if (cachedUid == uid) return cachedValue
        }

        val value = runCatching {
            firestore.collection(USERS_COLLECTION)
                .document(uid)
                .get()
                .await()
                .getBoolean(ADS_ENABLED_FIELD) ?: true
        }.getOrDefault(true)

        cacheMutex.withLock {
            cachedUid = uid
            cachedValue = value
        }
        return value
    }

    companion object {
        const val USERS_COLLECTION = "users"
        const val ADS_ENABLED_FIELD = "adsEnabled"
    }
}
