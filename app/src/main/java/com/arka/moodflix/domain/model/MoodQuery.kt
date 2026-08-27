package com.arka.moodflix.domain.model

/** Everything the user picked on the discover screen. */
data class MoodQuery(
    val mood: Mood,
    val genre: Genre = Genre.ANY,
    val minRating: Float = 7.0f,
    val freeText: String = "",
    val excludeTitles: List<String> = emptyList(),
    val selectedProviderIds: List<Int> = emptyList(),
    val mediaFilter: MediaTypeFilter = MediaTypeFilter.BOTH,
    // Only consulted by the TMDB-only fallback path. Each "load more" tap
    // increments this so a rate-limited or keyless session pages through
    // TMDB's ranked list instead of re-fetching the identical page 1 forever.
    val page: Int = 1
)