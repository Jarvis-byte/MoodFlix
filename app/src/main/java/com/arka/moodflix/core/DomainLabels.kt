package com.arka.moodflix.core

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.arka.moodflix.R
import com.arka.moodflix.domain.model.Genre
import com.arka.moodflix.domain.model.MediaTypeFilter
import com.arka.moodflix.domain.model.Mood

/**
 * Mood/Genre/MediaTypeFilter live in the shared KMP module and hardcode
 * their [label] in English - commonMain has no Android resource system to
 * pull a translation from - so the Hindi translation is layered on here
 * instead, in the Android UI layer, rather than in the shared model.
 */
@Composable
fun Mood.localizedLabel(): String = stringResource(
    when (this) {
        Mood.COZY -> R.string.mood_cozy
        Mood.HYPED -> R.string.mood_hyped
        Mood.MELANCHOLY -> R.string.mood_melancholy
        Mood.MIND_BENDING -> R.string.mood_mind_bending
        Mood.ROMANTIC -> R.string.mood_romantic
        Mood.FUNNY -> R.string.mood_funny
        Mood.TENSE -> R.string.mood_tense
        Mood.INSPIRED -> R.string.mood_inspired
    }
)

@Composable
fun Genre.localizedLabel(): String = stringResource(
    when (this) {
        Genre.ANY -> R.string.genre_any
        Genre.ACTION -> R.string.genre_action
        Genre.ADVENTURE -> R.string.genre_adventure
        Genre.ANIMATION -> R.string.genre_animation
        Genre.COMEDY -> R.string.genre_comedy
        Genre.CRIME -> R.string.genre_crime
        Genre.DOCUMENTARY -> R.string.genre_documentary
        Genre.DRAMA -> R.string.genre_drama
        Genre.FANTASY -> R.string.genre_fantasy
        Genre.HORROR -> R.string.genre_horror
        Genre.MYSTERY -> R.string.genre_mystery
        Genre.ROMANCE -> R.string.genre_romance
        Genre.SCI_FI -> R.string.genre_sci_fi
        Genre.THRILLER -> R.string.genre_thriller
    }
)

@Composable
fun MediaTypeFilter.localizedLabel(): String = stringResource(
    when (this) {
        MediaTypeFilter.MOVIES -> R.string.media_filter_movies
        MediaTypeFilter.SERIES -> R.string.media_filter_series
        MediaTypeFilter.BOTH -> R.string.media_filter_both
    }
)
