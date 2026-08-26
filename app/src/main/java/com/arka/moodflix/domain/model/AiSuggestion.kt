package com.arka.moodflix.domain.model

/** The raw shape we ask the LLM to return. Deliberately minimal. */
data class AiSuggestion(
    val title: String,
    val year: String,
    val reason: String
)
