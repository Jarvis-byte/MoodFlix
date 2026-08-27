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
}