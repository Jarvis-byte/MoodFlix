package com.arka.moodflix.di

import android.content.Context
import androidx.room.Room
import com.arka.moodflix.data.local.watchlist.MoodFlixDatabase
import com.arka.moodflix.data.local.watchlist.WatchlistDao
import com.arka.moodflix.data.repository.RoomWatchlistRepository
import com.arka.moodflix.domain.repository.WatchlistRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MoodFlixDatabase =
        Room.databaseBuilder(context, MoodFlixDatabase::class.java, "moodflix.db").build()

    @Provides
    fun provideWatchlistDao(database: MoodFlixDatabase): WatchlistDao = database.watchlistDao()

    @Provides
    @Singleton
    fun provideWatchlistRepository(dao: WatchlistDao): WatchlistRepository =
        RoomWatchlistRepository(dao)
}
