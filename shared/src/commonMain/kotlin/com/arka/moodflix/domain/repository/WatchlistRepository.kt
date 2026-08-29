package com.arka.moodflix.domain.repository

import com.arka.moodflix.domain.model.MediaType
import com.arka.moodflix.domain.model.Movie
import kotlinx.coroutines.flow.Flow

/**
 * Local, on-device watchlist - no account/sync involved, unlike
 * [com.arka.moodflix.domain.repository.AuthRepository]-gated data. Movies
 * are stored with just enough fields to render a card; opening a saved
 * title still goes through [MovieRepository.getMovieDetail] for the full
 * picture, same as every other list in the app.
 */
interface WatchlistRepository {
    fun observeWatchlist(): Flow<List<Movie>>
    fun observeIsSaved(tmdbId: Int, mediaType: MediaType): Flow<Boolean>
    suspend fun toggle(movie: Movie)
}
