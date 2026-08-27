package com.arka.moodflix.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.arka.moodflix.domain.model.AiProviderType

/**
 * Android implementation: keys are encrypted at rest with a key held in the
 * Android Keystore.
 */
class AndroidSecureKeyStore(context: Context) : SecureKeyStore {

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

    override fun put(type: AiProviderType, key: String) {
        prefs.edit().putString(type.name, key.trim()).apply()
    }

    override fun get(type: AiProviderType): String? = prefs.getString(type.name, null)

    override fun remove(type: AiProviderType) {
        prefs.edit().remove(type.name).apply()
    }

    override fun configuredTypes(): Set<AiProviderType> =
        AiProviderType.entries.filter { !get(it).isNullOrBlank() }.toSet()
}
