package com.arka.moodflix.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.arka.moodflix.domain.model.AiProviderType
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * API keys are encrypted at rest with a key held in the Android Keystore and
 * never leave the device. There is deliberately no backend and no analytics
 * on this file - other people's credentials are not our liability to hold.
 */
@Singleton
class SecureKeyStore @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            "moodflix_keys",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun put(type: AiProviderType, key: String) {
        prefs.edit().putString(type.name, key.trim()).apply()
    }

    fun get(type: AiProviderType): String? = prefs.getString(type.name, null)

    fun remove(type: AiProviderType) {
        prefs.edit().remove(type.name).apply()
    }

    fun configuredTypes(): Set<AiProviderType> =
        AiProviderType.entries.filter { !get(it).isNullOrBlank() }.toSet()
}
