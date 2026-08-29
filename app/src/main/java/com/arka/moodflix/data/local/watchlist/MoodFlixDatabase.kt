package com.arka.moodflix.data.local.watchlist

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [WatchlistEntity::class], version = 1, exportSchema = false)
abstract class MoodFlixDatabase : RoomDatabase() {
    abstract fun watchlistDao(): WatchlistDao
}
