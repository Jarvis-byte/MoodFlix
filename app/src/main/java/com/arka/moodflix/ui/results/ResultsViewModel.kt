package com.arka.moodflix.ui.results

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arka.moodflix.R
import com.arka.moodflix.core.analytics.AnalyticsEvent
import com.arka.moodflix.core.analytics.AnalyticsManager
import com.arka.moodflix.core.AppError
import com.arka.moodflix.domain.model.Genre
import com.arka.moodflix.domain.model.MediaTypeFilter
import com.arka.moodflix.domain.model.Mood
import com.arka.moodflix.domain.model.MoodQuery
import com.arka.moodflix.domain.model.Movie
import com.arka.moodflix.domain.model.watchlistId
import com.arka.moodflix.domain.repository.RecommendationState
import com.arka.moodflix.domain.repository.WatchlistRepository
import com.arka.moodflix.domain.usecase.GetRecommendationsUseCase
import com.arka.moodflix.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ResultsUiState(
    val mood: Mood,
    val phase: Phase,
    val results: List<Movie> = emptyList(),
    val answeredBy: String? = null,
    val usingTmdbFallback: Boolean = false,
    val selectedProviderCount: Int = 0,
    val error: AppError? = null,
    val message: String? = null,
    val showTmdbFallbackDialog: Boolean = false
) {
    sealed interface Phase {
        data class Loading(val label: String) : Phase
        data object Done : Phase
    }
}

@HiltViewModel
class ResultsViewModel @Inject constructor(
    private val getRecommendations: GetRecommendationsUseCase,
    private val watchlistRepository: WatchlistRepository,
    private val analytics: AnalyticsManager,
    @ApplicationContext private val appContext: Context,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val savedIds: StateFlow<Set<String>> = watchlistRepository.observeWatchlist()
        .map { movies -> movies.map { it.watchlistId }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

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

    private val mood: Mood = Mood.valueOf(checkNotNull(savedStateHandle["mood"]))
    private val genre: Genre = Genre.valueOf(checkNotNull(savedStateHandle["genre"]))
    private val minRating: Float = checkNotNull(savedStateHandle.get<Float>("minRating"))
    private val freeText: String = Routes.decodeFreeText(checkNotNull(savedStateHandle["freeText"]))
    private val selectedProviderIds: List<Int> =
        Routes.decodeProviderIds(checkNotNull(savedStateHandle["providers"]))
    private val mediaFilter: MediaTypeFilter =
        MediaTypeFilter.valueOf(checkNotNull(savedStateHandle["mediaFilter"]))

    private val _uiState = MutableStateFlow(
        ResultsUiState(
            mood = mood,
            phase = ResultsUiState.Phase.Loading(appContext.getString(R.string.results_reading_your_mood)),
            selectedProviderCount = selectedProviderIds.size
        )
    )
    val uiState: StateFlow<ResultsUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null
    private var currentPage = 1
    private var pendingQuery: MoodQuery? = null
    private var pendingAppend = false

    init {
        search(append = false)
    }

    fun retry() = search(append = false)

    fun loadMore() {
        currentPage += 1
        analytics.log(AnalyticsEvent.LoadMoreTapped)
        search(append = true)
    }

    /** The rewarded ad was shown but closed before it finished - no more picks this time. */
    fun onAdClosedWithoutReward() {
        _uiState.update { it.copy(message = appContext.getString(R.string.results_ad_closed_early)) }
    }

    private fun search(append: Boolean) {
        searchJob?.cancel()
        if (!append) currentPage = 1

        val query = MoodQuery(
            mood = mood,
            genre = genre,
            minRating = minRating,
            freeText = freeText,
            excludeTitles = if (append) _uiState.value.results.map { it.title } else emptyList(),
            selectedProviderIds = selectedProviderIds,
            mediaFilter = mediaFilter,
            page = currentPage
        )
        pendingQuery = query
        pendingAppend = append

        searchJob = getRecommendations(query)
            .onEach { state -> reduce(state, append) }
            .launchIn(viewModelScope)
    }

    private fun reduce(state: RecommendationState, append: Boolean) {
        _uiState.update { current ->
            when (state) {
                RecommendationState.AskingAi -> current.copy(
                    phase = ResultsUiState.Phase.Loading(
                        appContext.getString(R.string.results_reading_your_mood)
                    ),
                    error = null,
                    message = null,
                    usingTmdbFallback = false,
                    results = if (append) current.results else emptyList()
                )

                is RecommendationState.AiResponded -> current.copy(
                    phase = ResultsUiState.Phase.Loading(
                        appContext.getString(R.string.results_found_picks, state.titleCount)
                    ),
                    answeredBy = state.answeredBy
                )

                is RecommendationState.Enriched -> {
                    val merged = if (append) {
                        (current.results + state.movies).distinctBy { it.tmdbId to it.mediaType }
                    } else {
                        state.movies
                    }
                    if (!append) {
                        analytics.log(
                            AnalyticsEvent.SearchSucceeded(
                                resultCount = merged.size,
                                answeredBy = state.answeredBy
                            )
                        )
                    }
                    current.copy(
                        phase = ResultsUiState.Phase.Done,
                        results = merged,
                        answeredBy = state.answeredBy,
                        usingTmdbFallback = false,
                        error = null,
                        message = noNewResultsMessage(append, current.results.size, merged.size)
                    )
                }

                is RecommendationState.AiFailed -> {
                    if (!append) analytics.log(AnalyticsEvent.SearchFailed)
                    current.copy(
                        phase = ResultsUiState.Phase.Done,
                        error = state.reason,
                        showTmdbFallbackDialog = true
                    )
                }

                is RecommendationState.Failed -> {
                    if (!append) analytics.log(AnalyticsEvent.SearchFailed)
                    current.copy(
                        phase = ResultsUiState.Phase.Done,
                        error = state.error
                    )
                }
            }
        }
    }

    private fun noNewResultsMessage(append: Boolean, previousSize: Int, mergedSize: Int): String? =
        if (append && mergedSize == previousSize) {
            appContext.getString(R.string.results_no_new_matches)
        } else {
            null
        }

    fun dismissMessage() {
        _uiState.update { it.copy(message = null) }
    }

    /** User agreed, from the fallback dialog, to switch to plain TMDB picks. */
    fun confirmTmdbFallback() {
        val query = pendingQuery ?: return
        val append = pendingAppend
        _uiState.update {
            it.copy(
                showTmdbFallbackDialog = false,
                phase = ResultsUiState.Phase.Loading(appContext.getString(R.string.results_tmdb_fallback_loading))
            )
        }
        viewModelScope.launch {
            val movies = getRecommendations.fallbackToTmdb(query)
            _uiState.update { current ->
                val merged = if (append) {
                    (current.results + movies).distinctBy { it.tmdbId to it.mediaType }
                } else {
                    movies
                }
                if (merged.isEmpty()) {
                    current.copy(phase = ResultsUiState.Phase.Done, error = AppError.NoMatches)
                } else {
                    if (!append) analytics.log(AnalyticsEvent.SearchFellBackToTmdb)
                    current.copy(
                        phase = ResultsUiState.Phase.Done,
                        results = merged,
                        answeredBy = null,
                        usingTmdbFallback = true,
                        error = null,
                        message = noNewResultsMessage(append, current.results.size, merged.size)
                    )
                }
            }
        }
    }

    /** User declined the fallback dialog - leave the AI error on screen. */
    fun dismissTmdbFallbackDialog() {
        _uiState.update { it.copy(showTmdbFallbackDialog = false) }
    }
}