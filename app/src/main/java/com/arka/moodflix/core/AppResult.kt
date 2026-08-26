package com.arka.moodflix.core

/**
 * A tiny result wrapper so the data layer never throws into the ViewModel.
 * Named AppResult to avoid clashing with kotlin.Result.
 */
sealed interface AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>
    data class Failure(val error: AppError) : AppResult<Nothing>
}

sealed class AppError(open val message: String) {
    data class Network(override val message: String = "No internet connection") : AppError(message)
    data class QuotaExceeded(val provider: String) : AppError("$provider quota exhausted")
    data class InvalidKey(val provider: String) : AppError("$provider key is invalid or expired")
    data class NoKeysConfigured(override val message: String = "No AI provider connected") : AppError(message)
    data class ParseFailed(override val message: String = "Could not read the AI response") : AppError(message)
    data class Unknown(override val message: String = "Something went wrong") : AppError(message)
}

inline fun <T, R> AppResult<T>.map(transform: (T) -> R): AppResult<R> = when (this) {
    is AppResult.Success -> AppResult.Success(transform(data))
    is AppResult.Failure -> this
}

fun <T> AppResult<T>.getOrNull(): T? = (this as? AppResult.Success)?.data
