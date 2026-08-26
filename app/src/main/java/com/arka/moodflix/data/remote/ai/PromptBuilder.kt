package com.arka.moodflix.data.remote.ai

import com.arka.moodflix.domain.model.AiSuggestion
import com.arka.moodflix.domain.model.Genre
import com.arka.moodflix.domain.model.MoodQuery
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class SuggestionWire(
    val title: String,
    val year: String = "",
    @SerialName("reason") val reason: String = ""
)

object PromptBuilder {

    private const val COUNT = 8

    val systemInstruction = """
        You are a film curator with wide taste - arthouse, world cinema, and mainstream.
        You recommend films that match an emotional state, not just a genre label.
        
        Rules:
        - Return ONLY a JSON array. No prose, no markdown fences, no preamble.
        - Each element: {"title": string, "year": string, "reason": string}
        - "reason" is one sentence, max 20 words, addressed to the viewer,
          explaining why this fits their stated mood specifically.
        - Prefer real, verifiable films. Never invent titles.
        - Vary the picks: do not return $COUNT films by the same director or from
          the same franchise.
        - Do not include ratings or streaming availability. Those are looked up separately.
    """.trimIndent()

    fun build(query: MoodQuery): String = buildString {
        appendLine("Recommend exactly $COUNT films.")
        appendLine()
        appendLine("Mood: ${query.mood.label} - ${query.mood.promptDescriptor}")

        if (query.genre != Genre.ANY) {
            appendLine("Genre preference: ${query.genre.label}")
        }

        appendLine("Minimum quality bar: roughly ${query.minRating}/10 on IMDb or similar.")

        if (query.freeText.isNotBlank()) {
            appendLine("Extra context from the viewer: \"${query.freeText.take(300)}\"")
        }

        if (query.excludeTitles.isNotEmpty()) {
            appendLine("Already suggested, do NOT repeat: ${query.excludeTitles.joinToString(", ")}")
        }

        appendLine()
        append("Respond with the JSON array only.")
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Models wrap JSON in markdown fences, add "Here you go:", or emit a
     * {"movies": [...]} object no matter how firmly you ask. This grabs the
     * first balanced array in the text and parses that.
     */
    fun parseSuggestions(raw: String): List<AiSuggestion>? {
        val array = extractJsonArray(raw) ?: return null
        return runCatching {
            json.decodeFromString<List<SuggestionWire>>(array)
                .filter { it.title.isNotBlank() }
                .map { AiSuggestion(it.title.trim(), it.year.trim(), it.reason.trim()) }
        }.getOrNull()?.takeIf { it.isNotEmpty() }
    }

    private fun extractJsonArray(raw: String): String? {
        val text = raw.replace("```json", "").replace("```", "").trim()
        val start = text.indexOf('[')
        if (start == -1) return null

        var depth = 0
        var inString = false
        var escaped = false

        for (i in start until text.length) {
            val c = text[i]
            when {
                escaped -> escaped = false
                c == '\\' && inString -> escaped = true
                c == '"' -> inString = !inString
                inString -> Unit
                c == '[' -> depth++
                c == ']' -> {
                    depth--
                    if (depth == 0) return text.substring(start, i + 1)
                }
            }
        }
        return null
    }
}
