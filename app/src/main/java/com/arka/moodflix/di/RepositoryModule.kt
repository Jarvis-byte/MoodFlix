package com.arka.moodflix.di

import com.arka.moodflix.data.remote.ai.AiProviderClient
import com.arka.moodflix.data.remote.ai.AnthropicClient
import com.arka.moodflix.data.remote.ai.GeminiClient
import com.arka.moodflix.data.remote.ai.OpenAiClient
import com.arka.moodflix.data.repository.AiKeyRepositoryImpl
import com.arka.moodflix.data.repository.MovieRepositoryImpl
import com.arka.moodflix.domain.repository.AiKeyRepository
import com.arka.moodflix.domain.repository.MovieRepository
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoSet
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindMovieRepository(impl: MovieRepositoryImpl): MovieRepository

    @Binds
    @Singleton
    abstract fun bindAiKeyRepository(impl: AiKeyRepositoryImpl): AiKeyRepository

    // Multibinding: adding a fourth provider later means adding one @Binds here
    // and nothing else changes.
    @Binds
    @IntoSet
    abstract fun bindGemini(impl: GeminiClient): AiProviderClient

    @Binds
    @IntoSet
    abstract fun bindOpenAi(impl: OpenAiClient): AiProviderClient

    @Binds
    @IntoSet
    abstract fun bindAnthropic(impl: AnthropicClient): AiProviderClient
}
