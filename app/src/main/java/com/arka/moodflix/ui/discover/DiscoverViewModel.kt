package com.arka.moodflix.ui.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arka.moodflix.core.AppError
import com.arka.moodflix.domain.model.Genre
import com.arka.moodflix.domain.model.Mood
import com.arka.moodflix.domain.model.MoodQuery
import com.arka.moodflix.domain.model.Movie
import com.arka.moodflix.domain.repository.RecommendationState
import com.arka.moodflix.domain.usecase.GetRecommendationsUseCase
import com.arka.moodflix.domain.usecase.ObserveConnectedProvidersUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class DiscoverUiState(
    val selectedMood: Mood? = null,
    val selectedGenre: Genre = Genre.ANY,
    val minRating: Float = 7.0f,
    val freeText: String = "",
    val phase: Phase = Phase.Idle,
    val results: List<Movie> = emptyList(),
    val answeredBy: String? = null,
    val error: AppError? = null
) {
    val canSearch: Boolean get() = selectedMood != null && phase !is Phase.Loading

    sealed interface Phase {
        data object Idle : Phase
        data class Loading(val label: String) : Phase
        data object Done : Phase
    }
}

sealed interface DiscoverEvent {
    data class MoodSelected(val mood: Mood) : DiscoverEvent
    data class GenreSelected(val genre: Genre) : DiscoverEvent
    data class RatingChanged(val rating: Float) : DiscoverEvent
    data class FreeTextChanged(val text: String) : DiscoverEvent
    data object Search : DiscoverEvent
    data object SurpriseMe : DiscoverEvent
    data object LoadMore : DiscoverEvent
    data object DismissError : DiscoverEvent
    data object Reset : DiscoverEvent
}

@HiltViewModel
class DiscoverViewModel @Inject constructor(
    private val getRecommendations: GetRecommendationsUseCase,
    observeProviders: ObserveConnectedProvidersUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DiscoverUiState())
    val uiState: StateFlow<DiscoverUiState> = _uiState.asStateFlow()

    /** Drives the "connect a provider first" empty state. */
    val hasAnyProvider: StateFlow<Boolean> = observeProviders()
        .map { list -> list.any { it.hasKey } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private var searchJob: Job? = null

    fun onEvent(event: DiscoverEvent) {
        when (event) {
            is DiscoverEvent.MoodSelected ->
                _uiState.update { it.copy(selectedMood = event.mood) }

            is DiscoverEvent.GenreSelected ->
                _uiState.update { it.copy(selectedGenre = event.genre) }

            is DiscoverEvent.RatingChanged ->
                _uiState.update { it.copy(minRating = event.rating) }

            is DiscoverEvent.FreeTextChanged ->
                _uiState.update { it.copy(freeText = event.text) }

            DiscoverEvent.Search -> search(append = false)

            DiscoverEvent.LoadMore -> search(append = true)

            DiscoverEvent.SurpriseMe -> {
                _uiState.update { it.copy(selectedMood = Mood.entries.random()) }
                search(append = false)
            }

            DiscoverEvent.DismissError -> _uiState.update { it.copy(error = null) }

            DiscoverEvent.Reset -> {
                searchJob?.cancel()
                _uiState.update {
                    it.copy(
                        phase = DiscoverUiState.Phase.Idle,
                        results = emptyList(),
                        answeredBy = null,
                        error = null
                    )
                }
            }
        }
    }

    private fun search(append: Boolean) {
        val mood = _uiState.value.selectedMood ?: return

        // Cancelling the previous job means rapid mood switching does not
        // race two responses into the same list.
        searchJob?.cancel()

        val query = MoodQuery(
            mood = mood,
            genre = _uiState.value.selectedGenre,
            minRating = _uiState.value.minRating,
            freeText = _uiState.value.freeText,
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
                    phase = DiscoverUiState.Phase.Loading("Reading your mood"),
                    error = null,
                    results = if (append) current.results else emptyList()
                )

                is RecommendationState.AiResponded -> current.copy(
                    phase = DiscoverUiState.Phase.Loading("Found ${state.titleCount} picks, looking them up"),
                    answeredBy = state.answeredBy
                )

                is RecommendationState.Enriched -> current.copy(
                    phase = DiscoverUiState.Phase.Done,
                    results = if (append) {
                        (current.results + state.movies).distinctBy { it.tmdbId }
                    } else {
                        state.movies
                    },
                    answeredBy = state.answeredBy,
                    error = null
                )

                is RecommendationState.Failed -> current.copy(
                    phase = DiscoverUiState.Phase.Idle,
                    error = state.error
                )
            }
        }
    }
}
