package com.arka.moodflix.data.remote.tmdb

import com.arka.moodflix.domain.model.Genre
import com.arka.moodflix.domain.model.MediaType
import com.arka.moodflix.domain.model.Movie
import com.arka.moodflix.domain.model.ProviderType
import com.arka.moodflix.domain.model.Trailer
import com.arka.moodflix.domain.model.WatchProvider

private const val IMAGE_BASE = "https://image.tmdb.org/t/p/"

fun posterUrl(path: String?): String? = path?.let { "${IMAGE_BASE}w500$it" }
fun backdropUrl(path: String?): String? = path?.let { "${IMAGE_BASE}w780$it" }
fun logoUrl(path: String?): String? = path?.let { "${IMAGE_BASE}w92$it" }

private fun buildProviders(
    watchProviders: TmdbWatchProvidersDto?,
    countryCode: String
): Pair<List<WatchProvider>, String?> {
    val country = watchProviders?.results?.get(countryCode)

    val providers = buildList {
        country?.flatrate?.forEach {
            add(WatchProvider(it.providerId, it.providerName, logoUrl(it.logoPath), ProviderType.STREAM))
        }
        country?.rent?.forEach {
            add(WatchProvider(it.providerId, it.providerName, logoUrl(it.logoPath), ProviderType.RENT))
        }
        country?.buy?.forEach {
            add(WatchProvider(it.providerId, it.providerName, logoUrl(it.logoPath), ProviderType.BUY))
        }
    }.distinctBy { it.providerId to it.type }

    return providers to country?.link
}

private fun pickTrailer(videos: TmdbVideosDto?): Trailer? =
    videos?.results
        ?.filter { it.site.equals("YouTube", true) }
        ?.sortedWith(
            compareByDescending<TmdbVideoDto> { it.type.equals("Trailer", true) }
                .thenByDescending { it.official }
        )
        ?.firstOrNull()
        ?.let { Trailer(it.key, it.name.ifBlank { "Trailer" }) }

fun TmdbMovieDetailDto.toDomain(
    moodReason: String,
    countryCode: String
): Movie {
    val (providers, link) = buildProviders(watchProviders, countryCode)

    return Movie(
        tmdbId = id,
        mediaType = MediaType.MOVIE,
        title = title,
        year = releaseDate?.take(4).orEmpty(),
        overview = overview,
        posterUrl = posterUrl(posterPath),
        backdropUrl = backdropUrl(backdropPath),
        rating = voteAverage,
        voteCount = voteCount,
        runtimeMinutes = runtime?.takeIf { it > 0 },
        seasonCount = null,
        episodeCount = null,
        genres = genres.map { it.name },
        moodReason = moodReason,
        trailer = pickTrailer(videos),
        watchProviders = providers,
        justWatchLink = link
    )
}

/**
 * Series have no single runtime - episode_run_time is a list because length
 * can vary by season, so we take the first entry as a representative average.
 */
fun TmdbTvDetailDto.toDomain(
    moodReason: String,
    countryCode: String
): Movie {
    val (providers, link) = buildProviders(watchProviders, countryCode)

    return Movie(
        tmdbId = id,
        mediaType = MediaType.SERIES,
        title = name,
        year = firstAirDate?.take(4).orEmpty(),
        overview = overview,
        posterUrl = posterUrl(posterPath),
        backdropUrl = backdropUrl(backdropPath),
        rating = voteAverage,
        voteCount = voteCount,
        runtimeMinutes = episodeRunTime.firstOrNull()?.takeIf { it > 0 },
        seasonCount = numberOfSeasons,
        episodeCount = numberOfEpisodes,
        genres = genres.map { it.name },
        moodReason = moodReason,
        trailer = pickTrailer(videos),
        watchProviders = providers,
        justWatchLink = link
    )
}

fun TmdbMovieDto.genreLabels(): List<String> =
    genreIds.mapNotNull { Genre.fromTmdbId(it)?.label }

/**
 * Cheap mapping straight from a search/discover list item - no per-title
 * detail call, so no trailer/runtime/watch-providers. Used for browse grids
 * (search results, "top this month") where fetching detail for 20+ items
 * up front would be slow; [MovieRepository.getMovieDetail] fills those in
 * once the user actually opens a title.
 */
fun TmdbMovieDto.toDomainLight(): Movie = Movie(
    tmdbId = id,
    mediaType = MediaType.MOVIE,
    title = title,
    year = releaseDate?.take(4).orEmpty(),
    overview = overview,
    posterUrl = posterUrl(posterPath),
    backdropUrl = backdropUrl(backdropPath),
    rating = voteAverage,
    voteCount = voteCount,
    runtimeMinutes = null,
    seasonCount = null,
    episodeCount = null,
    genres = genreLabels(),
    moodReason = "",
    trailer = null,
    watchProviders = emptyList(),
    justWatchLink = null
)

fun TmdbTvDto.genreLabels(): List<String> =
    genreIds.mapNotNull { Genre.fromTvGenreId(it)?.label }

/** TV equivalent of [TmdbMovieDto.toDomainLight] - no per-title detail call. */
fun TmdbTvDto.toDomainLight(): Movie = Movie(
    tmdbId = id,
    mediaType = MediaType.SERIES,
    title = name,
    year = firstAirDate?.take(4).orEmpty(),
    overview = overview,
    posterUrl = posterUrl(posterPath),
    backdropUrl = backdropUrl(backdropPath),
    rating = voteAverage,
    voteCount = voteCount,
    runtimeMinutes = null,
    seasonCount = null,
    episodeCount = null,
    genres = genreLabels(),
    moodReason = "",
    trailer = null,
    watchProviders = emptyList(),
    justWatchLink = null
)

fun TmdbProviderEntryDto.toDomain(): com.arka.moodflix.domain.model.OttProvider =
    com.arka.moodflix.domain.model.OttProvider(
        id = providerId,
        name = providerName,
        logoUrl = logoUrl(logoPath)
    )