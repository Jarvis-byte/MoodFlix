package com.arka.moodflix.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arka.moodflix.core.AppError
import com.arka.moodflix.core.AppResult
import com.arka.moodflix.domain.model.Genre
import com.arka.moodflix.domain.model.MediaType
import com.arka.moodflix.domain.model.MediaTypeFilter
import com.arka.moodflix.domain.model.Movie
import com.arka.moodflix.domain.model.watchlistId
import com.arka.moodflix.domain.repository.WatchlistRepository
import com.arka.moodflix.domain.usecase.GetTopMoviesThisMonthUseCase
import com.arka.moodflix.domain.usecase.SearchMoviesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val mediaFilter: MediaTypeFilter = MediaTypeFilter.BOTH,
    val selectedGenre: Genre = Genre.ANY,
    val isLoadingBrowse: Boolean = true,
    val browseMovies: List<Movie> = emptyList(),
    val isSearching: Boolean = false,
    val searchResults: List<Movie> = emptyList(),
    val isRefreshing: Boolean = false,
    val error: AppError? = null
) {
    /**
     * What the grid actually shows: search results while a query is active,
     * browse list otherwise. [mediaFilter] and [selectedGenre] are applied
     * client-side here - for the browse list they're also applied
     * server-side (TMDB discover supports a genre param), so this is a
     * no-op re-filter there; for search results TMDB's search endpoint
     * doesn't support genre filtering at all, so this is the only place
     * it takes effect.
     */
    val displayedMovies: List<Movie> get() {
        var base = if (query.isBlank()) browseMovies else searchResults
        base = when (mediaFilter) {
            MediaTypeFilter.MOVIES -> base.filter { it.mediaType == MediaType.MOVIE }
            MediaTypeFilter.SERIES -> base.filter { it.mediaType == MediaType.SERIES }
            MediaTypeFilter.BOTH -> base
        }
        return if (selectedGenre == Genre.ANY) {
            base
        } else {
            base.filter { it.genres.contains(selectedGenre.label) }
        }
    }
    val isLoading: Boolean get() = if (query.isBlank()) isLoadingBrowse else isSearching
}

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val getTopMoviesThisMonth: GetTopMoviesThisMonthUseCase,
    private val searchMovies: SearchMoviesUseCase,
    private val watchlistRepository: WatchlistRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    val savedIds: StateFlow<Set<String>> = watchlistRepository.observeWatchlist()
        .map { movies -> movies.map { it.watchlistId }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    private var searchJob: Job? = null
    private var filterRefetchJob: Job? = null

    init {
        viewModelScope.launch { loadBrowseMovies(forceRefresh = false) }
    }

    fun onMediaFilterChange(filter: MediaTypeFilter) {
        _uiState.update { it.copy(mediaFilter = filter) }
    }

    fun onGenreChange(genre: Genre) {
        _uiState.update { it.copy(selectedGenre = genre) }
        refetchBrowseForFilters()
    }

    /**
     * Genre only needs a network call for the browse list (TMDB discover
     * supports it server-side); search results are re-filtered client-side
     * via [SearchUiState.displayedMovies] with no extra call. The
     * repository also caches per filter combo, so a repeat visit to an
     * already-seen genre in this session costs nothing either way.
     */
    private fun refetchBrowseForFilters() {
        if (_uiState.value.query.isNotBlank()) return
        filterRefetchJob?.cancel()
        filterRefetchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            loadBrowseMovies(forceRefresh = false)
        }
    }

    fun toggleWatchlist(movie: Movie) {
        viewModelScope.launch { watchlistRepository.toggle(movie) }
    }

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
        searchJob?.cancel()

        if (query.isBlank()) {
            _uiState.update { it.copy(isSearching = false, searchResults = emptyList()) }
            return
        }

        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            runSearch(query, forceRefresh = false)
        }
    }

    /**
     * Pull-to-refresh is the only thing allowed to force a fresh network
     * call - every other revisit (switching tabs, retyping an already
     * searched query) is served from the repository's cache.
     */
    fun onRefresh() {
        searchJob?.cancel()
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            val query = _uiState.value.query
            if (query.isBlank()) {
                loadBrowseMovies(forceRefresh = true)
            } else {
                runSearch(query, forceRefresh = true)
            }
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    private suspend fun runSearch(query: String, forceRefresh: Boolean) {
        _uiState.update { it.copy(isSearching = true, error = null) }
        when (val result = searchMovies(query, forceRefresh)) {
            is AppResult.Success -> _uiState.update {
                it.copy(isSearching = false, searchResults = result.data)
            }
            is AppResult.Failure -> _uiState.update {
                it.copy(isSearching = false, error = result.error)
            }
        }
    }

    private suspend fun loadBrowseMovies(forceRefresh: Boolean) {
        _uiState.update { it.copy(isLoadingBrowse = true, error = null) }
        val today = LocalDate.now()
        val format = DateTimeFormatter.ISO_LOCAL_DATE
        val from = today.withDayOfMonth(1).format(format)
        val to = today.withDayOfMonth(today.lengthOfMonth()).format(format)
        val genre = _uiState.value.selectedGenre

        when (
            val result = getTopMoviesThisMonth(
                from, to,
                genre = genre,
                forceRefresh = forceRefresh
            )
        ) {
            is AppResult.Success -> _uiState.update {
                it.copy(isLoadingBrowse = false, browseMovies = result.data)
            }
            is AppResult.Failure -> _uiState.update {
                it.copy(isLoadingBrowse = false, error = result.error)
            }
        }
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 400L
    }
}
