package com.arka.moodflix.data.remote.ai

import com.arka.moodflix.core.AppError
import com.arka.moodflix.core.AppResult
import com.arka.moodflix.domain.model.AiProviderType
import com.arka.moodflix.domain.model.AiSuggestion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

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

@Singleton
class GeminiClient @Inject constructor(
    private val okHttp: OkHttpClient,
    private val json: Json
) : AiProviderClient {

    override val type = AiProviderType.GEMINI

    private val endpoint =
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent"

    override suspend fun suggest(apiKey: String, prompt: String): AppResult<List<AiSuggestion>> =
        withContext(Dispatchers.IO) {
            val body = GeminiRequest(
                contents = listOf(
                    GeminiRequest.Content(listOf(GeminiRequest.Part(prompt)), role = "user")
                ),
                systemInstruction = GeminiRequest.Content(
                    listOf(GeminiRequest.Part(PromptBuilder.systemInstruction))
                )
            )

            val request = Request.Builder()
                .url(endpoint)
                .addHeader("x-goog-api-key", apiKey)
                .post(
                    json.encodeToString(GeminiRequest.serializer(), body)
                        .toRequestBody("application/json".toMediaType())
                )
                .build()

            try {
                okHttp.newCall(request).execute().use { response ->
                    when {
                        response.code == 429 ->
                            return@withContext AppResult.Failure(AppError.QuotaExceeded(type.displayName))
                        response.code == 401 || response.code == 403 ->
                            return@withContext AppResult.Failure(AppError.InvalidKey(type.displayName))
                        !response.isSuccessful ->
                            return@withContext AppResult.Failure(
                                AppError.Unknown("${type.displayName} returned ${response.code}")
                            )
                    }

                    val payload = response.body?.string().orEmpty()
                    val text = json.decodeFromString(GeminiResponse.serializer(), payload)
                        .candidates.firstOrNull()
                        ?.content?.parts?.joinToString("") { it.text }
                        .orEmpty()

                    PromptBuilder.parseSuggestions(text)
                        ?.let { AppResult.Success(it) }
                        ?: AppResult.Failure(AppError.ParseFailed())
                }
            } catch (e: IOException) {
                AppResult.Failure(AppError.Network())
            } catch (e: Exception) {
                AppResult.Failure(AppError.Unknown(e.message ?: "Gemini call failed"))
            }
        }
}
