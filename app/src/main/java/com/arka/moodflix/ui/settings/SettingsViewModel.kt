package com.arka.moodflix.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arka.moodflix.R
import com.arka.moodflix.core.AppLanguage
import com.arka.moodflix.core.analytics.AnalyticsEvent
import com.arka.moodflix.core.analytics.AnalyticsManager
import com.arka.moodflix.data.local.UserPreferences
import com.arka.moodflix.domain.model.AiProviderType
import com.arka.moodflix.domain.model.ConnectedProvider
import com.arka.moodflix.domain.model.User
import com.arka.moodflix.domain.repository.AiKeyRepository
import com.arka.moodflix.domain.repository.AuthRepository
import com.arka.moodflix.domain.repository.WatchlistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val editingProvider: AiProviderType? = null,
    val draftKey: String = "",
    val message: String? = null
)

sealed interface SettingsEvent {
    data class StartEditing(val type: AiProviderType) : SettingsEvent
    data class DraftChanged(val value: String) : SettingsEvent
    data object SaveKey : SettingsEvent
    data object CancelEditing : SettingsEvent
    data class RemoveKey(val type: AiProviderType) : SettingsEvent
    data class MoveUp(val type: AiProviderType) : SettingsEvent
    data object DismissMessage : SettingsEvent
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val keyRepository: AiKeyRepository,
    private val prefs: UserPreferences,
    private val authRepository: AuthRepository,
    private val watchlistRepository: WatchlistRepository,
    private val analytics: AnalyticsManager,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    val currentUser: StateFlow<User?> = authRepository.authState

    val providers: StateFlow<List<ConnectedProvider>> = keyRepository.connectedProviders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val watchCountry: StateFlow<String> = prefs.watchCountry
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "IN")

    val darkThemeEnabled: StateFlow<Boolean> = prefs.darkThemeEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    fun setDarkThemeEnabled(enabled: Boolean) {
        analytics.log(AnalyticsEvent.DarkThemeToggled(enabled))
        viewModelScope.launch { prefs.setDarkThemeEnabled(enabled) }
    }

    fun setHindiEnabled(enabled: Boolean) {
        analytics.log(AnalyticsEvent.LanguageToggled(if (enabled) "hi" else "en"))
        AppLanguage.setHindi(enabled)
    }

    fun onEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.StartEditing ->
                _uiState.update { it.copy(editingProvider = event.type, draftKey = "") }

            is SettingsEvent.DraftChanged ->
                _uiState.update { it.copy(draftKey = event.value) }

            SettingsEvent.CancelEditing ->
                _uiState.update { it.copy(editingProvider = null, draftKey = "") }

            SettingsEvent.SaveKey -> {
                val type = _uiState.value.editingProvider ?: return
                val key = _uiState.value.draftKey.trim()
                if (key.isBlank()) return

                viewModelScope.launch {
                    keyRepository.saveKey(type, key)
                    analytics.log(AnalyticsEvent.AiProviderConnected(type.displayName))
                    _uiState.update {
                        it.copy(
                            editingProvider = null,
                            draftKey = "",
                            message = appContext.getString(R.string.settings_provider_connected, type.displayName)
                        )
                    }
                }
            }

            is SettingsEvent.RemoveKey -> viewModelScope.launch {
                keyRepository.removeKey(event.type)
                analytics.log(AnalyticsEvent.AiProviderRemoved(event.type.displayName))
                _uiState.update {
                    it.copy(
                        message = appContext.getString(R.string.settings_provider_removed, event.type.displayName)
                    )
                }
            }

            is SettingsEvent.MoveUp -> viewModelScope.launch {
                val current = providers.value.map { it.type }.toMutableList()
                val index = current.indexOf(event.type)
                if (index > 0) {
                    val above = current[index - 1]
                    current[index - 1] = current[index]
                    current[index] = above
                    keyRepository.setFallbackOrder(current)
                }
            }

            SettingsEvent.DismissMessage ->
                _uiState.update { it.copy(message = null) }
        }
    }

    fun setCountry(code: String) {
        viewModelScope.launch { prefs.setWatchCountry(code) }
    }

    fun signOut(context: Context, onSignedOut: () -> Unit) {
        viewModelScope.launch {
            authRepository.signOut(context)
            watchlistRepository.clearAll()
            analytics.log(AnalyticsEvent.LoggedOut)
            onSignedOut()
        }
    }
}