package com.arka.moodflix.domain.model

/** What a resolved title actually is. Drives which TMDB detail shape applies. */
enum class MediaType {
    MOVIE, SERIES
}

/**
 * What the user asked to see on the discover screen. BOTH is the default,
 * since people browsing by mood usually don't pre-sort into "movie night" vs
 * "show night" - they just want something that fits the mood.
 */
enum class MediaTypeFilter(val label: String) {
    MOVIES("Movies"),
    SERIES("Series"),
    BOTH("Both")
}