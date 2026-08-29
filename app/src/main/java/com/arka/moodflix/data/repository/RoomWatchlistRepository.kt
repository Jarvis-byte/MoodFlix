package com.arka.moodflix.data.repository

import com.arka.moodflix.data.local.watchlist.WatchlistDao
import com.arka.moodflix.data.local.watchlist.WatchlistEntity
import com.arka.moodflix.domain.model.MediaType
import com.arka.moodflix.domain.model.Movie
import com.arka.moodflix.domain.model.watchlistId
import com.arka.moodflix.domain.repository.WatchlistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomWatchlistRepository(
    private val dao: WatchlistDao
) : WatchlistRepository {

    override fun observeWatchlist(): Flow<List<Movie>> =
        dao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override fun observeIsSaved(tmdbId: Int, mediaType: MediaType): Flow<Boolean> =
        dao.observeIsSaved("$tmdbId:${mediaType.name}")

    override suspend fun toggle(movie: Movie) {
        val id = movie.watchlistId
        if (dao.exists(id)) {
            dao.delete(id)
        } else {
            dao.insert(movie.toEntity(id))
        }
    }

    private fun WatchlistEntity.toDomain(): Movie = Movie(
        tmdbId = tmdbId,
        mediaType = MediaType.valueOf(mediaType),
        title = title,
        year = year,
        overview = "",
        posterUrl = posterUrl,
        backdropUrl = null,
        rating = rating,
        voteCount = 0,
        runtimeMinutes = null,
        seasonCount = null,
        episodeCount = null,
        genres = emptyList(),
        moodReason = "",
        trailer = null,
        watchProviders = emptyList(),
        justWatchLink = null
    )

    private fun Movie.toEntity(id: String): WatchlistEntity = WatchlistEntity(
        id = id,
        tmdbId = tmdbId,
        mediaType = mediaType.name,
        title = title,
        year = year,
        posterUrl = posterUrl,
        rating = rating,
        addedAtEpochMillis = System.currentTimeMillis()
    )
}
