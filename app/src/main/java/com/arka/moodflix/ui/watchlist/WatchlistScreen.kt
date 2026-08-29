package com.arka.moodflix.ui.watchlist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arka.moodflix.domain.model.MediaType
import com.arka.moodflix.domain.model.Movie
import com.arka.moodflix.domain.model.watchlistId
import com.arka.moodflix.ui.components.PosterGridCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchlistScreen(
    onOpenMovie: (Int, MediaType) -> Unit,
    viewModel: WatchlistViewModel = hiltViewModel()
) {
    val watchlist by viewModel.watchlist.collectAsStateWithLifecycle()
    var pendingRemoval by remember { mutableStateOf<Movie?>(null) }

    pendingRemoval?.let { movie ->
        AlertDialog(
            onDismissRequest = { pendingRemoval = null },
            title = { Text("Remove from watchlist?") },
            text = { Text("\"${movie.title}\" will be removed from your watchlist.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.remove(movie)
                    pendingRemoval = null
                }) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRemoval = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Watchlist",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                watchlist == null -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                watchlist!!.isEmpty() -> {
                    Text(
                        text = "Your watchlist is empty.\nTap the bookmark icon on any title to save it here.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(horizontal = 32.dp)
                    )
                }

                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(watchlist!!, key = { it.watchlistId }) { movie ->
                            PosterGridCard(
                                movie = movie,
                                onClick = { onOpenMovie(movie.tmdbId, movie.mediaType) },
                                isSaved = true,
                                onToggleWatchlist = { pendingRemoval = movie }
                            )
                        }
                    }
                }
            }
        }
    }
}
