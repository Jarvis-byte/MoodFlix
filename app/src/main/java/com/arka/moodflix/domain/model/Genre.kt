package com.arka.moodflix.domain.model

/** TMDB genre ids are stable, so hardcoding them avoids an extra network call. */
enum class Genre(val tmdbId: Int, val label: String) {
    ANY(-1, "Any"),
    ACTION(28, "Action"),
    ADVENTURE(12, "Adventure"),
    ANIMATION(16, "Animation"),
    COMEDY(35, "Comedy"),
    CRIME(80, "Crime"),
    DOCUMENTARY(99, "Documentary"),
    DRAMA(18, "Drama"),
    FANTASY(14, "Fantasy"),
    HORROR(27, "Horror"),
    MYSTERY(9648, "Mystery"),
    ROMANCE(10749, "Romance"),
    SCI_FI(878, "Sci-Fi"),
    THRILLER(53, "Thriller");

    companion object {
        fun fromTmdbId(id: Int) = entries.firstOrNull { it.tmdbId == id }
    }
}
