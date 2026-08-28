package com.arka.moodflix.domain.repository

/**
 * Supplies the TMDB API key at call time rather than baking it into TmdbApi's
 * constructor, so a platform can source it from somewhere that changes at
 * runtime (Firebase Remote Config on Android) instead of a fixed build value.
 */
fun interface TmdbKeyProvider {
    suspend fun getKey(): String
}
