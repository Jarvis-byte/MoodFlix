package com.arka.moodflix.ui.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arka.moodflix.core.AppResult
import com.arka.moodflix.data.local.UserPreferences
import com.arka.moodflix.domain.model.Genre
import com.arka.moodflix.domain.model.MediaTypeFilter
import com.arka.moodflix.domain.model.Mood
import com.arka.moodflix.domain.model.OttProvider
import com.arka.moodflix.domain.usecase.GetOttProvidersUseCase
import com.arka.moodflix.domain.usecase.ObserveConnectedProvidersUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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
    val freeText: String = "",
    val availableProviders: List<OttProvider> = emptyList(),
    val selectedProviderIds: Set<Int> = emptySet(),
    val mediaFilter: MediaTypeFilter = MediaTypeFilter.BOTH
) {
    val canSearch: Boolean get() = selectedMood != null
}

sealed interface DiscoverEvent {
    data class MoodSelected(val mood: Mood) : DiscoverEvent
    data class GenreSelected(val genre: Genre) : DiscoverEvent
    data class RatingChanged(val rating: Float) : DiscoverEvent
    data class FreeTextChanged(val text: String) : DiscoverEvent
    data class ProviderToggled(val id: Int) : DiscoverEvent
    data object ClearProviders : DiscoverEvent
    data class MediaFilterSelected(val filter: MediaTypeFilter) : DiscoverEvent
}

@HiltViewModel
class DiscoverViewModel @Inject constructor(
    observeProviders: ObserveConnectedProvidersUseCase,
    private val getOttProviders: GetOttProvidersUseCase,
    private val prefs: UserPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(DiscoverUiState())
    val uiState: StateFlow<DiscoverUiState> = _uiState.asStateFlow()

    /** Drives the "connect a provider first" hint - search still works without one. */
    val hasAnyProvider: StateFlow<Boolean> = observeProviders()
        .map { list -> list.any { it.hasKey } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /** Whether the Discover screen's one-time intro tooltip still needs to be shown. */
    val shouldShowIntro: StateFlow<Boolean> = prefs.discoverIntroSeen
        .map { seen -> !seen }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    init {
        loadOttProviders()
    }

    fun markIntroSeen() {
        viewModelScope.launch { prefs.markDiscoverIntroSeen() }
    }

    private fun loadOttProviders() {
        viewModelScope.launch {
            val region = prefs.watchCountry.first()
            // Fails silently: the OTT filter is a nice-to-have, not core
            // functionality, so a TMDB hiccup here shouldn't block search.
            when (val result = getOttProviders(region)) {
                is AppResult.Success ->
                    _uiState.update { it.copy(availableProviders = result.data) }
                is AppResult.Failure -> Unit
            }
        }
    }

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

            is DiscoverEvent.ProviderToggled -> _uiState.update { state ->
                val updated = if (event.id in state.selectedProviderIds) {
                    state.selectedProviderIds - event.id
                } else {
                    state.selectedProviderIds + event.id
                }
                state.copy(selectedProviderIds = updated)
            }

            DiscoverEvent.ClearProviders ->
                _uiState.update { it.copy(selectedProviderIds = emptySet()) }

            is DiscoverEvent.MediaFilterSelected ->
                _uiState.update { it.copy(mediaFilter = event.filter) }
        }
    }

    /** Picks a random mood and returns it so the caller can navigate immediately. */
    fun surpriseMood(): Mood {
        val mood = Mood.entries.random()
        _uiState.update { it.copy(selectedMood = mood) }
        return mood
    }
}