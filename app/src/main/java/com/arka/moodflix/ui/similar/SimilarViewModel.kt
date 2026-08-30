package com.arka.moodflix.ui.similar

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arka.moodflix.core.AppError
import com.arka.moodflix.core.AppResult
import com.arka.moodflix.core.analytics.AnalyticsEvent
import com.arka.moodflix.core.analytics.AnalyticsManager
import com.arka.moodflix.domain.model.MediaType
import com.arka.moodflix.domain.model.Movie
import com.arka.moodflix.domain.model.watchlistId
import com.arka.moodflix.domain.repository.MovieRepository
import com.arka.moodflix.domain.repository.WatchlistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SimilarUiState(
    val isLoading: Boolean = true,
    val movies: List<Movie> = emptyList(),
    val error: AppError? = null
)

/**
 * Full "More like this" grid, reached from the 6th slot of the detail
 * screen's row. Refetches via [MovieRepository.getSimilar] rather than
 * receiving the list through nav args - it's cached per title for the
 * session, so this is a cache hit right after visiting the detail screen.
 */
@HiltViewModel
class SimilarViewModel @Inject constructor(
    private val repository: MovieRepository,
    private val watchlistRepository: WatchlistRepository,
    private val analytics: AnalyticsManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val movieId: Int = checkNotNull(savedStateHandle["movieId"])

    private val mediaType: MediaType = MediaType.valueOf(
        savedStateHandle.get<String>("mediaType") ?: MediaType.MOVIE.name
    )

    private val _uiState = MutableStateFlow(SimilarUiState())
    val uiState: StateFlow<SimilarUiState> = _uiState.asStateFlow()

    val savedIds: StateFlow<Set<String>> = watchlistRepository.observeWatchlist()
        .map { movies -> movies.map { it.watchlistId }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = SimilarUiState(isLoading = true)
            _uiState.value = when (val result = repository.getSimilar(movieId, mediaType)) {
                is AppResult.Success -> SimilarUiState(isLoading = false, movies = result.data)
                is AppResult.Failure -> SimilarUiState(isLoading = false, error = result.error)
            }
        }
    }

    fun toggleWatchlist(movie: Movie) {
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
