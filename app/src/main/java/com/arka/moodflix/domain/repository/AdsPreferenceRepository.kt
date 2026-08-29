package com.arka.moodflix.domain.repository

interface AdsPreferenceRepository {
    /** Whether the signed-in user should see ads. True (ads on) if unset or unreachable. */
    suspend fun isAdsEnabledForCurrentUser(): Boolean
}
