package com.arka.moodflix.di

import com.arka.moodflix.data.local.IosSecureKeyStore
import com.arka.moodflix.data.local.IosUserPreferences
import com.arka.moodflix.data.local.SecureKeyStore
import com.arka.moodflix.data.local.UserPreferences
import com.arka.moodflix.data.remote.ai.AiProviderClient
import com.arka.moodflix.data.remote.ai.AiRouter
import com.arka.moodflix.data.remote.ai.AnthropicClient
import com.arka.moodflix.data.remote.ai.GeminiClient
import com.arka.moodflix.data.remote.ai.OpenAiClient
import com.arka.moodflix.data.remote.tmdb.TmdbApi
import com.arka.moodflix.data.repository.AiKeyRepositoryImpl
import com.arka.moodflix.data.repository.MovieRepositoryImpl
import com.arka.moodflix.domain.repository.AiKeyRepository
import com.arka.moodflix.domain.repository.MovieRepository
import com.arka.moodflix.domain.usecase.GetOttProvidersUseCase
import com.arka.moodflix.domain.usecase.GetRecommendationsUseCase
import com.arka.moodflix.domain.usecase.ObserveConnectedProvidersUseCase
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * The iOS composition root - the counterpart to the Android app's Hilt
 * modules (app/di/NetworkModule.kt, RepositoryModule.kt, PlatformModule.kt,
 * UseCaseModule.kt). There's no Hilt/Koin equivalent wired up on this side,
 * so this one class builds the whole dependency graph by hand and exposes
 * only what the UI layer needs - same shape as the Android graph, just
 * assembled in code instead of by annotation processing.
 *
 * Construct exactly one instance - it owns the HttpClient and the Keychain-
 * backed key store, so it should live as long as the app (e.g. held by the
 * SwiftUI `App` struct, or behind a plain singleton).
 *
 * Usage from Swift:
 *
 *   let container = IosAppContainer(tmdbApiKey: "...")
 *
 *   // suspend funcs surface as async automatically
 *   let result = try await container.getOttProvidersUseCase.invoke(region: "US")
 *
 *   // Flow-returning use cases go through collectForSwift (see FlowBridge.kt)
 *   try await container.getRecommendationsUseCase.invoke(query: query)
 *       .collectForSwift { state in
 *           // update @Published state on the main actor
 *       }
 */
class IosAppContainer(tmdbApiKey: String) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        explicitNulls = false
    }

    // No engine specified - ktor-client-darwin is the only engine on this
    // source set's classpath, so it's picked automatically at compile time.
    private val httpClient = HttpClient {
        expectSuccess = false
        install(ContentNegotiation) { json(json) }
        install(HttpTimeout) {
            connectTimeoutMillis = 15_000
            requestTimeoutMillis = 60_000
        }
        install(Logging) { level = LogLevel.INFO }
    }

    private val secureKeyStore: SecureKeyStore = IosSecureKeyStore()
    private val userPreferences: UserPreferences = IosUserPreferences()

    private val tmdbApi = TmdbApi(httpClient, tmdbApiKey)

    private val aiProviderClients: Set<AiProviderClient> = setOf(
        GeminiClient(httpClient),
        OpenAiClient(httpClient),
        AnthropicClient(httpClient)
    )

    val userPrefs: UserPreferences get() = userPreferences

    val aiKeyRepository: AiKeyRepository = AiKeyRepositoryImpl(secureKeyStore, userPreferences)

    private val aiRouter = AiRouter(aiProviderClients, aiKeyRepository)

    val movieRepository: MovieRepository =
        MovieRepositoryImpl(tmdbApi, aiRouter, userPreferences)

    val getRecommendationsUseCase = GetRecommendationsUseCase(movieRepository)
    val getOttProvidersUseCase = GetOttProvidersUseCase(movieRepository)
    val observeConnectedProvidersUseCase = ObserveConnectedProvidersUseCase(aiKeyRepository)
}
