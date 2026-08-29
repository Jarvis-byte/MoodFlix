package com.arka.moodflix.ui.splash

import androidx.lifecycle.ViewModel
import com.arka.moodflix.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    val isLoggedIn: Boolean get() = authRepository.currentUser != null
}
