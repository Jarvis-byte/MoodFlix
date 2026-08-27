package com.arka.moodflix.di

import com.arka.moodflix.domain.model.Genre
import com.arka.moodflix.domain.model.MediaTypeFilter
import com.arka.moodflix.domain.model.Mood

/**
 * Kotlin enum companion members (entries/values()) don't bridge predictably
 * to Swift through Objective-C interop, so these plain top-level lists give
 * Swift a reliable, direct way to enumerate cases - callable as
 * EnumSupportKt.allMoods / .allGenres / .allMediaTypeFilters.
 */
val allMoods: List<Mood> = Mood.entries
val allGenres: List<Genre> = Genre.entries
val allMediaTypeFilters: List<MediaTypeFilter> = MediaTypeFilter.entries
