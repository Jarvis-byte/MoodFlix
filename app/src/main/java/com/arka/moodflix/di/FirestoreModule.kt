package com.arka.moodflix.di

import com.arka.moodflix.data.repository.FirestoreAdsPreferenceRepository
import com.arka.moodflix.data.repository.FirestoreWatchlistRepository
import com.arka.moodflix.domain.repository.AdsPreferenceRepository
import com.arka.moodflix.domain.repository.AuthRepository
import com.arka.moodflix.domain.repository.WatchlistRepository
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FirestoreModule {

    @Provides
    @Singleton
    fun provideFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides
    @Singleton
    fun provideAdsPreferenceRepository(
        firestore: FirebaseFirestore,
        authRepository: AuthRepository
    ): AdsPreferenceRepository = FirestoreAdsPreferenceRepository(firestore, authRepository)

    @Provides
    @Singleton
    fun provideWatchlistRepository(
        firestore: FirebaseFirestore,
        authRepository: AuthRepository
    ): WatchlistRepository = FirestoreWatchlistRepository(firestore, authRepository)
}
