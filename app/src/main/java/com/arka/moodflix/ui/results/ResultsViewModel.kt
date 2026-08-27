package com.arka.moodflix.ui.results

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arka.moodflix.core.AppError
import com.arka.moodflix.domain.model.Genre
import com.arka.moodflix.domain.model.Mood
import com.arka.moodflix.domain.model.MoodQuery
import com.arka.moodflix.domain.model.Movie
import com.arka.moodflix.domain.repository.RecommendationState
import com.arka.moodflix.domain.usecase.GetRecommendationsUseCase
import com.arka.moodflix.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class ResultsUiState(
    val mood: Mood,
    val phase: Phase = Phase.Loading("Reading your mood"),
    val results: List<Movie> = emptyList(),
    val answeredBy: String? = null,
    val usingTmdbFallback: Boolean = false,
    val error: AppError? = null
) {
    sealed interface Phase {
        data class Loading(val label: String) : Phase
        data object Done : Phase
    }
}

@HiltViewModel
class ResultsViewModel @Inject constructor(
    private val getRecommendations: GetRecommendationsUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val mood: Mood = Mood.valueOf(checkNotNull(savedStateHandle["mood"]))
    private val genre: Genre = Genre.valueOf(checkNotNull(savedStateHandle["genre"]))
    private val minRating: Float = checkNotNull(savedStateHandle.get<Float>("minRating"))
    private val freeText: String = Routes.decodeFreeText(checkNotNull(savedStateHandle["freeText"]))

    private val _uiState = MutableStateFlow(ResultsUiState(mood = mood))
    val uiState: StateFlow<ResultsUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        search(append = false)
    }

    fun retry() = search(append = false)

    fun loadMore() = search(append = true)

    private fun search(append: Boolean) {
        searchJob?.cancel()

        val query = MoodQuery(
            mood = mood,
            genre = genre,
            minRating = minRating,
            freeText = freeText,
            excludeTitles = if (append) _uiState.value.results.map { it.title } else emptyList()
        )

        searchJob = getRecommendations(query)
            .onEach { state -> reduce(state, append) }
            .launchIn(viewModelScope)
    }

    private fun reduce(state: RecommendationState, append: Boolean) {
        _uiState.update { current ->
            when (state) {
                RecommendationState.AskingAi -> current.copy(
                    phase = ResultsUiState.Phase.Loading("Reading your mood"),
                    error = null,
                    usingTmdbFallback = false,
                    results = if (append) current.results else emptyList()
                )

                is RecommendationState.AiResponded -> current.copy(
                    phase = ResultsUiState.Phase.Loading(
                        "Found ${state.titleCount} picks, looking them up"
                    ),
                    answeredBy = state.answeredBy
                )

                is RecommendationState.Enriched -> current.copy(
                    phase = ResultsUiState.Phase.Done,
                    results = if (append) {
                        (current.results + state.movies).distinctBy { it.tmdbId }
                    } else {
                        state.movies
                    },
                    answeredBy = state.answeredBy,
                    usingTmdbFallback = false,
                    error = null
                )

                is RecommendationState.FallbackToTmdb -> current.copy(
                    phase = ResultsUiState.Phase.Done,
                    results = if (append) {
                        (current.results + state.movies).distinctBy { it.tmdbId }
                    } else {
                        state.movies
                    },
                    answeredBy = null,
                    usingTmdbFallback = true,
                    error = state.reason
                )

                is RecommendationState.Failed -> current.copy(
                    phase = ResultsUiState.Phase.Done,
                    error = state.error
                )
            }
        }
    }
}