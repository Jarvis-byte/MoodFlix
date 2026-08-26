package com.arka.moodflix.data.repository

import com.arka.moodflix.core.AppError
import com.arka.moodflix.core.AppResult
import com.arka.moodflix.data.local.UserPreferences
import com.arka.moodflix.data.remote.ai.AiRouter
import com.arka.moodflix.data.remote.ai.PromptBuilder
import com.arka.moodflix.data.remote.tmdb.TmdbApi
import com.arka.moodflix.data.remote.tmdb.toDomain
import com.arka.moodflix.domain.model.AiSuggestion
import com.arka.moodflix.domain.model.Movie
import com.arka.moodflix.domain.model.MoodQuery
import com.arka.moodflix.domain.repository.MovieRepository
import com.arka.moodflix.domain.repository.RecommendationState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MovieRepositoryImpl @Inject constructor(
    private val tmdb: TmdbApi,
    private val aiRouter: AiRouter,
    private val prefs: UserPreferences
) : MovieRepository {

    override fun recommend(query: MoodQuery): Flow<RecommendationState> = flow {
        emit(RecommendationState.AskingAi)

        val routed = when (val result = aiRouter.suggest(PromptBuilder.build(query))) {
            is AppResult.Success -> result.data
            is AppResult.Failure -> {
                emit(RecommendationState.Failed(result.error))
                return@flow
            }
        }

        val answeredBy = routed.answeredBy.displayName
        emit(RecommendationState.AiResponded(routed.suggestions.size, answeredBy))

        val country = prefs.watchCountry.first()
        val movies = enrich(routed.suggestions, country, query.minRating)

        if (movies.isEmpty()) {
            emit(RecommendationState.Failed(AppError.Unknown("Could not find these titles on TMDB")))
        } else {
            emit(RecommendationState.Enriched(movies, answeredBy))
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Resolves every AI title against TMDB in parallel. Titles that don't
     * resolve are dropped silently rather than shown as broken cards - that is
     * the safety net for the occasional hallucinated film.
     */
    private suspend fun enrich(
        suggestions: List<AiSuggestion>,
        country: String,
        minRating: Float
    ): List<Movie> = coroutineScope {
        suggestions
            .map { suggestion ->
                async {
                    runCatching {
                        val hit = tmdb.searchMovie(
                            query = suggestion.title,
                            year = suggestion.year.takeIf { it.length == 4 }
                        ).results.firstOrNull() ?: return@runCatching null

                        tmdb.getMovieDetail(hit.id).toDomain(
                            moodReason = suggestion.reason,
                            countryCode = country
                        )
                    }.getOrNull()
                }
            }
            .mapNotNull { it.await() }
            // The model is asked for a quality bar but cannot enforce it, so we do.
            .filter { it.rating >= minRating - RATING_TOLERANCE || it.voteCount < LOW_VOTE_FLOOR }
            .distinctBy { it.tmdbId }
    }

    override suspend fun getMovieDetail(tmdbId: Int): AppResult<Movie> = try {
        val country = prefs.watchCountry.first()
        AppResult.Success(tmdb.getMovieDetail(tmdbId).toDomain("", country))
    } catch (e: Exception) {
        AppResult.Failure(AppError.Unknown(e.message ?: "Could not load this film"))
    }

    private companion object {
        // TMDB skews slightly lower than IMDb, so allow a little slack.
        const val RATING_TOLERANCE = 0.4f
        // Obscure films with few votes get a pass; the rating is not meaningful yet.
        const val LOW_VOTE_FLOOR = 200
    }
}
