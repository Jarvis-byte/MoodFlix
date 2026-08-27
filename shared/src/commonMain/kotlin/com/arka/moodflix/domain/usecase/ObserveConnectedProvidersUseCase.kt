package com.arka.moodflix.domain.usecase

import com.arka.moodflix.domain.model.ConnectedProvider
import com.arka.moodflix.domain.repository.AiKeyRepository
import kotlinx.coroutines.flow.Flow

class ObserveConnectedProvidersUseCase(
    private val repository: AiKeyRepository
) {
    operator fun invoke(): Flow<List<ConnectedProvider>> = repository.connectedProviders
}
