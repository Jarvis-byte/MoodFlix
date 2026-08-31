package com.arka.moodflix.domain.repository

import com.arka.moodflix.domain.model.MediaType
import com.arka.moodflix.domain.model.Movie
import kotlinx.coroutines.flow.Flow

/**
 * Per-user watchlist, tied to the signed-in [com.arka.moodflix.domain.repository.AuthRepository]
 * account rather than living only on this device - it follows the user
 * across devices. Movies are stored with just enough fields to render a
 * card; opening a saved title still goes through [MovieRepository.getMovieDetail]
 * for the full picture, same as every other list in the app.
 *
 * [observeWatchlist] and [observeIsSaved] emit an empty/false result while
 * signed out and switch to the newly signed-in account's data automatically
 * once auth state changes - callers don't need to re-subscribe on login.
 */
interface WatchlistRepository {
    fun observeWatchlist(): Flow<List<Movie>>
    fun observeIsSaved(tmdbId: Int, mediaType: MediaType): Flow<Boolean>

    /** @return true if [movie] was added, false if it was already saved and got removed. */
    suspend fun toggle(movie: Movie): Boolean
}
