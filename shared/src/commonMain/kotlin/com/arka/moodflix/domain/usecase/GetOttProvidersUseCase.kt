package com.arka.moodflix.domain.usecase

import com.arka.moodflix.core.AppResult
import com.arka.moodflix.domain.model.OttProvider
import com.arka.moodflix.domain.repository.MovieRepository

class GetOttProvidersUseCase(
    private val repository: MovieRepository
) {
    suspend operator fun invoke(region: String): AppResult<List<OttProvider>> =
        repository.getOttProviders(region)
}