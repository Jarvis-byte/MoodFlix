package com.arka.moodflix.core

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

/**
 * English/Hindi toggle for the whole app, backed by AndroidX's per-app
 * language API rather than a custom DataStore flag. AppCompat stores the
 * choice itself (see the `autoStoreLocales` meta-data in the manifest) and
 * reapplies it on every app start, and setting it recreates every running
 * Activity to pick up the new resources - no manual persistence or
 * recomposition plumbing needed, unlike the dark-theme toggle.
 */
object AppLanguage {
    const val ENGLISH = "en"
    const val HINDI = "hi"

    val isHindi: Boolean
        get() = AppCompatDelegate.getApplicationLocales().toLanguageTags().startsWith(HINDI)

    fun setHindi(enabled: Boolean) {
        val tag = if (enabled) HINDI else ENGLISH
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
    }
}
