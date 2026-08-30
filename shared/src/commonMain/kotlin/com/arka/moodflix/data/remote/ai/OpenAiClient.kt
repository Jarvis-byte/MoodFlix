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
import kotlinx.serialization.Serializable

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

class OpenAiClient(private val client: HttpClient) : AiProviderClient {

    override val type = AiProviderType.OPENAI

    override suspend fun suggest(apiKey: String, prompt: String): AppResult<List<AiSuggestion>> {
        val body = OpenAiRequest(
            model = "gpt-4o-mini",
            messages = listOf(
                OpenAiRequest.Message("system", PromptBuilder.systemInstruction),
                OpenAiRequest.Message("user", prompt)
            )
        )

        return try {
            val response: HttpResponse = client.post("https://api.openai.com/v1/chat/completions") {
                header("Authorization", "Bearer $apiKey")
                contentType(ContentType.Application.Json)
                setBody(body)
            }

            // OpenAI uses 429 for both rate limit and exhausted credit;
            // insufficient_quota means the key needs topping up, not retrying.
            when (response.status) {
                HttpStatusCode.TooManyRequests ->
                    return AppResult.Failure(AppError.QuotaExceeded(type.displayName))
                HttpStatusCode.Unauthorized ->
                    return AppResult.Failure(AppError.InvalidKey(type.displayName))
                else -> if (!response.status.isSuccess()) {
                    return AppResult.Failure(
                        AppError.ProviderError(type.displayName, response.status.value)
                    )
                }
            }

            val text = response.body<OpenAiResponse>()
                .choices.firstOrNull()?.message?.content.orEmpty()

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
