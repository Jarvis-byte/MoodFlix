package com.arka.moodflix.data.repository

import com.arka.moodflix.data.local.SecureKeyStore
import com.arka.moodflix.data.local.UserPreferences
import com.arka.moodflix.domain.model.AiProviderType
import com.arka.moodflix.domain.model.ConnectedProvider
import com.arka.moodflix.domain.repository.AiKeyRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class AiKeyRepositoryImpl(
    private val keyStore: SecureKeyStore,
    private val prefs: UserPreferences
) : AiKeyRepository {

    // EncryptedSharedPreferences is not observable, so we nudge this on writes.
    private val revision = MutableStateFlow(0)

    override val connectedProviders: Flow<List<ConnectedProvider>> =
        combine(prefs.fallbackOrder, revision) { order, _ ->
            val configured = keyStore.configuredTypes()
            order.mapIndexed { index, type ->
                ConnectedProvider(
                    type = type,
                    hasKey = type in configured,
                    order = index
                )
            }
        }

    override suspend fun saveKey(type: AiProviderType, key: String) {
        keyStore.put(type, key)
        revision.value += 1
    }

    override suspend fun removeKey(type: AiProviderType) {
        keyStore.remove(type)
        revision.value += 1
    }

    override suspend fun getKey(type: AiProviderType): String? = keyStore.get(type)

    override suspend fun setFallbackOrder(order: List<AiProviderType>) {
        prefs.setFallbackOrder(order)
    }

    override suspend fun fallbackOrder(): List<AiProviderType> = prefs.fallbackOrder.first()
}
