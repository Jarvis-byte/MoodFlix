package com.arka.moodflix.data.repository

import com.arka.moodflix.core.AppError
import com.arka.moodflix.core.AppResult
import com.arka.moodflix.data.local.UserPreferences
import com.arka.moodflix.data.remote.ai.AiRouter
import com.arka.moodflix.data.remote.ai.PromptBuilder
import com.arka.moodflix.data.remote.tmdb.TmdbApi
import com.arka.moodflix.data.remote.tmdb.toDomain
import com.arka.moodflix.domain.model.AiSuggestion
import com.arka.moodflix.domain.model.Genre
import com.arka.moodflix.domain.model.Movie
import com.arka.moodflix.domain.model.MoodQuery
import com.arka.moodflix.domain.model.OttProvider
import com.arka.moodflix.domain.repository.MovieRepository
import com.arka.moodflix.domain.repository.RecommendationState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MovieRepositoryImpl @Inject constructor(
    private val tmdb: TmdbApi,
    private val aiRouter: AiRouter,
    private val prefs: UserPreferences
) : MovieRepository {

    // Provider lists rarely change mid-session; avoids refetching every time
    // the discover screen is revisited.
    private val providerCache = ConcurrentHashMap<String, List<OttProvider>>()

    override fun recommend(query: MoodQuery): Flow<RecommendationState> = flow {
        emit(RecommendationState.AskingAi)

        val routed = when (val result = aiRouter.suggest(PromptBuilder.build(query))) {
            is AppResult.Success -> result.data
            is AppResult.Failure -> {
                // No provider worked (none connected, all out of quota, or all
                // offline). Rather than a dead end, fall back to a straight
                // TMDB discover call filtered by the same genre/rating/OTTs.
                val fallback = discoverFallback(query)
                if (fallback.isEmpty()) {
                    emit(RecommendationState.Failed(result.error))
                } else {
                    emit(RecommendationState.FallbackToTmdb(fallback, result.error))
                }
                return@flow
            }
        }

        val answeredBy = routed.answeredBy.displayName
        emit(RecommendationState.AiResponded(routed.suggestions.size, answeredBy))

        val country = prefs.watchCountry.first()
        val movies = enrich(routed.suggestions, country, query.minRating, query.selectedProviderIds)

        if (movies.isEmpty()) {
            // Either a fully hallucinated list, or every real match was
            // filtered out by the OTT selection. Either way, TMDB discover
            // (which respects the same OTT filter) is a better landing spot
            // than an empty screen.
            val fallback = discoverFallback(query)
            if (fallback.isEmpty()) {
                emit(RecommendationState.Failed(AppError.Unknown("Nothing matched on your selected platforms")))
            } else {
                emit(RecommendationState.FallbackToTmdb(fallback, AppError.ParseFailed()))
            }
        } else {
            emit(RecommendationState.Enriched(movies, answeredBy))
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Pure TMDB path: no mood reasoning, just "popular, well-rated films in
     * this genre, on these platforms." Keeps the app usable with zero AI keys
     * connected, or when every connected provider is down.
     */
    private suspend fun discoverFallback(query: MoodQuery): List<Movie> = coroutineScope {
        val country = prefs.watchCountry.first()
        val providerParam = query.selectedProviderIds.takeIf { it.isNotEmpty() }
            ?.joinToString("|") // pipe = OR: any one of the selected platforms

        val hits = runCatching {
            tmdb.discover(
                genreId = query.genre.takeIf { it != Genre.ANY }?.tmdbId?.toString(),
                minRating = query.minRating,
                sortBy = "vote_average.desc",
                withWatchProviders = providerParam,
                watchRegion = providerParam?.let { country }
            ).results
        }.getOrDefault(emptyList())

        hits.take(10)
            .map { hit ->
                async {
                    runCatching {
                        tmdb.getMovieDetail(hit.id).toDomain(
                            moodReason = "",
                            countryCode = country
                        )
                    }.getOrNull()
                }
            }
            .mapNotNull { it.await() }
            .distinctBy { it.tmdbId }
    }

    /**
     * Resolves every AI title against TMDB in parallel, then applies the
     * rating floor and the OTT filter. Titles that don't resolve, or don't
     * clear the bar, are dropped silently - that's the safety net for the
     * occasional hallucinated film, and the honest behaviour for "only show
     * me what's on my platforms."
     */
    private suspend fun enrich(
        suggestions: List<AiSuggestion>,
        country: String,
        minRating: Float,
        selectedProviderIds: List<Int>
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
            .filter { movie ->
                selectedProviderIds.isEmpty() ||
                        movie.watchProviders.any { it.providerId in selectedProviderIds }
            }
            .distinctBy { it.tmdbId }
    }

    override suspend fun getMovieDetail(tmdbId: Int): AppResult<Movie> = try {
        val country = prefs.watchCountry.first()
        AppResult.Success(tmdb.getMovieDetail(tmdbId).toDomain("", country))
    } catch (e: Exception) {
        AppResult.Failure(AppError.Unknown(e.message ?: "Could not load this film"))
    }

    /**
     * TMDB's own per-region display_priorities field, rather than a hardcoded
     * name list - so whatever a platform is currently branded as (JioHotstar
     * today, whatever it becomes tomorrow) shows up correctly ranked without
     * an app update.
     */
    override suspend fun getOttProviders(region: String): AppResult<List<OttProvider>> {
        providerCache[region]?.let { return AppResult.Success(it) }

        return try {
            val curated = tmdb.getWatchProviders(region).results
                .filter { it.displayPriorities.containsKey(region) }
                .sortedBy { it.displayPriorities.getValue(region) }
                .take(20)
                .map { it.toDomain() }

            providerCache[region] = curated
            AppResult.Success(curated)
        } catch (e: Exception) {
            AppResult.Failure(AppError.Unknown(e.message ?: "Could not load OTT platforms"))
        }
    }

    private companion object {
        // TMDB skews slightly lower than IMDb, so allow a little slack.
        const val RATING_TOLERANCE = 0.4f
        // Obscure films with few votes get a pass; the rating is not meaningful yet.
        const val LOW_VOTE_FLOOR = 200
    }
}