package com.arka.moodflix.ui.watchlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    private val watchlistRepository: WatchlistRepository
) : ViewModel() {

    /** Null while the first Room emission hasn't landed yet - distinguishes "loading" from "actually empty". */
    val watchlist: StateFlow<List<Movie>?> = watchlistRepository.observeWatchlist()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** Every item here is already saved, so this always removes it. */
    fun remove(movie: Movie) {
        viewModelScope.launch { watchlistRepository.toggle(movie) }
    }
}
