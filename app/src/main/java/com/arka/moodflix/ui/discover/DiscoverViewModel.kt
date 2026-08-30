package com.arka.moodflix.ui.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arka.moodflix.core.analytics.AnalyticsEvent
import com.arka.moodflix.core.analytics.AnalyticsManager
import com.arka.moodflix.core.AppResult
import com.arka.moodflix.data.local.UserPreferences
import com.arka.moodflix.domain.model.Genre
import com.arka.moodflix.domain.model.MediaTypeFilter
import com.arka.moodflix.domain.model.Mood
import com.arka.moodflix.domain.model.Movie
import com.arka.moodflix.domain.model.OttProvider
import com.arka.moodflix.domain.usecase.GetOttProvidersUseCase
import com.arka.moodflix.domain.usecase.GetTopMoviesThisMonthUseCase
import com.arka.moodflix.domain.usecase.ObserveConnectedProvidersUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DiscoverUiState(
    val selectedMood: Mood? = null,
    val selectedGenre: Genre = Genre.ANY,
    val minRating: Float = 7.0f,
    val freeText: String = "",
    val availableProviders: List<OttProvider> = emptyList(),
    val selectedProviderIds: Set<Int> = emptySet(),
    val mediaFilter: MediaTypeFilter = MediaTypeFilter.BOTH,
    val topMoviesThisMonth: List<Movie> = emptyList(),
    private val isLoadingProviders: Boolean = true,
    private val isLoadingTopMovies: Boolean = true
) {
    val canSearch: Boolean get() = selectedMood != null

    /**
     * True until every screen-load network call (OTT providers, top movies
     * this month) has settled - success or failure. The screen shows a full
     * shimmer skeleton for the whole time rather than popping sections in
     * one by one as each call resolves.
     */
    val isLoading: Boolean get() = isLoadingProviders || isLoadingTopMovies
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
    private val getTopMoviesThisMonth: GetTopMoviesThisMonthUseCase,
    private val prefs: UserPreferences,
    private val analytics: AnalyticsManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(DiscoverUiState())
    val uiState: StateFlow<DiscoverUiState> = _uiState.asStateFlow()

    val hasAnyProvider: StateFlow<Boolean> = observeProviders()
        .map { list -> list.any { it.hasKey } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /**
     * Which coach mark step to show.
     * -1 = none (intro already seen), 0 = first time open.
     * The screen manages local step progression (0 → 1 → dismiss).
     */
    val coachStep: StateFlow<Int> = prefs.discoverIntroSeen
        .map { seen -> if (seen) -1 else 0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), -1)

    init {
        loadOttProviders()
        loadTopMoviesThisMonth()
        analytics.log(AnalyticsEvent.DiscoverScreenOpened)
    }

    /** Called when the user dismisses the last coach mark step. */
    fun advanceCoach() {
        viewModelScope.launch { prefs.markDiscoverIntroSeen() }
    }

    fun logSearch() {
        val s = _uiState.value
        analytics.log(
            AnalyticsEvent.SearchStarted(
                mood = s.selectedMood ?: return,
                genre = s.selectedGenre,
                mediaFilter = s.mediaFilter,
                minRating = s.minRating,
                ottCount = s.selectedProviderIds.size,
                hasFreeText = s.freeText.isNotBlank()
            )
        )
    }

    private fun loadOttProviders() {
        viewModelScope.launch {
            val region = prefs.watchCountry.first()
            val providers = when (val result = getOttProviders(region)) {
                is AppResult.Success -> result.data
                is AppResult.Failure -> emptyList()
            }
            _uiState.update {
                it.copy(availableProviders = providers, isLoadingProviders = false)
            }
        }
    }

    /** Feeds the peeking carousel between the header and "Looking for" - a silent, best-effort load. */
    private fun loadTopMoviesThisMonth() {
        viewModelScope.launch {
            val today = LocalDate.now()
            val format = DateTimeFormatter.ISO_LOCAL_DATE
            val from = today.withDayOfMonth(1).format(format)
            val to = today.withDayOfMonth(today.lengthOfMonth()).format(format)

            val movies = when (val result = getTopMoviesThisMonth(from, to, limit = 10)) {
                is AppResult.Success -> result.data
                is AppResult.Failure -> emptyList()
            }
            _uiState.update {
                it.copy(topMoviesThisMonth = movies, isLoadingTopMovies = false)
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

    fun logTopMoviesBannerTapped(tmdbId: Int) {
        analytics.log(AnalyticsEvent.TopMoviesBannerTapped(tmdbId))
    }

    fun surpriseMood(): Mood {
        val mood = Mood.entries.random()
        _uiState.update { it.copy(selectedMood = mood) }
        return mood
    }
}