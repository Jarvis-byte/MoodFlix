package com.arka.moodflix.core.analytics

import com.arka.moodflix.domain.model.Genre
import com.arka.moodflix.domain.model.MediaTypeFilter
import com.arka.moodflix.domain.model.Mood

sealed interface AnalyticsEvent {

    data object DiscoverScreenOpened : AnalyticsEvent

    data class SearchStarted(
        val mood: Mood,
        val genre: Genre,
        val mediaFilter: MediaTypeFilter,
        val minRating: Float,
        val ottCount: Int,
        val hasFreeText: Boolean
    ) : AnalyticsEvent

    data class SearchSucceeded(
        val resultCount: Int,
        val answeredBy: String
    ) : AnalyticsEvent

    data object SearchFellBackToTmdb : AnalyticsEvent

    data object SearchFailed : AnalyticsEvent

    data object LoadMoreTapped : AnalyticsEvent

    data class TitleDetailOpened(
        val tmdbId: Int,
        val title: String,
        val mediaType: String
    ) : AnalyticsEvent

    data class TrailerPlayed(
        val tmdbId: Int,
        val title: String
    ) : AnalyticsEvent

    data class AiProviderConnected(val provider: String) : AnalyticsEvent

    data class AiProviderRemoved(val provider: String) : AnalyticsEvent

    /** [method] is "google", "email_signin", or "email_signup". */
    data class LoginSucceeded(val method: String) : AnalyticsEvent

    data class LoginFailed(val method: String) : AnalyticsEvent

    data object LoggedOut : AnalyticsEvent

    // --- Watchlist ---

    data class WatchlistToggled(
        val tmdbId: Int,
        val title: String,
        val mediaType: String,
        val added: Boolean
    ) : AnalyticsEvent

    data object WatchlistScreenOpened : AnalyticsEvent

    // --- Search tab (title search, distinct from the mood-based Discover flow) ---

    data object SearchScreenOpened : AnalyticsEvent

    /** Raw query text is never sent - just enough to gauge usage and typical query length. */
    data class SearchTabQuerySubmitted(val queryLength: Int) : AnalyticsEvent

    data class SearchTabResultsReturned(val resultCount: Int) : AnalyticsEvent

    // --- Discovery surfaces added after launch, tracked separately so their
    // engagement can be judged against the rest of Discover ---

    data class TopMoviesBannerTapped(val tmdbId: Int) : AnalyticsEvent

    data class SimilarSeeAllTapped(val tmdbId: Int) : AnalyticsEvent

    // --- Monetization ---

    data object RewardedAdShown : AnalyticsEvent
    data object RewardedAdCompleted : AnalyticsEvent
    data object RewardedAdSkippedOrFailed : AnalyticsEvent

    // --- Settings ---

    data class DarkThemeToggled(val enabled: Boolean) : AnalyticsEvent

    /** [language] is "en" or "hi". */
    data class LanguageToggled(val language: String) : AnalyticsEvent
}