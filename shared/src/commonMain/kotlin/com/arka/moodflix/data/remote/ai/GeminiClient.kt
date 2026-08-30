package com.arka.moodflix.data.remote.ai

import com.arka.moodflix.core.AppError
import com.arka.moodflix.core.AppResult
import com.arka.moodflix.domain.model.AiProviderType
import com.arka.moodflix.domain.model.AiSuggestion
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.utils.io.errors.IOException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
private data class GeminiRequest(
    val contents: List<Content>,
    @SerialName("system_instruction") val systemInstruction: Content? = null,
    @SerialName("generationConfig") val generationConfig: GenerationConfig = GenerationConfig()
) {
    @Serializable data class Content(val parts: List<Part>, val role: String? = null)
    @Serializable data class Part(val text: String)
    @Serializable data class GenerationConfig(
        val temperature: Float = 0.9f,
        @SerialName("responseMimeType") val responseMimeType: String = "application/json"
    )
}

@Serializable
private data class GeminiResponse(val candidates: List<Candidate> = emptyList()) {
    @Serializable data class Candidate(val content: Content? = null)
    @Serializable data class Content(val parts: List<Part> = emptyList())
    @Serializable data class Part(val text: String = "")
}

class GeminiClient(private val client: HttpClient) : AiProviderClient {

    override val type = AiProviderType.GEMINI

    private val endpoint =
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"

    override suspend fun suggest(apiKey: String, prompt: String): AppResult<List<AiSuggestion>> {
        val body = GeminiRequest(
            contents = listOf(
                GeminiRequest.Content(listOf(GeminiRequest.Part(prompt)), role = "user")
            ),
            systemInstruction = GeminiRequest.Content(
                listOf(GeminiRequest.Part(PromptBuilder.systemInstruction))
            )
        )

        return try {
            val response: HttpResponse = client.post(endpoint) {
                header("x-goog-api-key", apiKey)
                contentType(ContentType.Application.Json)
                setBody(body)
            }

            when (response.status) {
                HttpStatusCode.TooManyRequests ->
                    return AppResult.Failure(AppError.QuotaExceeded(type.displayName))
                HttpStatusCode.Unauthorized, HttpStatusCode.Forbidden ->
                    return AppResult.Failure(AppError.InvalidKey(type.displayName))
                else -> if (!response.status.isSuccess()) {
                    return AppResult.Failure(
                        AppError.ProviderError(type.displayName, response.status.value)
                    )
                }
            }

            val text = response.body<GeminiResponse>()
                .candidates.firstOrNull()
                ?.content?.parts?.joinToString("") { it.text }
                .orEmpty()

            PromptBuilder.parseSuggestions(text)
                ?.let { AppResult.Success(it) }
                ?: AppResult.Failure(AppError.ParseFailed)
        } catch (e: IOException) {
            AppResult.Failure(AppError.Network)
        } catch (e: Exception) {
            AppResult.Failure(AppError.Unknown)
        }
    }
}
