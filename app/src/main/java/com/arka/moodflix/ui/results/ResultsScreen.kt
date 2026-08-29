package com.arka.moodflix.ui.results

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arka.moodflix.core.AppError
import com.arka.moodflix.core.ads.RewardedAdManagerEntryPoint
import com.arka.moodflix.domain.model.MediaType
import com.arka.moodflix.ui.components.MovieCard
import com.arka.moodflix.ui.components.ReelLoadingAnimation
import dagger.hilt.android.EntryPointAccessors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultsScreen(
    onBack: () -> Unit,
    onOpenMovie: (Int, MediaType) -> Unit,
    onPlayTrailer: (String) -> Unit,
    viewModel: ResultsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHost = remember { SnackbarHostState() }

    val context = LocalContext.current
    val adManager = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            RewardedAdManagerEntryPoint::class.java
        ).rewardedAdManager()
    }
    var isShowingAd by remember { mutableStateOf(false) }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHost.showSnackbar(it)
            viewModel.dismissMessage()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "${state.mood.label} picks",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        if (state.selectedProviderCount > 0) {
                            Text(
                                text = "Filtered to ${state.selectedProviderCount} platform(s)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
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
                state.phase is ResultsUiState.Phase.Loading && state.results.isEmpty() -> {
                    LoadingState((state.phase as ResultsUiState.Phase.Loading).label)
                }

                state.error != null && state.results.isEmpty() -> {
                    ErrorState(error = state.error!!, onRetry = viewModel::retry)
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {

                        if (state.usingTmdbFallback) {
                            item {
                                Text(
                                    text = "AI picks weren't available, so these are popular, well-rated films in your filters instead.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            state.answeredBy?.let { provider ->
                                item {
                                    Text(
                                        text = "Curated by $provider",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        items(state.results, key = { it.tmdbId }) { movie ->
                            AnimatedVisibility(
                                visible = true,
                                enter = fadeIn() + slideInVertically { it / 4 }
                            ) {
                                MovieCard(
                                    movie = movie,
                                    onClick = { onOpenMovie(movie.tmdbId, movie.mediaType) },
                                    onPlayTrailer = { movie.trailer?.let { onPlayTrailer(it.youtubeKey) } }
                                )
                            }
                        }

                        if (state.phase is ResultsUiState.Phase.Loading || isShowingAd) {
                            item {
                                LoadingRow(
                                    if (isShowingAd) "Loading ad" else (state.phase as ResultsUiState.Phase.Loading).label
                                )
                            }
                        } else if (state.results.isNotEmpty()) {
                            item {
                                TextButton(
                                    onClick = {
                                        val activity = context.findActivity()
                                        if (activity == null) {
                                            viewModel.loadMore()
                                        } else {
                                            isShowingAd = true
                                            adManager.showOrSkip(activity) {
                                                isShowingAd = false
                                                viewModel.loadMore()
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Show me more like these")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
private fun LoadingState(label: String) {
    ReelLoadingAnimation(
        statusLabel = label,
        modifier = Modifier.fillMaxSize()
    )
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
private fun ErrorState(error: AppError, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = error.message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )
        TextButton(onClick = onRetry) {
            Text("Try again")
        }
    }
}