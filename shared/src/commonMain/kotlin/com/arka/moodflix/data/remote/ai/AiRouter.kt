package com.arka.moodflix.data.remote.ai

import com.arka.moodflix.core.AppError
import com.arka.moodflix.core.AppResult
import com.arka.moodflix.core.Logger
import com.arka.moodflix.domain.model.AiProviderType
import com.arka.moodflix.domain.model.AiSuggestion
import com.arka.moodflix.domain.repository.AiKeyRepository

data class RoutedSuggestions(
    val suggestions: List<AiSuggestion>,
    val answeredBy: AiProviderType
)

/**
 * Walks the user's connected providers in their configured order and returns
 * the first successful response.
 *
 * A provider is skipped and the next one tried when the failure is *its* fault
 * (quota exhausted, dead key). A network failure aborts the whole chain
 * immediately, because trying three more hosts with no connectivity just makes
 * the user wait three timeouts for the same error.
 */
class AiRouter(
    clients: Set<AiProviderClient>,
    private val keyRepository: AiKeyRepository
) {
    private val byType: Map<AiProviderType, AiProviderClient> = clients.associateBy { it.type }

    suspend fun suggest(prompt: String): AppResult<RoutedSuggestions> {
        val order = keyRepository.fallbackOrder()
        var lastError: AppError = AppError.NoKeysConfigured()
        var triedAny = false

        for (type in order) {
            val client = byType[type] ?: continue
            val key = keyRepository.getKey(type)?.takeIf { it.isNotBlank() } ?: continue

            triedAny = true
            Logger.d(TAG, "Trying ${type.displayName}")

            when (val result = client.suggest(key, prompt)) {
                is AppResult.Success -> {
                    Logger.d(TAG, "${type.displayName} answered with ${result.data.size} picks")
                    return AppResult.Success(RoutedSuggestions(result.data, type))
                }

                is AppResult.Failure -> {
                    lastError = result.error

                    // No point walking the chain when the phone itself is offline.
                    if (result.error is AppError.Network) {
                        return AppResult.Failure(result.error)
                    }

                    Logger.w(TAG, "${type.displayName} failed: ${result.error.message}, falling back")
                }
            }
        }

        return AppResult.Failure(
            if (!triedAny) AppError.NoKeysConfigured() else lastError
        )
    }

    private companion object {
        const val TAG = "AiRouter"
    }
}
