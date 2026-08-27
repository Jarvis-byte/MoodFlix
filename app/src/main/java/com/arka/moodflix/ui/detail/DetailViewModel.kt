package com.arka.moodflix.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val movieId: Int = checkNotNull(savedStateHandle["movieId"])

    // TMDB movie ids and TV ids are separate spaces, so which endpoint to
    // call must travel with the id rather than being guessed.
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
                is AppResult.Success -> DetailUiState(isLoading = false, movie = result.data)
                is AppResult.Failure -> DetailUiState(isLoading = false, error = result.error)
            }
        }
    }
}