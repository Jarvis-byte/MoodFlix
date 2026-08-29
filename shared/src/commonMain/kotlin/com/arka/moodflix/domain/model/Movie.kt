package com.arka.moodflix.domain.model

/**
 * The merged model the UI actually renders - covers both films and TV/web
 * series under one shape. [moodReason] comes from the LLM. Everything else
 * comes from TMDB, because models hallucinate ratings and streaming
 * availability.
 *
 * [runtimeMinutes] means the film's runtime for a MOVIE, or the average
 * episode length for a SERIES - television doesn't have one single runtime.
 * [seasonCount]/[episodeCount] are only populated for SERIES.
 */
data class Movie(
    val tmdbId: Int,
    val mediaType: MediaType,
    val title: String,
    val year: String,
    val overview: String,
    val posterUrl: String?,
    val backdropUrl: String?,
    val rating: Float,
    val voteCount: Int,
    val runtimeMinutes: Int?,
    val seasonCount: Int? = null,
    val episodeCount: Int? = null,
    val genres: List<String>,
    val moodReason: String,
    val trailer: Trailer?,
    val watchProviders: List<WatchProvider>,
    val justWatchLink: String?
)

data class Trailer(
    val youtubeKey: String,
    val name: String
) {
    val thumbnailUrl: String get() = "https://img.youtube.com/vi/$youtubeKey/hqdefault.jpg"
    val watchUrl: String get() = "https://www.youtube.com/watch?v=$youtubeKey"
}

data class WatchProvider(
    val providerId: Int,
    val name: String,
    val logoUrl: String?,
    val type: ProviderType
)

enum class ProviderType { STREAM, RENT, BUY }

/** tmdbId alone isn't unique - movie and TV id spaces overlap. */
val Movie.watchlistId: String get() = "$tmdbId:${mediaType.name}"