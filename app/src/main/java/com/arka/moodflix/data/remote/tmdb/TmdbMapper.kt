package com.arka.moodflix.data.remote.tmdb

import com.arka.moodflix.domain.model.Genre
import com.arka.moodflix.domain.model.Movie
import com.arka.moodflix.domain.model.ProviderType
import com.arka.moodflix.domain.model.Trailer
import com.arka.moodflix.domain.model.WatchProvider

private const val IMAGE_BASE = "https://image.tmdb.org/t/p/"

fun posterUrl(path: String?): String? = path?.let { "${IMAGE_BASE}w500$it" }
fun backdropUrl(path: String?): String? = path?.let { "${IMAGE_BASE}w780$it" }
fun logoUrl(path: String?): String? = path?.let { "${IMAGE_BASE}w92$it" }

fun TmdbMovieDetailDto.toDomain(
    moodReason: String,
    countryCode: String
): Movie {
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

    val trailer = videos?.results
        ?.filter { it.site.equals("YouTube", true) }
        ?.sortedWith(
            compareByDescending<TmdbVideoDto> { it.type.equals("Trailer", true) }
                .thenByDescending { it.official }
        )
        ?.firstOrNull()
        ?.let { Trailer(it.key, it.name.ifBlank { "Trailer" }) }

    return Movie(
        tmdbId = id,
        title = title,
        year = releaseDate?.take(4).orEmpty(),
        overview = overview,
        posterUrl = posterUrl(posterPath),
        backdropUrl = backdropUrl(backdropPath),
        rating = voteAverage,
        voteCount = voteCount,
        runtimeMinutes = runtime?.takeIf { it > 0 },
        genres = genres.map { it.name },
        moodReason = moodReason,
        trailer = trailer,
        watchProviders = providers,
        justWatchLink = country?.link
    )
}

fun TmdbMovieDto.genreLabels(): List<String> =
    genreIds.mapNotNull { Genre.fromTmdbId(it)?.label }

fun TmdbProviderEntryDto.toDomain(): com.arka.moodflix.domain.model.OttProvider =
    com.arka.moodflix.domain.model.OttProvider(
        id = providerId,
        name = providerName,
        logoUrl = logoUrl(logoPath)
    )