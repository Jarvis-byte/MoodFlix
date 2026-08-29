package com.arka.moodflix.domain.usecase

import com.arka.moodflix.core.AppResult
import com.arka.moodflix.domain.model.Genre
import com.arka.moodflix.domain.model.Movie
import com.arka.moodflix.domain.repository.MovieRepository

class GetTopMoviesThisMonthUseCase(
    private val repository: MovieRepository
) {
    suspend operator fun invoke(
        releaseFrom: String,
        releaseTo: String,
        limit: Int = 20,
        genre: Genre = Genre.ANY,
        minRating: Float = 0f,
        forceRefresh: Boolean = false
    ): AppResult<List<Movie>> =
        repository.getTopMoviesThisMonth(releaseFrom, releaseTo, limit, genre, minRating, forceRefresh)
}
