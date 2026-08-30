package com.arka.moodflix.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arka.moodflix.core.analytics.AnalyticsEvent
import com.arka.moodflix.core.analytics.AnalyticsManager
import com.arka.moodflix.core.AppError
import com.arka.moodflix.core.AppResult
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DetailUiState(
    val isLoading: Boolean = true,
    val movie: Movie? = null,
    val similarMovies: List<Movie> = emptyList(),
    val error: AppError? = null
)

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val repository: MovieRepository,
    private val watchlistRepository: WatchlistRepository,
    private val analytics: AnalyticsManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val movieId: Int = checkNotNull(savedStateHandle["movieId"])

    private val mediaType: MediaType = MediaType.valueOf(
        savedStateHandle.get<String>("mediaType") ?: MediaType.MOVIE.name
    )

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    val isSaved: StateFlow<Boolean> = watchlistRepository.observeIsSaved(movieId, mediaType)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    /** Used for the "more like this" row - a title-agnostic saved-state lookup. */
    val savedIds: StateFlow<Set<String>> = watchlistRepository.observeWatchlist()
        .map { movies -> movies.map { it.watchlistId }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    init {
        load()
    }

    fun toggleWatchlist() {
        val movie = _uiState.value.movie ?: return
        toggleWatchlist(movie)
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

    fun logSeeMoreSimilarTapped() {
        _uiState.value.movie?.let { movie ->
            analytics.log(AnalyticsEvent.SimilarSeeAllTapped(movie.tmdbId))
        }
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = DetailUiState(isLoading = true)
            _uiState.value = when (val result = repository.getMovieDetail(movieId, mediaType)) {
                is AppResult.Success -> {
                    analytics.log(
                        AnalyticsEvent.TitleDetailOpened(
                            tmdbId = result.data.tmdbId,
                            title = result.data.title,
                            mediaType = result.data.mediaType.name
                        )
                    )
                    DetailUiState(isLoading = false, movie = result.data)
                }
                is AppResult.Failure -> DetailUiState(isLoading = false, error = result.error)
            }
            loadSimilar()
        }
    }

    /**
     * "More like this" - loaded after the main detail so the primary screen
     * never waits on it. A failure here is silent; it's a nice-to-have, not
     * core to the detail view.
     */
    private suspend fun loadSimilar() {
        if (_uiState.value.movie == null) return
        val result = repository.getSimilar(movieId, mediaType)
        if (result is AppResult.Success) {
            _uiState.update { it.copy(similarMovies = result.data) }
        }
    }

    fun logTrailerPlayed() {
        _uiState.value.movie?.let { movie ->
            analytics.log(
                AnalyticsEvent.TrailerPlayed(
                    tmdbId = movie.tmdbId,
                    title = movie.title
                )
            )
        }
    }
}