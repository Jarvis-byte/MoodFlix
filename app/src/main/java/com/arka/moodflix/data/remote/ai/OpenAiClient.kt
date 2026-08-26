package com.arka.moodflix.data.remote.ai

import com.arka.moodflix.core.AppError
import com.arka.moodflix.core.AppResult
import com.arka.moodflix.domain.model.AiProviderType
import com.arka.moodflix.domain.model.AiSuggestion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
private data class OpenAiRequest(
    val model: String,
    val messages: List<Message>,
    val temperature: Float = 0.9f
) {
    @Serializable data class Message(val role: String, val content: String)
}

@Serializable
private data class OpenAiResponse(val choices: List<Choice> = emptyList()) {
    @Serializable data class Choice(val message: Message? = null)
    @Serializable data class Message(val content: String = "")
}

@Singleton
class OpenAiClient @Inject constructor(
    private val okHttp: OkHttpClient,
    private val json: Json
) : AiProviderClient {

    override val type = AiProviderType.OPENAI

    override suspend fun suggest(apiKey: String, prompt: String): AppResult<List<AiSuggestion>> =
        withContext(Dispatchers.IO) {
            val body = OpenAiRequest(
                model = "gpt-4o-mini",
                messages = listOf(
                    OpenAiRequest.Message("system", PromptBuilder.systemInstruction),
                    OpenAiRequest.Message("user", prompt)
                )
            )

            val request = Request.Builder()
                .url("https://api.openai.com/v1/chat/completions")
                .addHeader("Authorization", "Bearer $apiKey")
                .post(
                    json.encodeToString(OpenAiRequest.serializer(), body)
                        .toRequestBody("application/json".toMediaType())
                )
                .build()

            try {
                okHttp.newCall(request).execute().use { response ->
                    val payload = response.body?.string().orEmpty()

                    when {
                        // OpenAI uses 429 for both rate limit and exhausted credit;
                        // insufficient_quota means the key needs topping up, not retrying.
                        response.code == 429 ->
                            return@withContext AppResult.Failure(AppError.QuotaExceeded(type.displayName))
                        response.code == 401 ->
                            return@withContext AppResult.Failure(AppError.InvalidKey(type.displayName))
                        !response.isSuccessful ->
                            return@withContext AppResult.Failure(
                                AppError.Unknown("${type.displayName} returned ${response.code}")
                            )
                    }

                    val text = json.decodeFromString(OpenAiResponse.serializer(), payload)
                        .choices.firstOrNull()?.message?.content.orEmpty()

                    PromptBuilder.parseSuggestions(text)
                        ?.let { AppResult.Success(it) }
                        ?: AppResult.Failure(AppError.ParseFailed())
                }
            } catch (e: IOException) {
                AppResult.Failure(AppError.Network())
            } catch (e: Exception) {
                AppResult.Failure(AppError.Unknown(e.message ?: "OpenAI call failed"))
            }
        }
}
