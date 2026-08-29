package com.arka.moodflix.di

import com.arka.moodflix.domain.repository.AiKeyRepository
import com.arka.moodflix.domain.repository.MovieRepository
import com.arka.moodflix.domain.usecase.GetOttProvidersUseCase
import com.arka.moodflix.domain.usecase.GetRecommendationsUseCase
import com.arka.moodflix.domain.usecase.GetTopMoviesThisMonthUseCase
import com.arka.moodflix.domain.usecase.ObserveConnectedProvidersUseCase
import com.arka.moodflix.domain.usecase.SearchMoviesUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Use cases live in the shared KMP module with plain constructors (no
 * @Inject - javax.inject isn't available outside the JVM/Android target), so
 * they're wired up here instead of via @Inject constructor.
 */
@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {

    @Provides
    fun provideGetRecommendationsUseCase(repository: MovieRepository): GetRecommendationsUseCase =
        GetRecommendationsUseCase(repository)

    @Provides
    fun provideGetOttProvidersUseCase(repository: MovieRepository): GetOttProvidersUseCase =
        GetOttProvidersUseCase(repository)

    @Provides
    fun provideObserveConnectedProvidersUseCase(repository: AiKeyRepository): ObserveConnectedProvidersUseCase =
        ObserveConnectedProvidersUseCase(repository)

    @Provides
    fun provideGetTopMoviesThisMonthUseCase(repository: MovieRepository): GetTopMoviesThisMonthUseCase =
        GetTopMoviesThisMonthUseCase(repository)

    @Provides
    fun provideSearchMoviesUseCase(repository: MovieRepository): SearchMoviesUseCase =
        SearchMoviesUseCase(repository)
}
