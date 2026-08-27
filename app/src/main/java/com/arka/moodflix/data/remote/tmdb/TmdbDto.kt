package com.arka.moodflix.data.remote.tmdb

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TmdbSearchResponse(
    val results: List<TmdbMovieDto> = emptyList()
)

@Serializable
data class TmdbMovieDto(
    val id: Int,
    val title: String = "",
    @SerialName("release_date") val releaseDate: String? = null,
    val overview: String = "",
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("vote_average") val voteAverage: Float = 0f,
    @SerialName("vote_count") val voteCount: Int = 0,
    @SerialName("genre_ids") val genreIds: List<Int> = emptyList()
)

@Serializable
data class TmdbMovieDetailDto(
    val id: Int,
    val title: String = "",
    @SerialName("release_date") val releaseDate: String? = null,
    val overview: String = "",
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("vote_average") val voteAverage: Float = 0f,
    @SerialName("vote_count") val voteCount: Int = 0,
    val runtime: Int? = null,
    val genres: List<TmdbGenreDto> = emptyList(),
    val videos: TmdbVideosDto? = null,
    @SerialName("watch/providers") val watchProviders: TmdbWatchProvidersDto? = null
)

@Serializable
data class TmdbGenreDto(val id: Int, val name: String)

@Serializable
data class TmdbVideosDto(val results: List<TmdbVideoDto> = emptyList())

@Serializable
data class TmdbVideoDto(
    val key: String,
    val name: String = "",
    val site: String = "",
    val type: String = "",
    val official: Boolean = false
)

@Serializable
data class TmdbWatchProvidersDto(
    val results: Map<String, TmdbCountryProvidersDto> = emptyMap()
)

@Serializable
data class TmdbCountryProvidersDto(
    val link: String? = null,
    val flatrate: List<TmdbProviderDto> = emptyList(),
    val rent: List<TmdbProviderDto> = emptyList(),
    val buy: List<TmdbProviderDto> = emptyList()
)

@Serializable
data class TmdbProviderDto(
    @SerialName("provider_id") val providerId: Int,
    @SerialName("provider_name") val providerName: String,
    @SerialName("logo_path") val logoPath: String? = null
)

/** Response from GET /watch/providers/movie - the full catalog for a region. */
@Serializable
data class TmdbProviderListResponse(
    val results: List<TmdbProviderEntryDto> = emptyList()
)

@Serializable
data class TmdbProviderEntryDto(
    @SerialName("provider_id") val providerId: Int,
    @SerialName("provider_name") val providerName: String,
    @SerialName("logo_path") val logoPath: String? = null,
    // Lower number = more prominent in that region. Per-region, unlike the
    // deprecated global display_priority field.
    @SerialName("display_priorities") val displayPriorities: Map<String, Int> = emptyMap()
)