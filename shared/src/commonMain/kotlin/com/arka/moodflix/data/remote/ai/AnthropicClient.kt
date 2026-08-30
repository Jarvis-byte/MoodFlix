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

class AnthropicClient(private val client: HttpClient) : AiProviderClient {

    override val type = AiProviderType.ANTHROPIC

    override suspend fun suggest(apiKey: String, prompt: String): AppResult<List<AiSuggestion>> {
        val body = AnthropicRequest(
            model = "claude-sonnet-4-5",
            system = PromptBuilder.systemInstruction,
            messages = listOf(AnthropicRequest.Message("user", prompt))
        )

        return try {
            val response: HttpResponse = client.post("https://api.anthropic.com/v1/messages") {
                header("x-api-key", apiKey)
                header("anthropic-version", "2023-06-01")
                contentType(ContentType.Application.Json)
                setBody(body)
            }

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

            val text = response.body<AnthropicResponse>()
                .content.filter { it.type == "text" }
                .joinToString("") { it.text }

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
