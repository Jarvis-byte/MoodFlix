package com.arka.moodflix.data.remote.tmdb

import com.arka.moodflix.domain.repository.TmdbKeyProvider
import com.arka.moodflix.domain.repository.TmdbLanguageProvider
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.URLProtocol
import io.ktor.http.path

/**
 * [keyProvider] is asked for the key on every call rather than once at
 * construction time, since on Android it comes from Firebase Remote Config
 * and can change without an app restart (e.g. once it's fetched right after
 * login). Each request function pulls the key itself, because Ktor's
 * defaultRequest block isn't a suspend context.
 *
 * [languageProvider] drives TMDB's `language` param the same way, so a
 * language-toggle change (e.g. English to Hindi) picks up TMDB's own
 * translated titles/overviews on the next call, not just the app's own UI
 * chrome - defaultRequest's block runs fresh on every request, not once.
 */
class TmdbApi(
    engine: HttpClient,
    private val keyProvider: TmdbKeyProvider,
    private val languageProvider: TmdbLanguageProvider
) {
    private val client = engine.config {
        defaultRequest {
            url {
                protocol = URLProtocol.HTTPS
                host = "api.themoviedb.org"
                path("3/")
                parameters.append("language", languageProvider.current())
            }
        }
    }

    suspend fun searchMovie(
        query: String,
        year: String? = null,
        includeAdult: Boolean = false
    ): TmdbSearchResponse = client.get("search/movie") {
        parameter("api_key", keyProvider.getKey())
        parameter("query", query)
        parameter("year", year)
        parameter("include_adult", includeAdult)
    }.body()

    /**
     * append_to_response lets us pull videos + watch providers in a single
     * round trip instead of three. Worth it when we resolve 5-8 titles at once.
     */
    suspend fun getMovieDetail(
        id: Int,
        append: String = "videos,watch/providers"
    ): TmdbMovieDetailDto = client.get("movie/$id") {
        parameter("api_key", keyProvider.getKey())
        parameter("append_to_response", append)
    }.body()

    /** Used as a fallback when the AI call fails entirely. */
    suspend fun discover(
        genreId: String? = null,
        minRating: Float? = null,
        minVotes: Int = 300,
        sortBy: String = "popularity.desc",
        includeAdult: Boolean = false,
        // Pipe-separated = OR. Must be paired with watch_region or TMDB ignores it.
        withWatchProviders: String? = null,
        watchRegion: String? = null,
        page: Int = 1
    ): TmdbSearchResponse = client.get("discover/movie") {
        parameter("api_key", keyProvider.getKey())
        parameter("with_genres", genreId)
        parameter("vote_average.gte", minRating)
        parameter("vote_count.gte", minVotes)
        parameter("sort_by", sortBy)
        parameter("include_adult", includeAdult)
        parameter("with_watch_providers", withWatchProviders)
        parameter("watch_region", watchRegion)
        parameter("page", page)
    }.body()

    /**
     * "Top movies this month": popularity-sorted movies whose primary release
     * date falls within [releaseFrom, releaseFrom] (each "yyyy-MM-dd", caller
     * computes the calendar-month range since KMP commonMain has no date API
     * here). A low vote-count floor since a brand-new release hasn't had time
     * to rack up votes yet.
     */
    suspend fun discoverThisMonth(
        releaseFrom: String,
        releaseTo: String,
        genreId: String? = null,
        minRating: Float? = null,
        minVotes: Int = 20,
        page: Int = 1
    ): TmdbSearchResponse = client.get("discover/movie") {
        parameter("api_key", keyProvider.getKey())
        parameter("primary_release_date.gte", releaseFrom)
        parameter("primary_release_date.lte", releaseTo)
        parameter("with_genres", genreId)
        parameter("vote_average.gte", minRating)
        parameter("sort_by", "popularity.desc")
        parameter("vote_count.gte", minVotes)
        parameter("page", page)
    }.body()

    /** TV equivalent of [discoverThisMonth] - filters on first_air_date instead of primary_release_date. */
    suspend fun discoverTvThisMonth(
        releaseFrom: String,
        releaseTo: String,
        genreId: String? = null,
        minRating: Float? = null,
        minVotes: Int = 20,
        page: Int = 1
    ): TmdbTvSearchResponse = client.get("discover/tv") {
        parameter("api_key", keyProvider.getKey())
        parameter("first_air_date.gte", releaseFrom)
        parameter("first_air_date.lte", releaseTo)
        parameter("with_genres", genreId)
        parameter("vote_average.gte", minRating)
        parameter("sort_by", "popularity.desc")
        parameter("vote_count.gte", minVotes)
        parameter("page", page)
    }.body()

    /**
     * "More like this" on the detail screen. Recommendations (behavior-based:
     * "people who watched this also watched") reads better here than the
     * genre/keyword-based `similar` endpoint.
     */
    suspend fun getMovieRecommendations(id: Int, page: Int = 1): TmdbSearchResponse =
        client.get("movie/$id/recommendations") {
            parameter("api_key", keyProvider.getKey())
            parameter("page", page)
        }.body()

    /** Full provider catalog for a region, used to populate the OTT picker. */
    suspend fun getWatchProviders(watchRegion: String): TmdbProviderListResponse =
        client.get("watch/providers/movie") {
            parameter("api_key", keyProvider.getKey())
            parameter("watch_region", watchRegion)
        }.body()

    // ---- TV (series) equivalents ----

    suspend fun searchTv(
        query: String,
        year: String? = null,
        includeAdult: Boolean = false
    ): TmdbTvSearchResponse = client.get("search/tv") {
        parameter("api_key", keyProvider.getKey())
        parameter("query", query)
        parameter("first_air_date_year", year)
        parameter("include_adult", includeAdult)
    }.body()

    suspend fun getTvDetail(
        id: Int,
        append: String = "videos,watch/providers"
    ): TmdbTvDetailDto = client.get("tv/$id") {
        parameter("api_key", keyProvider.getKey())
        parameter("append_to_response", append)
    }.body()

    /** TV equivalent of [getMovieRecommendations]. */
    suspend fun getTvRecommendations(id: Int, page: Int = 1): TmdbTvSearchResponse =
        client.get("tv/$id/recommendations") {
            parameter("api_key", keyProvider.getKey())
            parameter("page", page)
        }.body()

    /** TV fallback path, mirroring discover/movie. */
    suspend fun discoverTv(
        genreId: String? = null,
        minRating: Float? = null,
        minVotes: Int = 300,
        sortBy: String = "popularity.desc",
        withWatchProviders: String? = null,
        watchRegion: String? = null,
        page: Int = 1
    ): TmdbTvSearchResponse = client.get("discover/tv") {
        parameter("api_key", keyProvider.getKey())
        parameter("with_genres", genreId)
        parameter("vote_average.gte", minRating)
        parameter("vote_count.gte", minVotes)
        parameter("sort_by", sortBy)
        parameter("with_watch_providers", withWatchProviders)
        parameter("watch_region", watchRegion)
        parameter("page", page)
    }.body()
}
