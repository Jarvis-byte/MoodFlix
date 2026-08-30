package com.arka.moodflix.domain.repository

/**
 * Supplies the TMDB `language` query param at call time (e.g. "en-US",
 * "hi-IN"), so TmdbApi picks up the app's current language toggle on the
 * very next request without needing to be reconstructed - same reasoning as
 * [TmdbKeyProvider] sourcing its key from a value that can change at runtime.
 */
fun interface TmdbLanguageProvider {
    fun current(): String
}
