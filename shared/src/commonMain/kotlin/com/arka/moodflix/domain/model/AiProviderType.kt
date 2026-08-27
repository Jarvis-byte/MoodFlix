package com.arka.moodflix.domain.model

enum class AiProviderType(
    val displayName: String,
    val keyConsoleUrl: String,
    val keyPrefixHint: String
) {
    GEMINI(
        displayName = "Google Gemini",
        keyConsoleUrl = "https://aistudio.google.com/app/apikey",
        keyPrefixHint = "AIza..."
    ),
    OPENAI(
        displayName = "OpenAI",
        keyConsoleUrl = "https://platform.openai.com/api-keys",
        keyPrefixHint = "sk-..."
    ),
    ANTHROPIC(
        displayName = "Anthropic Claude",
        keyConsoleUrl = "https://console.anthropic.com/settings/keys",
        keyPrefixHint = "sk-ant-..."
    );
}

/** A key the user has connected, plus its position in the fallback chain. */
data class ConnectedProvider(
    val type: AiProviderType,
    val hasKey: Boolean,
    val order: Int
)
