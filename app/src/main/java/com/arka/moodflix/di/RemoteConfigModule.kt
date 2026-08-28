package com.arka.moodflix.di

import com.arka.moodflix.BuildConfig
import com.arka.moodflix.data.remote.config.FirebaseTmdbKeyProvider
import com.arka.moodflix.domain.repository.TmdbKeyProvider
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RemoteConfigModule {

    @Provides
    @Singleton
    fun provideFirebaseRemoteConfig(): FirebaseRemoteConfig {
        val remoteConfig = FirebaseRemoteConfig.getInstance()
        val settings = FirebaseRemoteConfigSettings.Builder()
            // Debug builds fetch every time so a key change in the console
            // shows up without waiting out the cache; release builds use a
            // 1h floor since the key rarely changes.
            .setMinimumFetchIntervalInSeconds(if (BuildConfig.DEBUG) 0 else 3600)
            .build()
        remoteConfig.setConfigSettingsAsync(settings)
        remoteConfig.setDefaultsAsync(
            mapOf(FirebaseTmdbKeyProvider.KEY to BuildConfig.TMDB_API_KEY)
        )
        return remoteConfig
    }

    @Provides
    @Singleton
    fun provideTmdbKeyProvider(remoteConfig: FirebaseRemoteConfig): FirebaseTmdbKeyProvider =
        FirebaseTmdbKeyProvider(remoteConfig)

    @Provides
    @Singleton
    fun provideTmdbKeyProviderInterface(impl: FirebaseTmdbKeyProvider): TmdbKeyProvider = impl
}
