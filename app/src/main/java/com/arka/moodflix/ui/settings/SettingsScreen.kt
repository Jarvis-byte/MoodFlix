package com.arka.moodflix.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.arka.moodflix.R
import com.arka.moodflix.core.AppLanguage
import com.arka.moodflix.domain.model.AiProviderType
import com.arka.moodflix.domain.model.ConnectedProvider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenUrl: (String) -> Unit,
    onLoggedOut: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val providers by viewModel.providers.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val darkThemeEnabled by viewModel.darkThemeEnabled.collectAsStateWithLifecycle()
    val snackbarHost = remember { SnackbarHostState() }
    val context = LocalContext.current
    var showSignOutConfirm by remember { mutableStateOf(false) }

    if (showSignOutConfirm) {
        AlertDialog(
            onDismissRequest = { showSignOutConfirm = false },
            title = { Text(stringResource(R.string.settings_signout_title)) },
            text = { Text(stringResource(R.string.settings_signout_body)) },
            confirmButton = {
                TextButton(onClick = {
                    showSignOutConfirm = false
                    viewModel.signOut(context, onSignedOut = onLoggedOut)
                }) {
                    Text(stringResource(R.string.action_log_out), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutConfirm = false }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHost.showSnackbar(it)
            viewModel.onEvent(SettingsEvent.DismissMessage)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            item {
                AccountCard(
                    displayName = currentUser?.displayName,
                    email = currentUser?.email,
                    photoUrl = currentUser?.photoUrl,
                    onLogOut = { showSignOutConfirm = true }
                )
            }

            item {
                AppearanceCard(
                    darkThemeEnabled = darkThemeEnabled,
                    onDarkThemeChanged = viewModel::setDarkThemeEnabled
                )
            }

            item {
                LanguageCard(onLanguageChanged = viewModel::setHindiEnabled)
            }

            item {
                Text(
                    text = stringResource(R.string.settings_keys_local_note),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.settings_fallback_note),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            items(providers, key = { it.type.name }) { provider ->
                ProviderCard(
                    provider = provider,
                    isEditing = state.editingProvider == provider.type,
                    draftKey = state.draftKey,
                    isFirst = provider.order == 0,
                    onStartEditing = { viewModel.onEvent(SettingsEvent.StartEditing(provider.type)) },
                    onDraftChanged = { viewModel.onEvent(SettingsEvent.DraftChanged(it)) },
                    onSave = { viewModel.onEvent(SettingsEvent.SaveKey) },
                    onCancel = { viewModel.onEvent(SettingsEvent.CancelEditing) },
                    onRemove = { viewModel.onEvent(SettingsEvent.RemoveKey(provider.type)) },
                    onMoveUp = { viewModel.onEvent(SettingsEvent.MoveUp(provider.type)) },
                    onGetKey = { onOpenUrl(provider.type.keyConsoleUrl) }
                )
            }

            item {
                TextButton(onClick = { onOpenUrl(PRIVACY_POLICY_URL) }) {
                    Text(stringResource(R.string.settings_privacy_policy))
                    Spacer(Modifier.size(4.dp))
                    Icon(
                        Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

private const val PRIVACY_POLICY_URL = "https://claude.ai/code/artifact/3f79fda7-e6c1-4cc0-a032-b986ec41cff2"

@Composable
private fun ProviderCard(
    provider: ConnectedProvider,
    isEditing: Boolean,
    draftKey: String,
    isFirst: Boolean,
    onStartEditing: () -> Unit,
    onDraftChanged: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onRemove: () -> Unit,
    onMoveUp: () -> Unit,
    onGetKey: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = provider.type.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (provider.hasKey) {
                            if (isFirst) {
                                stringResource(R.string.provider_connected_first)
                            } else {
                                stringResource(R.string.provider_connected_fallback, provider.order + 1)
                            }
                        } else {
                            stringResource(R.string.provider_not_connected)
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (provider.hasKey) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }

                if (provider.hasKey) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    if (!isFirst) {
                        IconButton(onClick = onMoveUp) {
                            Icon(
                                Icons.Default.KeyboardArrowUp,
                                contentDescription = stringResource(R.string.cd_move_up_fallback)
                            )
                        }
                    }
                }
            }

            if (isEditing) {
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = draftKey,
                    onValueChange = onDraftChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.settings_paste_api_key)) },
                    placeholder = { Text(provider.type.keyPrefixHint) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    )
                )

                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onSave,
                        enabled = draftKey.isNotBlank(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(stringResource(R.string.action_save))
                    }
                    TextButton(onClick = onCancel) { Text(stringResource(R.string.action_cancel)) }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onGetKey) {
                        Text(stringResource(R.string.action_get_a_key))
                        Spacer(Modifier.size(4.dp))
                        Icon(
                            Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            } else {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onStartEditing) {
                        Text(
                            stringResource(
                                if (provider.hasKey) R.string.action_replace_key else R.string.action_connect
                            )
                        )
                    }
                    if (provider.hasKey) {
                        TextButton(onClick = onRemove) {
                            Text(stringResource(R.string.action_remove), color = MaterialTheme.colorScheme.error)
                        }
                    } else {
                        TextButton(onClick = onGetKey) { Text(stringResource(R.string.action_get_a_free_key)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppearanceCard(
    darkThemeEnabled: Boolean,
    onDarkThemeChanged: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.settings_dark_theme_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(
                        if (darkThemeEnabled) {
                            R.string.settings_dark_theme_on
                        } else {
                            R.string.settings_dark_theme_off
                        }
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = darkThemeEnabled,
                onCheckedChange = onDarkThemeChanged,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    }
}

@Composable
private fun LanguageCard(onLanguageChanged: (Boolean) -> Unit) {
    var isHindi by remember { mutableStateOf(AppLanguage.isHindi) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.settings_language_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(
                        if (isHindi) R.string.settings_language_hindi else R.string.settings_language_english
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = isHindi,
                onCheckedChange = {
                    isHindi = it
                    onLanguageChanged(it)
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    }
}

@Composable
private fun AccountCard(
    displayName: String?,
    email: String?,
    photoUrl: String?,
    onLogOut: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (photoUrl != null) {
                AsyncImage(
                    model = photoUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                )
                Spacer(Modifier.size(12.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayName ?: stringResource(R.string.account_signed_in),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (email != null) {
                    Text(
                        text = email,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            TextButton(onClick = onLogOut) {
                Icon(
                    Icons.AutoMirrored.Filled.Logout,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(Modifier.size(4.dp))
                Text(stringResource(R.string.action_log_out), color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
