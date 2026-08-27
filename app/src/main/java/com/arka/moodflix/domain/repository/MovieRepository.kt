package com.arka.moodflix.domain.repository

import com.arka.moodflix.core.AppResult
import com.arka.moodflix.domain.model.Movie
import com.arka.moodflix.domain.model.MoodQuery
import kotlinx.coroutines.flow.Flow

interface MovieRepository {

    /**
     * Emits progressively: the AI picks land first, then each movie is
     * enriched with TMDB data as it resolves. The UI can show cards early.
     */
    fun recommend(query: MoodQuery): Flow<RecommendationState>

    suspend fun getMovieDetail(tmdbId: Int): AppResult<Movie>
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
