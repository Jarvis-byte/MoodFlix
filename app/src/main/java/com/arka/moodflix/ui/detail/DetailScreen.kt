package com.arka.moodflix.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.arka.moodflix.R
import com.arka.moodflix.core.localizedMessage
import com.arka.moodflix.domain.model.MediaType
import com.arka.moodflix.domain.model.Movie
import com.arka.moodflix.domain.model.ProviderType
import com.arka.moodflix.domain.model.WatchProvider
import com.arka.moodflix.domain.model.watchlistId
import com.arka.moodflix.ui.components.PosterGridCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    onBack: () -> Unit,
    onOpenMovie: (Int, MediaType) -> Unit,
    onPlayTrailer: (String) -> Unit,
    onOpenUrl: (String) -> Unit,
    onSeeMoreSimilar: (Int, MediaType) -> Unit,
    viewModel: DetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isSaved by viewModel.isSaved.collectAsStateWithLifecycle()
    val savedIds by viewModel.savedIds.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                },
                actions = {
                    if (state.movie != null) {
                        IconButton(onClick = viewModel::toggleWatchlist) {
                            Icon(
                                imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                                contentDescription = stringResource(
                                    if (isSaved) R.string.cd_remove_from_watchlist else R.string.cd_add_to_watchlist
                                ),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->

        when {
            state.isLoading -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }

            state.error != null -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = state.error?.localizedMessage().orEmpty(),
                        color = MaterialTheme.colorScheme.error
                    )
                    TextButton(onClick = viewModel::load) { Text(stringResource(R.string.action_try_again)) }
                }
            }

            else -> state.movie?.let { movie ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                ) {
                    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                                .clip(RoundedCornerShape(18.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            AsyncImage(
                                model = movie.trailer?.thumbnailUrl ?: movie.backdropUrl,
                                contentDescription = movie.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            movie.trailer?.let { trailer ->
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .size(58.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(MaterialTheme.colorScheme.primary)
                                        .clickable {
                                            viewModel.logTrailerPlayed()
                                            onPlayTrailer(trailer.youtubeKey)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.PlayArrow,
                                        contentDescription = stringResource(R.string.cd_play_trailer),
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(18.dp))

                        Text(
                            text = movie.title,
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        Spacer(Modifier.height(6.dp))

                        val seasonsText = movie.seasonCount?.let {
                            pluralStringResource(R.plurals.detail_seasons, it, it)
                        }
                        val episodesText = movie.episodeCount?.let {
                            stringResource(R.string.detail_episodes, it)
                        }
                        val minPerEpText = movie.runtimeMinutes?.let {
                            stringResource(R.string.detail_min_per_episode, it)
                        }
                        val minutesText = movie.runtimeMinutes?.let {
                            stringResource(R.string.detail_minutes, it)
                        }
                        Text(
                            text = buildString {
                                append(String.format("%.1f", movie.rating))
                                append(" · ")
                                append(movie.year)
                                if (movie.mediaType == MediaType.SERIES) {
                                    seasonsText?.let { append(" · $it") }
                                    episodesText?.let { append(" · $it") }
                                    minPerEpText?.let { append(" · $it") }
                                } else {
                                    minutesText?.let { append(" · $it") }
                                }
                                if (movie.genres.isNotEmpty()) {
                                    append(" · ${movie.genres.joinToString(", ")}")
                                }
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(Modifier.height(16.dp))

                        Text(
                            text = movie.overview,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        Spacer(Modifier.height(24.dp))

                        ProviderSection(
                            stringResource(R.string.detail_stream),
                            movie.watchProviders.filter { it.type == ProviderType.STREAM }
                        )
                        ProviderSection(
                            stringResource(R.string.detail_rent),
                            movie.watchProviders.filter { it.type == ProviderType.RENT }
                        )
                        ProviderSection(
                            stringResource(R.string.detail_buy),
                            movie.watchProviders.filter { it.type == ProviderType.BUY }
                        )

                        movie.justWatchLink?.let { link ->
                            Spacer(Modifier.height(12.dp))
                            Button(
                                onClick = { onOpenUrl(link) },
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text(stringResource(R.string.detail_see_all_watch_options))
                            }
                        }
                    }

                    if (state.similarMovies.isNotEmpty()) {
                        Spacer(Modifier.height(28.dp))
                        MoreLikeThisSection(
                            movies = state.similarMovies,
                            savedIds = savedIds,
                            onSelect = { onOpenMovie(it.tmdbId, it.mediaType) },
                            onToggleWatchlist = viewModel::toggleWatchlist,
                            onSeeMore = {
                                viewModel.logSeeMoreSimilarTapped()
                                onSeeMoreSimilar(movie.tmdbId, movie.mediaType)
                            }
                        )
                    }

                    Spacer(Modifier.height(40.dp))
                }
            }
        }
    }
}

@Composable
private fun ProviderSection(label: String, providers: List<WatchProvider>) {
    if (providers.isEmpty()) return

    Column(modifier = Modifier.padding(bottom = 16.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(providers) { provider ->
                AsyncImage(
                    model = provider.logoUrl,
                    contentDescription = provider.name,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
            }
        }
    }
}

/**
 * TMDB recommendations, as a plain horizontal row of poster cards below the
 * watch-options button. Capped at [ROW_LIMIT] cards; when there are more,
 * a trailing "See all" card opens the full grid instead of growing the row.
 */
@Composable
private fun MoreLikeThisSection(
    movies: List<Movie>,
    savedIds: Set<String>,
    onSelect: (Movie) -> Unit,
    onToggleWatchlist: (Movie) -> Unit,
    onSeeMore: () -> Unit
) {
    val visible = movies.take(ROW_LIMIT)
    val hasMore = movies.size > ROW_LIMIT

    Column {
        Text(
            text = stringResource(R.string.detail_more_like_this),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        Spacer(Modifier.height(8.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(visible, key = { it.tmdbId }) { movie ->
                PosterGridCard(
                    movie = movie,
                    onClick = { onSelect(movie) },
                    isSaved = movie.watchlistId in savedIds,
                    onToggleWatchlist = { onToggleWatchlist(movie) },
                    modifier = Modifier.width(150.dp)
                )
            }

            if (hasMore) {
                item(key = "see_more") {
                    SeeMoreCard(onClick = onSeeMore, modifier = Modifier.width(150.dp))
                }
            }
        }
    }
}

/** 6th slot in the "More like this" row - opens the full 2-column grid. */
@Composable
private fun SeeMoreCard(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.clickable(onClick = onClick)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.detail_see_all),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private const val ROW_LIMIT = 5
