package com.arka.moodflix.ui.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arka.moodflix.core.analytics.AnalyticsEvent
import com.arka.moodflix.core.analytics.AnalyticsManager
import com.arka.moodflix.data.remote.config.FirebaseTmdbKeyProvider
import com.arka.moodflix.domain.model.User
import com.arka.moodflix.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val isSigningIn: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val tmdbKeyProvider: FirebaseTmdbKeyProvider,
    private val analytics: AnalyticsManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    /** True if a previous session is already active - skip the login screen. */
    val isAlreadySignedIn: Boolean get() = authRepository.currentUser != null

    fun signIn(context: Context, onSuccess: () -> Unit) {
        if (_uiState.value.isSigningIn) return
        _uiState.update { it.copy(isSigningIn = true, error = null) }

        viewModelScope.launch {
            authRepository.signInWithGoogle(context)
                .onSuccess {
                    tmdbKeyProvider.refresh()
                    analytics.log(AnalyticsEvent.LoginSucceeded)
                    _uiState.update { it.copy(isSigningIn = false) }
                    onSuccess()
                }
                .onFailure { error ->
                    analytics.log(AnalyticsEvent.LoginFailed)
                    _uiState.update {
                        it.copy(
                            isSigningIn = false,
                            error = error.message ?: "Sign-in failed. Try again."
                        )
                    }
                }
        }
    }

    fun signInWithEmail(email: String, password: String, onSuccess: () -> Unit) =
        submitEmailAuth(email, password, onSuccess) { authRepository.signInWithEmail(it, password) }

    fun signUpWithEmail(email: String, password: String, onSuccess: () -> Unit) =
        submitEmailAuth(email, password, onSuccess) { authRepository.signUpWithEmail(it, password) }

    private fun submitEmailAuth(
        email: String,
        password: String,
        onSuccess: () -> Unit,
        authCall: suspend (email: String) -> Result<User>
    ) {
        if (_uiState.value.isSigningIn) return
        if (email.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(error = "Enter your email and password.") }
            return
        }
        _uiState.update { it.copy(isSigningIn = true, error = null) }

        viewModelScope.launch {
            authCall(email.trim())
                .onSuccess {
                    tmdbKeyProvider.refresh()
                    analytics.log(AnalyticsEvent.LoginSucceeded)
                    _uiState.update { it.copy(isSigningIn = false) }
                    onSuccess()
                }
                .onFailure { error ->
                    analytics.log(AnalyticsEvent.LoginFailed)
                    _uiState.update {
                        it.copy(
                            isSigningIn = false,
                            error = error.message ?: "Sign-in failed. Try again."
                        )
                    }
                }
        }
    }
}
