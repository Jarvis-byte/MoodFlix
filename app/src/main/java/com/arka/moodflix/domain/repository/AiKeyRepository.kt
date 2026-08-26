package com.arka.moodflix.domain.repository

import com.arka.moodflix.domain.model.AiProviderType
import com.arka.moodflix.domain.model.ConnectedProvider
import kotlinx.coroutines.flow.Flow

interface AiKeyRepository {
    val connectedProviders: Flow<List<ConnectedProvider>>

    suspend fun saveKey(type: AiProviderType, key: String)
    suspend fun removeKey(type: AiProviderType)
    suspend fun getKey(type: AiProviderType): String?
    suspend fun setFallbackOrder(order: List<AiProviderType>)
    suspend fun fallbackOrder(): List<AiProviderType>
}
