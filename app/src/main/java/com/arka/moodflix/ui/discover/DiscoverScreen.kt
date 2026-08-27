package com.arka.moodflix.ui.discover

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arka.moodflix.domain.model.Genre
import com.arka.moodflix.domain.model.MediaTypeFilter
import com.arka.moodflix.domain.model.Mood
import com.arka.moodflix.ui.components.MoodChip
import com.arka.moodflix.ui.components.ProviderChip
import com.arka.moodflix.ui.components.RatingSlider
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreen(
    onOpenSettings: () -> Unit,
    onSearch: (Mood, Genre, Float, String, List<Int>, MediaTypeFilter) -> Unit,
    viewModel: DiscoverViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val hasProvider by viewModel.hasAnyProvider.collectAsStateWithLifecycle()
    val shouldShowIntro by viewModel.shouldShowIntro.collectAsStateWithLifecycle()

    // A real TooltipState hoisted here (not created fresh inside the tooltip
    // block below) so we can call .show() on it programmatically - PlainTooltip
    // only reveals itself on long-press by default, which nobody discovers
    // on their own, so the first-run coach mark triggers it explicitly instead.
    val settingsTooltipState = rememberTooltipState()

    LaunchedEffect(shouldShowIntro) {
        if (shouldShowIntro) {
            delay(700) // let the screen settle before popping it
            settingsTooltipState.show()
            viewModel.markIntroSeen()
        }
    }

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
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                        tooltip = { PlainTooltip { Text("Connect your AI provider") } },
                        state = settingsTooltipState
                    ) {
                        IconButton(onClick = onOpenSettings) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = MaterialTheme.colorScheme.onBackground
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

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp)
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
                SectionHeader("Looking for")
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MediaTypeFilter.entries.forEach { filter ->
                        MoodChip(
                            label = filter.label,
                            selected = state.mediaFilter == filter,
                            onClick = { viewModel.onEvent(DiscoverEvent.MediaFilterSelected(filter)) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            item {
                SectionHeader("Mood")
                Spacer(Modifier.height(8.dp))
                MoodGrid(
                    selected = state.selectedMood,
                    onSelect = { viewModel.onEvent(DiscoverEvent.MoodSelected(it)) }
                )
            }

            item {
                SectionHeader("Genre")
                Spacer(Modifier.height(8.dp))
                LazyRow(
                    contentPadding = PaddingValues(start = 4.dp, end = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(Genre.entries.toList()) { genre ->
                        MoodChip(
                            label = genre.label,
                            selected = state.selectedGenre == genre,
                            onClick = { viewModel.onEvent(DiscoverEvent.GenreSelected(genre)) }
                        )
                    }
                }
                if (state.mediaFilter != MediaTypeFilter.MOVIES &&
                    state.selectedGenre.tvGenreId == null &&
                    state.selectedGenre != Genre.ANY
                ) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${state.selectedGenre.label} doesn't map to a TV genre - series results won't be genre-filtered.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (state.availableProviders.isNotEmpty()) {
                item {
                    SectionHeader("Where do you watch?")
                    Spacer(Modifier.height(8.dp))
                    LazyRow(
                        contentPadding = PaddingValues(start = 4.dp, end = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            MoodChip(
                                label = "All platforms",
                                selected = state.selectedProviderIds.isEmpty(),
                                onClick = { viewModel.onEvent(DiscoverEvent.ClearProviders) }
                            )
                        }
                        items(state.availableProviders, key = { it.id }) { provider ->
                            ProviderChip(
                                name = provider.name,
                                logoUrl = provider.logoUrl,
                                selected = provider.id in state.selectedProviderIds,
                                onClick = { viewModel.onEvent(DiscoverEvent.ProviderToggled(provider.id)) }
                            )
                        }
                    }
                    if (state.selectedProviderIds.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Only showing titles on ${state.selectedProviderIds.size} selected platform(s)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                        onClick = {
                            state.selectedMood?.let { mood ->
                                viewModel.logSearch()
                                onSearch(
                                    mood,
                                    state.selectedGenre,
                                    state.minRating,
                                    state.freeText,
                                    state.selectedProviderIds.toList(),
                                    state.mediaFilter
                                )
                            }
                        },
                        enabled = state.canSearch,
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Find me something", style = MaterialTheme.typography.labelLarge)
                    }

                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                        tooltip = { PlainTooltip { Text("Surprise me with a random mood") } },
                        state = rememberTooltipState()
                    ) {
                        OutlinedButton(
                            onClick = {
                                val mood = viewModel.surpriseMood()
                                viewModel.logSearch()
                                onSearch(
                                    mood,
                                    state.selectedGenre,
                                    state.minRating,
                                    state.freeText,
                                    state.selectedProviderIds.toList(),
                                    state.mediaFilter
                                )
                            },
                            modifier = Modifier.height(54.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Default.Casino, contentDescription = "Surprise me")
                        }
                    }
                }
            }

            if (!hasProvider) {
                item { ConnectProviderHint(onOpenSettings) }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp
        ),
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun MoodGrid(
    selected: Mood?,
    onSelect: (Mood) -> Unit,
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
private fun ConnectProviderHint(onOpenSettings: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "No AI provider connected yet",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "You can still search - MoodFlix will show popular picks from TMDB instead of AI-curated ones.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(10.dp))
            TextButton(onClick = onOpenSettings) {
                Text("Connect a provider")
            }
        }
    }
}