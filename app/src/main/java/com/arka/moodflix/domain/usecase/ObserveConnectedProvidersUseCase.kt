package com.arka.moodflix.domain.usecase

import com.arka.moodflix.domain.model.ConnectedProvider
import com.arka.moodflix.domain.repository.AiKeyRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveConnectedProvidersUseCase @Inject constructor(
    private val repository: AiKeyRepository
) {
    operator fun invoke(): Flow<List<ConnectedProvider>> = repository.connectedProviders
}
