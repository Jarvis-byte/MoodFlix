package com.arka.moodflix.data.local

import android.content.Context
import android.content.res.Configuration
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.arka.moodflix.domain.model.AiProviderType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Locale

private val Context.dataStore by preferencesDataStore("moodflix_prefs")

class AndroidUserPreferences(private val context: Context) : UserPreferences {

    private val orderKey = stringPreferencesKey("fallback_order")
    private val countryKey = stringPreferencesKey("watch_country")
    private val introSeenKey = booleanPreferencesKey("discover_intro_seen")
    private val darkThemeKey = booleanPreferencesKey("dark_theme_enabled")

    override val fallbackOrder: Flow<List<AiProviderType>> = context.dataStore.data.map { prefs ->
        prefs[orderKey]
            ?.split(",")
            ?.mapNotNull { name -> AiProviderType.entries.firstOrNull { it.name == name } }
            ?.takeIf { it.isNotEmpty() }
            ?: AiProviderType.entries.toList()
    }

    override val watchCountry: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[countryKey] ?: Locale.getDefault().country.ifBlank { "IN" }
    }

    override val discoverIntroSeen: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[introSeenKey] ?: false
    }

    override val darkThemeEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[darkThemeKey] ?: isSystemInDarkMode()
    }

    override suspend fun setFallbackOrder(order: List<AiProviderType>) {
        context.dataStore.edit { it[orderKey] = order.joinToString(",") { p -> p.name } }
    }

    override suspend fun setWatchCountry(code: String) {
        context.dataStore.edit { it[countryKey] = code.uppercase() }
    }

    override suspend fun markDiscoverIntroSeen() {
        context.dataStore.edit { it[introSeenKey] = true }
    }

    override suspend fun setDarkThemeEnabled(enabled: Boolean) {
        context.dataStore.edit { it[darkThemeKey] = enabled }
    }

    private fun isSystemInDarkMode(): Boolean {
        val uiMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return uiMode == Configuration.UI_MODE_NIGHT_YES
    }
}
