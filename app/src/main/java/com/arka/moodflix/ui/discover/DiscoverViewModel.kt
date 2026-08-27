package com.arka.moodflix.ui.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arka.moodflix.domain.model.Genre
import com.arka.moodflix.domain.model.Mood
import com.arka.moodflix.domain.usecase.ObserveConnectedProvidersUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * Holds only what the user has picked. Actually running the search now
 * happens on the results screen, which owns its own loading/error state -
 * that way navigating back to Discover doesn't carry stale results with it.
 */
data class DiscoverUiState(
    val selectedMood: Mood? = null,
    val selectedGenre: Genre = Genre.ANY,
    val minRating: Float = 7.0f,
    val freeText: String = ""
) {
    val canSearch: Boolean get() = selectedMood != null
}

sealed interface DiscoverEvent {
    data class MoodSelected(val mood: Mood) : DiscoverEvent
    data class GenreSelected(val genre: Genre) : DiscoverEvent
    data class RatingChanged(val rating: Float) : DiscoverEvent
    data class FreeTextChanged(val text: String) : DiscoverEvent
}

@HiltViewModel
class DiscoverViewModel @Inject constructor(
    observeProviders: ObserveConnectedProvidersUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DiscoverUiState())
    val uiState: StateFlow<DiscoverUiState> = _uiState.asStateFlow()

    /** Drives the "connect a provider first" hint - search still works without one. */
    val hasAnyProvider: StateFlow<Boolean> = observeProviders()
        .map { list -> list.any { it.hasKey } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

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
        }
    }

    /** Picks a random mood and returns it so the caller can navigate immediately. */
    fun surpriseMood(): Mood {
        val mood = Mood.entries.random()
        _uiState.update { it.copy(selectedMood = mood) }
        return mood
    }
}