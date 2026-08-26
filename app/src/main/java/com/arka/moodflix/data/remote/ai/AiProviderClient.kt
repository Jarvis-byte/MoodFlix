package com.arka.moodflix.data.remote.ai

import com.arka.moodflix.core.AppResult
import com.arka.moodflix.domain.model.AiProviderType
import com.arka.moodflix.domain.model.AiSuggestion

/**
 * One implementation per vendor. Each knows how to shape its own request body
 * and how to read its own error codes; the router below stays vendor-agnostic.
 */
interface AiProviderClient {
    val type: AiProviderType

    suspend fun suggest(apiKey: String, prompt: String): AppResult<List<AiSuggestion>>
}
