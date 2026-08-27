package com.arka.moodflix.di

import android.content.Context
import com.arka.moodflix.data.local.AndroidSecureKeyStore
import com.arka.moodflix.data.local.AndroidUserPreferences
import com.arka.moodflix.data.local.SecureKeyStore
import com.arka.moodflix.data.local.UserPreferences
import com.arka.moodflix.data.repository.AiKeyRepositoryImpl
import com.arka.moodflix.domain.repository.AiKeyRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Wires the Android implementations of the shared KMP module's platform
 * interfaces. The iOS app wires the iOS implementations (IosSecureKeyStore,
 * IosUserPreferences) the same way, with its own DI.
 */
@Module
@InstallIn(SingletonComponent::class)
object PlatformModule {

    @Provides
    @Singleton
    fun provideSecureKeyStore(@ApplicationContext context: Context): SecureKeyStore =
        AndroidSecureKeyStore(context)

    @Provides
    @Singleton
    fun provideUserPreferences(@ApplicationContext context: Context): UserPreferences =
        AndroidUserPreferences(context)

    @Provides
    @Singleton
    fun provideAiKeyRepository(
        keyStore: SecureKeyStore,
        prefs: UserPreferences
    ): AiKeyRepository = AiKeyRepositoryImpl(keyStore, prefs)
}
