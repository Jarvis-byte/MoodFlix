package com.arka.moodflix.domain.model

/**
 * The merged model the UI actually renders.
 * [moodReason] comes from the LLM. Everything else comes from TMDB, because
 * models hallucinate ratings and streaming availability.
 */
data class Movie(
    val tmdbId: Int,
    val title: String,
    val year: String,
    val overview: String,
    val posterUrl: String?,
    val backdropUrl: String?,
    val rating: Float,
    val voteCount: Int,
    val runtimeMinutes: Int?,
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