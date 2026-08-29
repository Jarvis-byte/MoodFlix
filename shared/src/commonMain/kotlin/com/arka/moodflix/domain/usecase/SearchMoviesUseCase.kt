package com.arka.moodflix.domain.usecase

import com.arka.moodflix.core.AppResult
import com.arka.moodflix.domain.model.Movie
import com.arka.moodflix.domain.repository.MovieRepository

class SearchMoviesUseCase(
    private val repository: MovieRepository
) {
    suspend operator fun invoke(query: String, forceRefresh: Boolean = false): AppResult<List<Movie>> =
        repository.searchMovies(query, forceRefresh)
}
