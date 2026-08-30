package com.arka.moodflix.ui.discover

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.arka.moodflix.R
import com.arka.moodflix.core.localizedLabel
import com.arka.moodflix.domain.model.Genre
import com.arka.moodflix.domain.model.MediaType
import com.arka.moodflix.domain.model.MediaTypeFilter
import com.arka.moodflix.domain.model.Mood
import com.arka.moodflix.domain.model.Movie
import com.arka.moodflix.ui.components.CoachMarkOverlay
import com.arka.moodflix.ui.components.CoachStep
import com.arka.moodflix.ui.components.MoodChip
import com.arka.moodflix.ui.components.ProviderChip
import com.arka.moodflix.ui.components.RatingSlider
import com.arka.moodflix.ui.components.ShimmerBlock
import com.arka.moodflix.ui.components.coachTarget
import com.arka.moodflix.ui.components.rememberShimmerBrush
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreen(
    onOpenSettings: () -> Unit,
    onSearch: (Mood, Genre, Float, String, List<Int>, MediaTypeFilter) -> Unit,
    onOpenMovie: (Int, MediaType) -> Unit,
    viewModel: DiscoverViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val hasProvider by viewModel.hasAnyProvider.collectAsStateWithLifecycle()
    val introCoachStep by viewModel.coachStep.collectAsStateWithLifecycle()

    // Screen-space bounds captured via onGloballyPositioned
    var settingsBounds by remember { mutableStateOf<Rect?>(null) }
    var moodGridBounds by remember { mutableStateOf<Rect?>(null) }

    // Local step index — resets only when introCoachStep flips from -1 to 0
    var localStep by remember(introCoachStep) {
        mutableIntStateOf(if (introCoachStep >= 0) 0 else -1)
    }

    val coachConnectAiTitle = stringResource(R.string.discover_coach_connect_ai_title)
    val coachConnectAiBody = stringResource(R.string.discover_coach_connect_ai_body)
    val coachPickMoodTitle = stringResource(R.string.discover_coach_pick_mood_title)
    val coachPickMoodBody = stringResource(R.string.discover_coach_pick_mood_body)

    val coachSteps = remember(settingsBounds, moodGridBounds) {
        listOf(
            CoachStep(
                title = coachConnectAiTitle,
                body = coachConnectAiBody,
                targetBounds = settingsBounds,
                spotlightRadius = 32.dp
            ),
            CoachStep(
                title = coachPickMoodTitle,
                body = coachPickMoodBody,
                targetBounds = moodGridBounds,
                spotlightRadius = 120.dp
            )
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    actions = {
                        IconButton(
                            onClick = onOpenSettings,
                            modifier = Modifier.coachTarget { settingsBounds = it }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = stringResource(R.string.cd_settings),
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

            if (state.isLoading) {
                DiscoverSkeleton(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                )
                return@Scaffold
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .imePadding(),
                // Vertical only - horizontal is applied per item instead of via
                // contentPadding, so the banner item can opt out and run edge-to-edge.
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(28.dp)
            ) {

                item {
                    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                        Text(
                            text = stringResource(R.string.discover_headline),
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.discover_subheadline),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (state.topMoviesThisMonth.isNotEmpty()) {
                    item {
                        TopMoviesBanner(
                            movies = state.topMoviesThisMonth,
                            onSelect = {
                                viewModel.logTopMoviesBannerTapped(it.tmdbId)
                                onOpenMovie(it.tmdbId, it.mediaType)
                            }
                        )
                    }
                }

                item {
                    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                        SectionHeader(stringResource(R.string.discover_looking_for))
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MediaTypeFilter.entries.forEach { filter ->
                                MoodChip(
                                    label = filter.localizedLabel(),
                                    selected = state.mediaFilter == filter,
                                    onClick = { viewModel.onEvent(DiscoverEvent.MediaFilterSelected(filter)) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                item {
                    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                        SectionHeader(stringResource(R.string.discover_mood))
                        Spacer(Modifier.height(8.dp))
                        MoodGrid(
                            selected = state.selectedMood,
                            onSelect = { viewModel.onEvent(DiscoverEvent.MoodSelected(it)) },
                            modifier = Modifier.coachTarget { moodGridBounds = it }
                        )
                    }
                }

                item {
                    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                        SectionHeader(stringResource(R.string.discover_genre))
                        Spacer(Modifier.height(8.dp))
                        LazyRow(
                            contentPadding = PaddingValues(start = 4.dp, end = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(Genre.entries.toList()) { genre ->
                                MoodChip(
                                    label = genre.localizedLabel(),
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
                                text = stringResource(
                                    R.string.discover_genre_no_tv_mapping,
                                    state.selectedGenre.localizedLabel()
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (state.availableProviders.isNotEmpty()) {
                    item {
                        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                            SectionHeader(stringResource(R.string.discover_where_do_you_watch))
                            Spacer(Modifier.height(8.dp))
                            LazyRow(
                                contentPadding = PaddingValues(start = 4.dp, end = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                item {
                                    MoodChip(
                                        label = stringResource(R.string.discover_all_platforms),
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
                                    text = stringResource(
                                        R.string.discover_platforms_selected,
                                        state.selectedProviderIds.size
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                item {
                    RatingSlider(
                        value = state.minRating,
                        onValueChange = { viewModel.onEvent(DiscoverEvent.RatingChanged(it)) },
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }

                item {
                    OutlinedTextField(
                        value = state.freeText,
                        onValueChange = { viewModel.onEvent(DiscoverEvent.FreeTextChanged(it)) },
                        modifier = Modifier
                            .padding(horizontal = 20.dp)
                            .fillMaxWidth(),
                        label = { Text(stringResource(R.string.discover_free_text_label)) },
                        placeholder = { Text(stringResource(R.string.discover_free_text_placeholder)) },
                        shape = RoundedCornerShape(16.dp),
                        maxLines = 3,
                        keyboardOptions = KeyboardOptions.Default
                    )
                }

                item {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(horizontal = 20.dp)
                    ) {
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
                            Text(stringResource(R.string.discover_find_me_something), style = MaterialTheme.typography.labelLarge)
                        }

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
                            Icon(Icons.Default.Casino, contentDescription = stringResource(R.string.discover_surprise_me))
                        }
                    }
                }

                if (!hasProvider) {
                    item {
                        ConnectProviderHint(
                            onOpenSettings = onOpenSettings,
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                    }
                }

                item { Spacer(Modifier.height(24.dp)) }
            }
        }

        // Coach mark overlay — sits on top of everything
        if (!state.isLoading && localStep >= 0 && localStep < coachSteps.size) {
            CoachMarkOverlay(
                steps = coachSteps,
                currentStep = localStep,
                onNext = { localStep++ },
                onFinish = {
                    localStep = -1
                    viewModel.advanceCoach()
                }
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp
        ),
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
    )
}

/**
 * Full-screen skeleton shown until every Discover network call (OTT
 * providers, top movies this month) has settled - nothing on this screen
 * pops in section by section, it's this skeleton or the whole layout at once.
 * Shapes roughly mirror the real layout below it so the swap doesn't jump.
 */
@Composable
private fun DiscoverSkeleton(modifier: Modifier = Modifier) {
    val brush = rememberShimmerBrush()

    // Horizontal padding is applied per block instead of once for the whole
    // Column, so the banner placeholder below can skip it and run edge-to-edge
    // - matching the real TopMoviesBanner it stands in for.
    val sidePadding = Modifier.padding(horizontal = 20.dp)

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        Column(sidePadding, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ShimmerBlock(brush, Modifier.fillMaxWidth(0.7f).height(34.dp))
            ShimmerBlock(brush, Modifier.fillMaxWidth(0.5f).height(18.dp))
        }

        ShimmerBlock(
            brush,
            Modifier.fillMaxWidth().height(230.dp),
            shape = RectangleShape
        )

        Column(sidePadding, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ShimmerBlock(brush, Modifier.fillMaxWidth(0.3f).height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(3) {
                    ShimmerBlock(brush, Modifier.weight(1f).height(44.dp))
                }
            }
        }

        Column(sidePadding, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ShimmerBlock(brush, Modifier.fillMaxWidth(0.2f).height(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(4) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ShimmerBlock(brush, Modifier.weight(1f).height(48.dp))
                        ShimmerBlock(brush, Modifier.weight(1f).height(48.dp))
                    }
                }
            }
        }

        Column(sidePadding, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ShimmerBlock(brush, Modifier.fillMaxWidth(0.25f).height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(5) {
                    ShimmerBlock(
                        brush,
                        Modifier.width(80.dp).height(36.dp),
                        shape = RoundedCornerShape(50)
                    )
                }
            }
        }

        ShimmerBlock(brush, sidePadding.fillMaxWidth(0.4f).height(24.dp))

        ShimmerBlock(
            brush,
            sidePadding.fillMaxWidth().height(80.dp),
            shape = RoundedCornerShape(16.dp)
        )

        Row(sidePadding, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ShimmerBlock(
                brush,
                Modifier.weight(1f).height(54.dp),
                shape = RoundedCornerShape(16.dp)
            )
            ShimmerBlock(
                brush,
                Modifier.width(54.dp).height(54.dp),
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}

/**
 * Wraps the carousel in a diagonal primary→secondary gradient card so this
 * section reads as a distinct "featured" banner rather than another plain
 * row on the page, the way the rest of Discover's flat-background sections do.
 */
@Composable
private fun TopMoviesBanner(
    movies: List<Movie>,
    onSelect: (Movie) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                // Fades to transparent at the top and bottom edges instead of
                // cutting off with a hard rectangular line, so the tinted
                // background blends into whatever background color sits above
                // and below it rather than looking like a pasted-on block.
                Brush.verticalGradient(
                    colorStops = arrayOf(
                        0f to Color.Transparent,
                        0.25f to MaterialTheme.colorScheme.primary.copy(alpha = 0.28f),
                        0.75f to MaterialTheme.colorScheme.secondary.copy(alpha = 0.28f),
                        1f to Color.Transparent
                    )
                )
            )
            .padding(vertical = 16.dp)
    ) {
        SectionHeader(
            title = stringResource(R.string.discover_top_movies_of_month),
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(Modifier.height(20.dp))
        TopMoviesCarousel(movies = movies, onSelect = onSelect)
    }
}

/**
 * Peeking carousel: the center card is fully visible, the neighbors peek in
 * from both edges via [HorizontalPager]'s own contentPadding, and it snaps
 * back to center on fling - all built into the pager, no manual math needed.
 *
 * Looping (so index 0 peeks the last item on its left) is done the standard
 * "virtual page count" way: the pager has a huge page count, and every
 * virtual page maps back onto the real list with `% movies.size`. Combined
 * with auto-scroll, that also means the pager never has to snap backwards
 * from the last item to the first - it just keeps incrementing.
 */
@Composable
private fun TopMoviesCarousel(
    movies: List<Movie>,
    onSelect: (Movie) -> Unit,
    modifier: Modifier = Modifier
) {
    val virtualPageCount = Int.MAX_VALUE
    val startPage = remember(movies.size) {
        val middle = virtualPageCount / 2
        middle - (middle % movies.size)
    }
    val pagerState = rememberPagerState(
        initialPage = startPage,
        pageCount = { virtualPageCount }
    )

    LaunchedEffect(pagerState, movies) {
        while (true) {
            delay(AUTO_SCROLL_DELAY_MS)
            if (!pagerState.isScrollInProgress) {
                pagerState.animateScrollToPage(pagerState.currentPage + 1)
            }
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 64.dp),
            pageSpacing = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            val movie = movies[page.mod(movies.size)]
            val scale by animateFloatAsState(
                targetValue = if (page == pagerState.currentPage) 1f else 0.9f,
                label = "carouselCardScale"
            )
            TopMovieCard(
                movie = movie,
                onClick = { onSelect(movie) },
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer(scaleX = scale, scaleY = scale)
            )
        }

        Spacer(Modifier.height(10.dp))

        CarouselIndicator(
            pageCount = movies.size,
            currentPage = pagerState.currentPage.mod(movies.size),
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.CenterHorizontally),
            alignment = Alignment.CenterHorizontally
        )
    }
}

@Composable
private fun CarouselIndicator(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier,
    alignment: Alignment.Horizontal = Alignment.CenterHorizontally
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp, alignment)
    ) {
        repeat(pageCount) { index ->
            val selected = index == currentPage
            Box(
                modifier = Modifier
                    .size(if (selected) 8.dp else 6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        }
                    )
            )
        }
    }
}

private const val AUTO_SCROLL_DELAY_MS = 4_000L

@Composable
private fun TopMovieCard(
    movie: Movie,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(190.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = movie.backdropUrl ?: movie.posterUrl,
            contentDescription = movie.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f)),
                        startY = 60f
                    )
                )
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp)
        ) {
            Text(
                text = movie.title,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.height(14.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = String.format("%.1f", movie.rating),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.9f)
                )
                if (movie.year.isNotBlank()) {
                    Text(
                        text = "  ·  ${movie.year}",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
        }
    }
}

@Composable
private fun MoodGrid(
    selected: Mood?,
    onSelect: (Mood) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Mood.entries.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { mood ->
                    MoodChip(
                        label = mood.localizedLabel(),
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
private fun ConnectProviderHint(onOpenSettings: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.discover_no_provider_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.discover_no_provider_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(10.dp))
            TextButton(onClick = onOpenSettings) {
                Text(stringResource(R.string.discover_connect_a_provider))
            }
        }
    }
}