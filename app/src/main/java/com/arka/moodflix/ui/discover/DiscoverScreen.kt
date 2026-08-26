package com.arka.moodflix.ui.discover

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arka.moodflix.core.AppError
import com.arka.moodflix.domain.model.Genre
import com.arka.moodflix.domain.model.Mood
import com.arka.moodflix.domain.model.Movie
import com.arka.moodflix.ui.components.MoodChip
import com.arka.moodflix.ui.components.MovieCard
import com.arka.moodflix.ui.components.RatingSlider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreen(
    onOpenSettings: () -> Unit,
    onOpenMovie: (Int) -> Unit,
    onPlayTrailer: (String) -> Unit,
    viewModel: DiscoverViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val hasProvider by viewModel.hasAnyProvider.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "MoodFlix",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            item {
                Text(
                    text = "Aaj kya dekhna hai?",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Pick a mood. The rest is on us.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                MoodGrid(
                    selected = state.selectedMood,
                    onSelect = { viewModel.onEvent(DiscoverEvent.MoodSelected(it)) }
                )
            }

            item {
                Text(
                    text = "Genre",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(Genre.entries.toList()) { genre ->
                        MoodChip(
                            label = genre.label,
                            selected = state.selectedGenre == genre,
                            onClick = { viewModel.onEvent(DiscoverEvent.GenreSelected(genre)) }
                        )
                    }
                }
            }

            item {
                RatingSlider(
                    value = state.minRating,
                    onValueChange = { viewModel.onEvent(DiscoverEvent.RatingChanged(it)) }
                )
            }

            item {
                OutlinedTextField(
                    value = state.freeText,
                    onValueChange = { viewModel.onEvent(DiscoverEvent.FreeTextChanged(it)) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Anything else? (optional)") },
                    placeholder = { Text("no subtitles, under 2 hours, nothing depressing") },
                    shape = RoundedCornerShape(16.dp),
                    maxLines = 3,
                    keyboardOptions = KeyboardOptions.Default
                )
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { viewModel.onEvent(DiscoverEvent.Search) },
                        enabled = state.canSearch && hasProvider,
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        if (state.phase is DiscoverUiState.Phase.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Find me something", style = MaterialTheme.typography.labelLarge)
                        }
                    }

                    OutlinedButton(
                        onClick = { viewModel.onEvent(DiscoverEvent.SurpriseMe) },
                        enabled = hasProvider && state.phase !is DiscoverUiState.Phase.Loading,
                        modifier = Modifier.height(54.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.Casino, contentDescription = "Surprise me")
                    }
                }
            }

            if (!hasProvider) {
                item { ConnectProviderPrompt(onOpenSettings) }
            }

            state.error?.let { error ->
                item { ErrorBanner(error = error, onOpenSettings = onOpenSettings) }
            }

            (state.phase as? DiscoverUiState.Phase.Loading)?.let { loading ->
                item { LoadingRow(loading.label) }
            }

            state.answeredBy?.takeIf { state.results.isNotEmpty() }?.let { provider ->
                item {
                    Text(
                        text = "Curated by $provider",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(state.results, key = { it.tmdbId }) { movie ->
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + slideInVertically { it / 4 }
                ) {
                    MovieCard(
                        movie = movie,
                        onClick = { onOpenMovie(movie.tmdbId) },
                        onPlayTrailer = { movie.trailer?.let { onPlayTrailer(it.youtubeKey) } }
                    )
                }
            }

            if (state.results.isNotEmpty() && state.phase is DiscoverUiState.Phase.Done) {
                item {
                    TextButton(
                        onClick = { viewModel.onEvent(DiscoverEvent.LoadMore) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Show me more like these")
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun MoodGrid(
    selected: Mood?,
    onSelect: (Mood) -> Unit
) {
    // A simple wrapped flow: two moods per row keeps labels readable
    // without pulling in a FlowRow experimental API.
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Mood.entries.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { mood ->
                    MoodChip(
                        label = mood.label,
                        selected = selected == mood,
                        onClick = { onSelect(mood) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun LoadingRow(label: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(18.dp),
            strokeWidth = 2.dp,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ConnectProviderPrompt(onOpenSettings: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Connect an AI provider to start",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "MoodFlix runs on your own free Gemini, OpenAI or Claude key. Nothing is charged to you by this app.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
            Button(onClick = onOpenSettings, shape = RoundedCornerShape(14.dp)) {
                Text("Connect a provider")
            }
        }
    }
}

@Composable
private fun ErrorBanner(error: AppError, onOpenSettings: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = error.message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error
        )
        if (error is AppError.QuotaExceeded || error is AppError.InvalidKey ||
            error is AppError.NoKeysConfigured
        ) {
            TextButton(onClick = onOpenSettings) {
                Text("Add a backup provider")
            }
        }
    }
}
