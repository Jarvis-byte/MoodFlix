package com.arka.moodflix.domain.repository

import com.arka.moodflix.core.AppResult
import com.arka.moodflix.domain.model.Genre
import com.arka.moodflix.domain.model.MediaType
import com.arka.moodflix.domain.model.Movie
import com.arka.moodflix.domain.model.MoodQuery
import com.arka.moodflix.domain.model.OttProvider
import kotlinx.coroutines.flow.Flow

interface MovieRepository {

    /**
     * Emits progressively: the AI picks land first, then each movie is
     * enriched with TMDB data as it resolves. The UI can show cards early.
     */
    fun recommend(query: MoodQuery): Flow<RecommendationState>

    /**
     * [mediaType] is required because TMDB movie ids and TV ids are separate
     * spaces - the same numeric id can point to two unrelated titles.
     */
    suspend fun getMovieDetail(tmdbId: Int, mediaType: MediaType): AppResult<Movie>

    /** Live provider catalog for a region, sorted by local popularity. */
    suspend fun getOttProviders(region: String): AppResult<List<OttProvider>>

    /**
     * Popularity-sorted movies whose primary release date falls in
     * [releaseFrom, releaseTo] ("yyyy-MM-dd"), capped at [limit], optionally
     * narrowed by [genre] and [minRating] (applied server-side, since TMDB's
     * discover endpoint supports both). Cached per parameter combination for
     * the session - pass [forceRefresh] (pull-to-refresh) to bypass it.
     */
    suspend fun getTopMoviesThisMonth(
        releaseFrom: String,
        releaseTo: String,
        limit: Int = 20,
        genre: Genre = Genre.ANY,
        minRating: Float = 0f,
        forceRefresh: Boolean = false
    ): AppResult<List<Movie>>

    /**
     * Free-text movie title search, for the Search tab. Cached per query for
     * the session - pass [forceRefresh] (pull-to-refresh) to bypass it.
     */
    suspend fun searchMovies(query: String, forceRefresh: Boolean = false): AppResult<List<Movie>>

    /** "More like this" on the detail screen. Cached per title for the session. */
    suspend fun getSimilar(tmdbId: Int, mediaType: MediaType, limit: Int = 15): AppResult<List<Movie>>
}

sealed interface RecommendationState {
    data object AskingAi : RecommendationState
    data class AiResponded(val titleCount: Int, val answeredBy: String) : RecommendationState
    data class Enriched(val movies: List<Movie>, val answeredBy: String) : RecommendationState

    /**
     * Every connected AI provider failed (or none are connected), so results
     * came straight from TMDB's popularity/rating filters instead. There's no
     * per-movie mood reasoning in this mode - that part genuinely needs the AI.
     */
    data class FallbackToTmdb(val movies: List<Movie>, val reason: com.arka.moodflix.core.AppError) :
        RecommendationState

    data class Failed(val error: com.arka.moodflix.core.AppError) : RecommendationState
}