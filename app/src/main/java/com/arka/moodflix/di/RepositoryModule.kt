package com.arka.moodflix.di

import com.arka.moodflix.data.local.UserPreferences
import com.arka.moodflix.data.remote.ai.AiProviderClient
import com.arka.moodflix.data.remote.ai.AiRouter
import com.arka.moodflix.data.remote.ai.AnthropicClient
import com.arka.moodflix.data.remote.ai.GeminiClient
import com.arka.moodflix.data.remote.ai.GroqClient
import com.arka.moodflix.data.remote.ai.OpenAiClient
import com.arka.moodflix.data.remote.tmdb.TmdbApi
import com.arka.moodflix.data.repository.MovieRepositoryImpl
import com.arka.moodflix.domain.repository.AiKeyRepository
import com.arka.moodflix.domain.repository.MovieRepository
import com.arka.moodflix.domain.repository.TmdbLanguageProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import javax.inject.Singleton

/**
 * MovieRepositoryImpl and the AI provider clients live in the shared KMP
 * module with plain constructors (no @Inject - javax.inject isn't available
 * outside the JVM/Android target), so they're wired up here with @Provides
 * instead of @Binds.
 */
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideMovieRepository(
        tmdb: TmdbApi,
        aiRouter: AiRouter,
        prefs: UserPreferences,
        languageProvider: TmdbLanguageProvider
    ): MovieRepository = MovieRepositoryImpl(tmdb, aiRouter, prefs, languageProvider)

    @Provides
    @Singleton
    fun provideAiRouter(
        clients: Set<@JvmSuppressWildcards AiProviderClient>,
        keyRepository: AiKeyRepository
    ): AiRouter = AiRouter(clients, keyRepository)

    // Multibinding: adding a fourth provider later means adding one @Provides
    // here and nothing else changes.
    @Provides
    @IntoSet
    fun provideGemini(client: HttpClient): AiProviderClient = GeminiClient(client)

    @Provides
    @IntoSet
    fun provideOpenAi(client: HttpClient): AiProviderClient = OpenAiClient(client)

    @Provides
    @IntoSet
    fun provideAnthropic(client: HttpClient): AiProviderClient = AnthropicClient(client)

    @Provides
    @IntoSet
    fun provideGroq(client: HttpClient): AiProviderClient = GroqClient(client)
}
