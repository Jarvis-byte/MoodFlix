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
private data class AnthropicRequest(
    val model: String,
    @SerialName("max_tokens") val maxTokens: Int = 1024,
    val system: String,
    val messages: List<Message>
) {
    @Serializable data class Message(val role: String, val content: String)
}

@Serializable
private data class AnthropicResponse(val content: List<Block> = emptyList()) {
    @Serializable data class Block(val type: String = "", val text: String = "")
}

@Singleton
class AnthropicClient @Inject constructor(
    private val okHttp: OkHttpClient,
    private val json: Json
) : AiProviderClient {

    override val type = AiProviderType.ANTHROPIC

    override suspend fun suggest(apiKey: String, prompt: String): AppResult<List<AiSuggestion>> =
        withContext(Dispatchers.IO) {
            val body = AnthropicRequest(
                model = "claude-sonnet-4-5",
                system = PromptBuilder.systemInstruction,
                messages = listOf(AnthropicRequest.Message("user", prompt))
            )

            val request = Request.Builder()
                .url("https://api.anthropic.com/v1/messages")
                .addHeader("x-api-key", apiKey)
                .addHeader("anthropic-version", "2023-06-01")
                .post(
                    json.encodeToString(AnthropicRequest.serializer(), body)
                        .toRequestBody("application/json".toMediaType())
                )
                .build()

            try {
                okHttp.newCall(request).execute().use { response ->
                    when {
                        response.code == 429 ->
                            return@withContext AppResult.Failure(AppError.QuotaExceeded(type.displayName))
                        response.code == 401 ->
                            return@withContext AppResult.Failure(AppError.InvalidKey(type.displayName))
                        !response.isSuccessful ->
                            return@withContext AppResult.Failure(
                                AppError.Unknown("${type.displayName} returned ${response.code}")
                            )
                    }

                    val payload = response.body?.string().orEmpty()
                    val text = json.decodeFromString(AnthropicResponse.serializer(), payload)
                        .content.filter { it.type == "text" }
                        .joinToString("") { it.text }

                    PromptBuilder.parseSuggestions(text)
                        ?.let { AppResult.Success(it) }
                        ?: AppResult.Failure(AppError.ParseFailed())
                }
            } catch (e: IOException) {
                AppResult.Failure(AppError.Network())
            } catch (e: Exception) {
                AppResult.Failure(AppError.Unknown(e.message ?: "Claude call failed"))
            }
        }
}
