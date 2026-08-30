package com.arka.moodflix.data.repository

import com.arka.moodflix.core.AppError
import com.arka.moodflix.core.AppResult
import com.arka.moodflix.core.Logger
import com.arka.moodflix.data.local.UserPreferences
import com.arka.moodflix.data.remote.ai.AiRouter
import com.arka.moodflix.data.remote.ai.PromptBuilder
import com.arka.moodflix.data.remote.tmdb.TmdbApi
import com.arka.moodflix.data.remote.tmdb.toDomain
import com.arka.moodflix.data.remote.tmdb.toDomainLight
import com.arka.moodflix.domain.model.AiSuggestion
import com.arka.moodflix.domain.model.Genre
import com.arka.moodflix.domain.model.MediaType
import com.arka.moodflix.domain.model.MediaTypeFilter
import com.arka.moodflix.domain.model.Movie
import com.arka.moodflix.domain.model.MoodQuery
import com.arka.moodflix.domain.model.OttProvider
import com.arka.moodflix.domain.repository.MovieRepository
import com.arka.moodflix.domain.repository.RecommendationState
import com.arka.moodflix.domain.repository.TmdbLanguageProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class MovieRepositoryImpl(
    private val tmdb: TmdbApi,
    private val aiRouter: AiRouter,
    private val prefs: UserPreferences,
    private val languageProvider: TmdbLanguageProvider
) : MovieRepository {

    // Provider lists rarely change mid-session; avoids refetching every time
    // the discover screen is revisited.
    private val providerCacheMutex = Mutex()
    private val providerCache = mutableMapOf<String, List<OttProvider>>()

    // Search-tab caches: browsing back to the tab or re-typing an already
    // searched query shouldn't re-hit the network - only pull-to-refresh should.
    private val topMoviesCacheMutex = Mutex()
    private var topMoviesCacheKey: String? = null
    private var topMoviesCache: List<Movie> = emptyList()

    private val searchCacheMutex = Mutex()
    private val searchCache = mutableMapOf<String, List<Movie>>()

    // "More like this" per title - opening the same detail again this
    // session (e.g. via the back-navigation carousel) shouldn't re-fetch.
    private val similarCacheMutex = Mutex()
    private val similarCache = mutableMapOf<String, List<Movie>>()

    override fun recommend(query: MoodQuery): Flow<RecommendationState> = flow {
        emit(RecommendationState.AskingAi)

        val routed = when (val result = aiRouter.suggest(PromptBuilder.build(query))) {
            is AppResult.Success -> result.data
            is AppResult.Failure -> {
                // No provider worked (none connected, all out of quota, or all
                // offline). Rather than a dead end, fall back to a straight
                // TMDB discover call filtered by the same genre/rating/OTTs/media type.
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
            // (which respects the same filters) is a better landing spot
            // than an empty screen.
            val fallback = discoverFallback(query)
            if (fallback.isEmpty()) {
                emit(RecommendationState.Failed(AppError.NoMatches))
            } else {
                emit(RecommendationState.FallbackToTmdb(fallback, AppError.ParseFailed))
            }
        } else {
            emit(RecommendationState.Enriched(movies, answeredBy))
        }
    }.flowOn(Dispatchers.Default)

    /**
     * Pure TMDB path: no mood reasoning, just "popular, well-rated titles in
     * this genre, on these platforms, of this media type." Keeps the app
     * usable with zero AI keys connected, or when every connected provider is
     * down. When mediaFilter is BOTH, movie and TV results are fetched in
     * parallel and merged, sorted by rating.
     */
    private suspend fun discoverFallback(query: MoodQuery): List<Movie> = coroutineScope {
        val country = prefs.watchCountry.first()
        val providerParam = query.selectedProviderIds.takeIf { it.isNotEmpty() }
            ?.joinToString("|") // pipe = OR: any one of the selected platforms

        val wantMovies = query.mediaFilter != MediaTypeFilter.SERIES
        val wantSeries = query.mediaFilter != MediaTypeFilter.MOVIES

        // Both branches always launch, but return an empty list immediately
        // when that media type wasn't requested - this sidesteps a Kotlin
        // inference issue where `if (cond) async {...} else null` fails to
        // resolve Deferred<T> against a null branch on newer compilers, and
        // it's simpler than juggling nullable Deferreds besides.
        val movieHitsDeferred = async {
            if (!wantMovies) return@async emptyList()
            runCatching {
                tmdb.discover(
                    genreId = query.genre.takeIf { it != Genre.ANY }?.tmdbId?.toString(),
                    minRating = query.minRating,
                    sortBy = "vote_average.desc",
                    withWatchProviders = providerParam,
                    watchRegion = providerParam?.let { country },
                    page = query.page
                ).results.map { it.id }
            }.getOrDefault(emptyList())
        }

        val seriesHitsDeferred = async {
            if (!wantSeries) return@async emptyList()
            runCatching {
                tmdb.discoverTv(
                    genreId = query.genre.takeIf { it != Genre.ANY }?.tvGenreId?.toString(),
                    minRating = query.minRating,
                    sortBy = "vote_average.desc",
                    withWatchProviders = providerParam,
                    watchRegion = providerParam?.let { country },
                    page = query.page
                ).results.map { it.id }
            }.getOrDefault(emptyList())
        }

        // Split the take(10) budget between the two media types when both are
        // wanted, so one type doesn't crowd out the other.
        val perTypeLimit = if (wantMovies && wantSeries) 5 else 10

        val movieDetails = movieHitsDeferred.await().take(perTypeLimit).map { id ->
            async {
                runCatching {
                    tmdb.getMovieDetail(id).toDomain(moodReason = "", countryCode = country)
                }.getOrNull()
            }
        }

        val seriesDetails = seriesHitsDeferred.await().take(perTypeLimit).map { id ->
            async {
                runCatching {
                    tmdb.getTvDetail(id).toDomain(moodReason = "", countryCode = country)
                }.getOrNull()
            }
        }

        (movieDetails + seriesDetails)
            .mapNotNull { it.await() }
            .sortedByDescending { it.rating }
            .distinctBy { it.tmdbId to it.mediaType }
    }

    /**
     * Resolves every AI title against TMDB in parallel, then applies the
     * rating floor and the OTT filter. Titles that don't resolve, or don't
     * clear the bar, are dropped silently - that's the safety net for the
     * occasional hallucinated title, and the honest behaviour for "only show
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
                async { resolveSuggestion(suggestion, country) }
            }
            .mapNotNull { it.await() }
            // The model is asked for a quality bar but cannot enforce it, so we do.
            .filter { it.rating >= minRating - RATING_TOLERANCE || it.voteCount < LOW_VOTE_FLOOR }
            .filter { movie ->
                selectedProviderIds.isEmpty() ||
                        movie.watchProviders.any { it.providerId in selectedProviderIds }
            }
            .distinctBy { it.tmdbId to it.mediaType }
    }

    /**
     * Tries the AI's declared type first (movie or series), then falls back
     * to the other TMDB endpoint if that search comes up empty - the AI's
     * classification is usually right but not guaranteed, and this costs one
     * extra call only on the rare miss.
     */
    private suspend fun resolveSuggestion(suggestion: AiSuggestion, country: String): Movie? {
        val primary = if (suggestion.mediaType == MediaType.SERIES) {
            resolveAsSeries(suggestion, country)
        } else {
            resolveAsMovie(suggestion, country)
        }
        if (primary != null) return primary

        return if (suggestion.mediaType == MediaType.SERIES) {
            resolveAsMovie(suggestion, country)
        } else {
            resolveAsSeries(suggestion, country)
        }
    }

    private suspend fun resolveAsMovie(suggestion: AiSuggestion, country: String): Movie? =
        runCatching {
            val hit = tmdb.searchMovie(
                query = suggestion.title,
                year = suggestion.year.takeIf { it.length == 4 }
            ).results.firstOrNull() ?: return null

            tmdb.getMovieDetail(hit.id).toDomain(
                moodReason = suggestion.reason,
                countryCode = country
            )
        }.getOrNull()

    private suspend fun resolveAsSeries(suggestion: AiSuggestion, country: String): Movie? =
        runCatching {
            val hit = tmdb.searchTv(
                query = suggestion.title,
                year = suggestion.year.takeIf { it.length == 4 }
            ).results.firstOrNull() ?: return null

            tmdb.getTvDetail(hit.id).toDomain(
                moodReason = suggestion.reason,
                countryCode = country
            )
        }.getOrNull()

    override suspend fun getMovieDetail(tmdbId: Int, mediaType: MediaType): AppResult<Movie> = try {
        val country = prefs.watchCountry.first()
        val movie = if (mediaType == MediaType.SERIES) {
            tmdb.getTvDetail(tmdbId).toDomain("", country)
        } else {
            tmdb.getMovieDetail(tmdbId).toDomain("", country)
        }
        AppResult.Success(movie)
    } catch (e: Exception) {
        // e.message can embed the request URL (TMDB key is a query param) - never surface it to the UI.
        Logger.w(TAG, "getMovieDetail failed: ${e.message}")
        AppResult.Failure(AppError.TitleLoadFailed)
    }

    /**
     * TMDB's own per-region display_priorities field, rather than a hardcoded
     * name list - so whatever a platform is currently branded as (JioHotstar
     * today, whatever it becomes tomorrow) shows up correctly ranked without
     * an app update.
     */
    override suspend fun getOttProviders(region: String): AppResult<List<OttProvider>> {
        val cacheKey = "$region:${languageProvider.current()}"
        providerCacheMutex.withLock { providerCache[cacheKey] }?.let { return AppResult.Success(it) }

        return try {
            val curated = tmdb.getWatchProviders(region).results
                .filter { it.displayPriorities.containsKey(region) }
                .sortedBy { it.displayPriorities.getValue(region) }
                .take(20)
                .map { it.toDomain() }

            providerCacheMutex.withLock { providerCache[cacheKey] = curated }
            AppResult.Success(curated)
        } catch (e: Exception) {
            Logger.w(TAG, "getOttProviders failed: ${e.message}")
            AppResult.Failure(AppError.ProvidersLoadFailed)
        }
    }

    override suspend fun getTopMoviesThisMonth(
        releaseFrom: String,
        releaseTo: String,
        limit: Int,
        genre: Genre,
        minRating: Float,
        forceRefresh: Boolean
    ): AppResult<List<Movie>> {
        val cacheKey = "$releaseFrom|$releaseTo|$limit|${genre.name}|$minRating|${languageProvider.current()}"
        if (!forceRefresh) {
            topMoviesCacheMutex.withLock {
                topMoviesCache.takeIf { topMoviesCacheKey == cacheKey }
            }?.let { return AppResult.Success(it) }
        }

        val ratingParam = minRating.takeIf { it > 0f }

        return try {
            val movies = coroutineScope {
                val moviesDeferred = async {
                    runCatching {
                        tmdb.discoverThisMonth(
                            releaseFrom = releaseFrom,
                            releaseTo = releaseTo,
                            genreId = genre.takeIf { it != Genre.ANY }?.tmdbId?.toString(),
                            minRating = ratingParam
                        ).results.map { it.toDomainLight() }
                    }.getOrDefault(emptyList())
                }
                val seriesDeferred = async {
                    // No honest TV equivalent for this genre (e.g. Horror/Romance/Thriller) -
                    // skip series rather than silently mapping to something close-but-wrong,
                    // same call DiscoverScreen already makes for its own genre filter.
                    if (genre != Genre.ANY && genre.tvGenreId == null) {
                        emptyList()
                    } else {
                        runCatching {
                            tmdb.discoverTvThisMonth(
                                releaseFrom = releaseFrom,
                                releaseTo = releaseTo,
                                genreId = genre.takeIf { it != Genre.ANY }?.tvGenreId?.toString(),
                                minRating = ratingParam
                            ).results.map { it.toDomainLight() }
                        }.getOrDefault(emptyList())
                    }
                }
                (moviesDeferred.await() + seriesDeferred.await())
                    .sortedByDescending { it.rating }
                    .take(limit)
            }
            topMoviesCacheMutex.withLock {
                topMoviesCacheKey = cacheKey
                topMoviesCache = movies
            }
            AppResult.Success(movies)
        } catch (e: Exception) {
            Logger.w(TAG, "getTopMoviesThisMonth failed: ${e.message}")
            AppResult.Failure(AppError.MonthlyTitlesLoadFailed)
        }
    }

    /** Movies and series merged, rating-sorted - the Search tab covers both media types. */
    override suspend fun searchMovies(query: String, forceRefresh: Boolean): AppResult<List<Movie>> {
        val cacheKey = "${query.trim().lowercase()}:${languageProvider.current()}"
        if (!forceRefresh) {
            searchCacheMutex.withLock { searchCache[cacheKey] }?.let { return AppResult.Success(it) }
        }

        return try {
            val results = coroutineScope {
                val moviesDeferred = async {
                    runCatching { tmdb.searchMovie(query).results.map { it.toDomainLight() } }
                        .getOrDefault(emptyList())
                }
                val seriesDeferred = async {
                    runCatching { tmdb.searchTv(query).results.map { it.toDomainLight() } }
                        .getOrDefault(emptyList())
                }
                (moviesDeferred.await() + seriesDeferred.await())
                    .sortedByDescending { it.rating }
            }
            searchCacheMutex.withLock { searchCache[cacheKey] = results }
            AppResult.Success(results)
        } catch (e: Exception) {
            Logger.w(TAG, "searchMovies failed: ${e.message}")
            AppResult.Failure(AppError.SearchFailed)
        }
    }

    override suspend fun getSimilar(
        tmdbId: Int,
        mediaType: MediaType,
        limit: Int
    ): AppResult<List<Movie>> {
        val cacheKey = "$tmdbId:${mediaType.name}:${languageProvider.current()}"
        similarCacheMutex.withLock { similarCache[cacheKey] }?.let { return AppResult.Success(it) }

        return try {
            val movies = if (mediaType == MediaType.SERIES) {
                tmdb.getTvRecommendations(tmdbId).results.map { it.toDomainLight() }
            } else {
                tmdb.getMovieRecommendations(tmdbId).results.map { it.toDomainLight() }
            }.take(limit)

            similarCacheMutex.withLock { similarCache[cacheKey] = movies }
            AppResult.Success(movies)
        } catch (e: Exception) {
            Logger.w(TAG, "getSimilar failed: ${e.message}")
            AppResult.Failure(AppError.SimilarLoadFailed)
        }
    }

    private companion object {
        const val TAG = "MovieRepository"
        // TMDB skews slightly lower than IMDb, so allow a little slack.
        const val RATING_TOLERANCE = 0.4f
        // Obscure films with few votes get a pass; the rating is not meaningful yet.
        const val LOW_VOTE_FLOOR = 200
    }
}