package com.arka.moodflix.domain.model

/**
 * The raw shape we ask the LLM to return. [mediaType] is the AI's own guess
 * at movie vs. series - used to pick the right TMDB endpoint first, with the
 * other endpoint tried as a fallback if that guess doesn't resolve.
 */
data class AiSuggestion(
    val title: String,
    val year: String,
    val reason: String,
    val mediaType: MediaType
)