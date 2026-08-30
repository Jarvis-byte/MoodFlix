package com.arka.moodflix.ui.watchlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arka.moodflix.core.analytics.AnalyticsEvent
import com.arka.moodflix.core.analytics.AnalyticsManager
import com.arka.moodflix.domain.model.Movie
import com.arka.moodflix.domain.repository.WatchlistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class WatchlistViewModel @Inject constructor(
    private val watchlistRepository: WatchlistRepository,
    private val analytics: AnalyticsManager
) : ViewModel() {

    /** Null while the first Room emission hasn't landed yet - distinguishes "loading" from "actually empty". */
    val watchlist: StateFlow<List<Movie>?> = watchlistRepository.observeWatchlist()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        analytics.log(AnalyticsEvent.WatchlistScreenOpened)
    }

    /** Every item here is already saved, so this always removes it. */
    fun remove(movie: Movie) {
        viewModelScope.launch {
            val added = watchlistRepository.toggle(movie)
            analytics.log(
                AnalyticsEvent.WatchlistToggled(
                    tmdbId = movie.tmdbId,
                    title = movie.title,
                    mediaType = movie.mediaType.name,
                    added = added
                )
            )
        }
    }
}
