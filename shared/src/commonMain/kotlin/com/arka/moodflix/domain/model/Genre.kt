package com.arka.moodflix.domain.model

/**
 * TMDB genre ids are stable, so hardcoding them avoids an extra network call.
 *
 * [tvGenreId] is TMDB's separate TV genre id, when a real equivalent exists.
 * TV's genre list is smaller than movies' - there's no standalone TV Horror,
 * Romance, Thriller, or Adventure category; Action+Adventure merge into one
 * id, as do Sci-Fi+Fantasy. Where no honest equivalent exists, [tvGenreId] is
 * null and the genre filter is simply not applied on the TV side, rather than
 * silently mapping to something close-but-wrong.
 */
enum class Genre(val tmdbId: Int, val tvGenreId: Int?, val label: String) {
    ANY(-1, null, "Any"),
    ACTION(28, 10759, "Action"),        // TV: "Action & Adventure"
    ADVENTURE(12, 10759, "Adventure"),  // TV: "Action & Adventure"
    ANIMATION(16, 16, "Animation"),
    COMEDY(35, 35, "Comedy"),
    CRIME(80, 80, "Crime"),
    DOCUMENTARY(99, 99, "Documentary"),
    DRAMA(18, 18, "Drama"),
    FANTASY(14, 10765, "Fantasy"),      // TV: "Sci-Fi & Fantasy"
    HORROR(27, null, "Horror"),         // no TV equivalent
    MYSTERY(9648, 9648, "Mystery"),
    ROMANCE(10749, null, "Romance"),    // no TV equivalent
    SCI_FI(878, 10765, "Sci-Fi"),       // TV: "Sci-Fi & Fantasy"
    THRILLER(53, null, "Thriller");     // no TV equivalent

    companion object {
        fun fromTmdbId(id: Int) = entries.firstOrNull { it.tmdbId == id }
        fun fromTvGenreId(id: Int) = entries.firstOrNull { it.tvGenreId == id }
    }
}