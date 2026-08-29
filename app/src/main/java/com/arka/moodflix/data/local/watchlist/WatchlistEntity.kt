package com.arka.moodflix.data.local.watchlist

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * [id] is "$tmdbId:$mediaType" - movie and TV id spaces overlap, so the
 * numeric id alone isn't unique (same story as [com.arka.moodflix.domain.model.MediaType]
 * being required everywhere else a bare tmdbId is used).
 */
@Entity(tableName = "watchlist")
data class WatchlistEntity(
    @PrimaryKey val id: String,
    val tmdbId: Int,
    val mediaType: String,
    val title: String,
    val year: String,
    val posterUrl: String?,
    val rating: Float,
    val addedAtEpochMillis: Long
)
