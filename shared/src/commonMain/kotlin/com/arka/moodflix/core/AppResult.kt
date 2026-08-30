package com.arka.moodflix.core

/**
 * A tiny result wrapper so the data layer never throws into the ViewModel.
 * Named AppResult to avoid clashing with kotlin.Result.
 */
sealed interface AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>
    data class Failure(val error: AppError) : AppResult<Nothing>
}

/**
 * Deliberately carries no message text - commonMain has no Android resource
 * system to localize from, so every consumer maps these cases to a
 * user-facing, localized string in its own UI layer instead of baking
 * English text in here.
 */
sealed class AppError {
    data object Network : AppError()
    data class QuotaExceeded(val provider: String) : AppError()
    data class InvalidKey(val provider: String) : AppError()
    data object NoKeysConfigured : AppError()
    data object ParseFailed : AppError()
    data class ProviderError(val provider: String, val statusCode: Int) : AppError()
    data object NoMatches : AppError()
    data object TitleLoadFailed : AppError()
    data object ProvidersLoadFailed : AppError()
    data object MonthlyTitlesLoadFailed : AppError()
    data object SearchFailed : AppError()
    data object SimilarLoadFailed : AppError()
    data object Unknown : AppError()
}

inline fun <T, R> AppResult<T>.map(transform: (T) -> R): AppResult<R> = when (this) {
    is AppResult.Success -> AppResult.Success(transform(data))
    is AppResult.Failure -> this
}

fun <T> AppResult<T>.getOrNull(): T? = (this as? AppResult.Success)?.data
