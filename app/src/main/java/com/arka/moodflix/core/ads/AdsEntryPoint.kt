package com.arka.moodflix.core.ads

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Lets plain @Composable functions reach [RewardedAdManager] without an
 * Activity-scoped ViewModel - the manager needs the Activity itself to
 * show the ad, which a ViewModel can't hold.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface RewardedAdManagerEntryPoint {
    fun rewardedAdManager(): RewardedAdManager
}
