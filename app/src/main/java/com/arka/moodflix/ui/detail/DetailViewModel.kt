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
import com.arka.moodflix.domain.repository.MovieRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DetailUiState(
    val isLoading: Boolean = true,
    val movie: Movie? = null,
    val error: AppError? = null
)

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val repository: MovieRepository,
    private val analytics: AnalyticsManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val movieId: Int = checkNotNull(savedStateHandle["movieId"])

    private val mediaType: MediaType = MediaType.valueOf(
        savedStateHandle.get<String>("mediaType") ?: MediaType.MOVIE.name
    )

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    init {
        load()
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