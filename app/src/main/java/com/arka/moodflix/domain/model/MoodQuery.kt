package com.arka.moodflix.domain.model

/** Everything the user picked on the discover screen. */
data class MoodQuery(
    val mood: Mood,
    val genre: Genre = Genre.ANY,
    val minRating: Float = 7.0f,
    val freeText: String = "",
    val excludeTitles: List<String> = emptyList(),
    val selectedProviderIds: List<Int> = emptyList()
)