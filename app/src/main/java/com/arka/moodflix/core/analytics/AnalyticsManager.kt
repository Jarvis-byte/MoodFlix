package com.arka.moodflix.core.analytics

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyticsManager @Inject constructor() {

    private val fa: FirebaseAnalytics by lazy { Firebase.analytics }

    fun log(event: AnalyticsEvent) {
        when (event) {

            AnalyticsEvent.DiscoverScreenOpened ->
                fa.logEvent("discover_opened", null)

            is AnalyticsEvent.SearchStarted ->
                fa.logEvent("search_started", bundle {
                    putString("mood", event.mood.name)
                    putString("genre", event.genre.name)
                    putString("media_filter", event.mediaFilter.name)
                    putDouble("min_rating", event.minRating.toDouble())
                    putInt("ott_platforms_selected", event.ottCount)
                    putString("has_free_text", event.hasFreeText.toString())
                })

            is AnalyticsEvent.SearchSucceeded ->
                fa.logEvent("search_succeeded", bundle {
                    putInt("result_count", event.resultCount)
                    putString("answered_by", event.answeredBy)
                })

            AnalyticsEvent.SearchFellBackToTmdb ->
                fa.logEvent("search_tmdb_fallback", null)

            AnalyticsEvent.SearchFailed ->
                fa.logEvent("search_failed", null)

            AnalyticsEvent.LoadMoreTapped ->
                fa.logEvent("load_more_tapped", null)

            is AnalyticsEvent.TitleDetailOpened ->
                fa.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle {
                    putString(FirebaseAnalytics.Param.CONTENT_TYPE, event.mediaType)
                    putString(FirebaseAnalytics.Param.ITEM_ID, event.tmdbId.toString())
                    putString(FirebaseAnalytics.Param.ITEM_NAME, event.title.take(100))
                })

            is AnalyticsEvent.TrailerPlayed ->
                fa.logEvent("trailer_played", bundle {
                    putString(FirebaseAnalytics.Param.ITEM_ID, event.tmdbId.toString())
                    putString(FirebaseAnalytics.Param.ITEM_NAME, event.title.take(100))
                })

            is AnalyticsEvent.AiProviderConnected ->
                fa.logEvent("ai_provider_connected", bundle {
                    putString("provider", event.provider)
                })

            is AnalyticsEvent.AiProviderRemoved ->
                fa.logEvent("ai_provider_removed", bundle {
                    putString("provider", event.provider)
                })

            is AnalyticsEvent.LoginSucceeded ->
                fa.logEvent(FirebaseAnalytics.Event.LOGIN, bundle {
                    putString(FirebaseAnalytics.Param.METHOD, event.method)
                })

            is AnalyticsEvent.LoginFailed ->
                fa.logEvent("login_failed", bundle {
                    putString(FirebaseAnalytics.Param.METHOD, event.method)
                })

            AnalyticsEvent.LoggedOut ->
                fa.logEvent("logged_out", null)

            is AnalyticsEvent.WatchlistToggled ->
                fa.logEvent(
                    if (event.added) "watchlist_added" else "watchlist_removed",
                    bundle {
                        putString(FirebaseAnalytics.Param.ITEM_ID, event.tmdbId.toString())
                        putString(FirebaseAnalytics.Param.ITEM_NAME, event.title.take(100))
                        putString(FirebaseAnalytics.Param.CONTENT_TYPE, event.mediaType)
                    }
                )

            AnalyticsEvent.WatchlistScreenOpened ->
                fa.logEvent("watchlist_opened", null)

            AnalyticsEvent.SearchScreenOpened ->
                fa.logEvent("search_tab_opened", null)

            is AnalyticsEvent.SearchTabQuerySubmitted ->
                fa.logEvent(FirebaseAnalytics.Event.SEARCH, bundle {
                    putInt("query_length", event.queryLength)
                })

            is AnalyticsEvent.SearchTabResultsReturned ->
                fa.logEvent("search_tab_results_returned", bundle {
                    putInt("result_count", event.resultCount)
                })

            is AnalyticsEvent.TopMoviesBannerTapped ->
                fa.logEvent("top_movies_banner_tapped", bundle {
                    putString(FirebaseAnalytics.Param.ITEM_ID, event.tmdbId.toString())
                })

            is AnalyticsEvent.SimilarSeeAllTapped ->
                fa.logEvent("similar_see_all_tapped", bundle {
                    putString(FirebaseAnalytics.Param.ITEM_ID, event.tmdbId.toString())
                })

            AnalyticsEvent.RewardedAdShown ->
                fa.logEvent("rewarded_ad_shown", null)

            AnalyticsEvent.RewardedAdCompleted ->
                fa.logEvent("rewarded_ad_completed", null)

            AnalyticsEvent.RewardedAdSkippedOrFailed ->
                fa.logEvent("rewarded_ad_skipped_or_failed", null)

            is AnalyticsEvent.DarkThemeToggled ->
                fa.logEvent("dark_theme_toggled", bundle {
                    putString("enabled", event.enabled.toString())
                })

            is AnalyticsEvent.LanguageToggled ->
                fa.logEvent("language_toggled", bundle {
                    putString("language", event.language)
                })
        }
    }

    private inline fun bundle(block: Bundle.() -> Unit): Bundle =
        Bundle().apply(block)
}