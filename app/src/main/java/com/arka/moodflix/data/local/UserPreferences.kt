package com.arka.moodflix.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.arka.moodflix.domain.model.AiProviderType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore("moodflix_prefs")

@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val orderKey = stringPreferencesKey("fallback_order")
    private val countryKey = stringPreferencesKey("watch_country")

    val fallbackOrder: Flow<List<AiProviderType>> = context.dataStore.data.map { prefs ->
        prefs[orderKey]
            ?.split(",")
            ?.mapNotNull { name -> AiProviderType.entries.firstOrNull { it.name == name } }
            ?.takeIf { it.isNotEmpty() }
            ?: AiProviderType.entries.toList()
    }

    /** Drives the "where to watch" lookup. Defaults to the device region. */
    val watchCountry: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[countryKey] ?: Locale.getDefault().country.ifBlank { "IN" }
    }

    suspend fun setFallbackOrder(order: List<AiProviderType>) {
        context.dataStore.edit { it[orderKey] = order.joinToString(",") { p -> p.name } }
    }

    suspend fun setWatchCountry(code: String) {
        context.dataStore.edit { it[countryKey] = code.uppercase() }
    }
}
