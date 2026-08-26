package com.arka.moodflix.domain.usecase

import com.arka.moodflix.domain.model.MoodQuery
import com.arka.moodflix.domain.repository.MovieRepository
import com.arka.moodflix.domain.repository.RecommendationState
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetRecommendationsUseCase @Inject constructor(
    private val repository: MovieRepository
) {
    operator fun invoke(query: MoodQuery): Flow<RecommendationState> = repository.recommend(query)
}
