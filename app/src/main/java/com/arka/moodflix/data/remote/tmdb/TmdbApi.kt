package com.arka.moodflix.data.remote.tmdb

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TmdbApi {

    @GET("search/movie")
    suspend fun searchMovie(
        @Query("query") query: String,
        @Query("year") year: String? = null,
        @Query("include_adult") includeAdult: Boolean = false
    ): TmdbSearchResponse

    /**
     * append_to_response lets us pull videos + watch providers in a single
     * round trip instead of three. Worth it when we resolve 5-8 titles at once.
     */
    @GET("movie/{id}")
    suspend fun getMovieDetail(
        @Path("id") id: Int,
        @Query("append_to_response") append: String = "videos,watch/providers"
    ): TmdbMovieDetailDto

    /** Used as a fallback when the AI call fails entirely. */
    @GET("discover/movie")
    suspend fun discover(
        @Query("with_genres") genreId: String? = null,
        @Query("vote_average.gte") minRating: Float? = null,
        @Query("vote_count.gte") minVotes: Int = 300,
        @Query("sort_by") sortBy: String = "popularity.desc",
        @Query("include_adult") includeAdult: Boolean = false
    ): TmdbSearchResponse
}
