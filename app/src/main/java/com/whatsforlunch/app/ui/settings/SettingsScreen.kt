package com.whatsforlunch.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.whatsforlunch.app.R
import com.whatsforlunch.app.domain.model.AppLanguage
import com.whatsforlunch.app.ui.theme.WarmRadius
import com.whatsforlunch.app.ui.theme.WarmSpacing

// Identificadores estables para las pruebas instrumentadas de Ajustes.
object SettingsTestTags {
    const val SCREEN = "settings_screen"
    const val STATUS = "settings_api_key_status"
    const val BUILD_SOURCE = "settings_api_key_build_source"
    const val KEY_FIELD = "settings_api_key_field"
    const val SAVE = "settings_api_key_save"
    const val DELETE = "settings_api_key_delete"
    const val DELETE_DIALOG = "settings_api_key_delete_dialog"
    const val DELETE_CONFIRM = "settings_api_key_delete_confirm"
    const val DELETE_CANCEL = "settings_api_key_delete_cancel"
    const val LANGUAGE_SECTION = "settings_language_section"
    const val LANGUAGE_OPTION_ES = "settings_language_option_es"
    const val LANGUAGE_OPTION_EN = "settings_language_option_en"
    const val PRIVACY_TITLE = "settings_privacy_title"
    const val PRIVACY_NOTICE = "settings_privacy_notice"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onBack: () -> Unit,
    onKeyChange: (String) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onSavedConsumed: () -> Unit,
    onLanguageSelected: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val savedMessage = stringResource(R.string.settings_api_key_saved)

    LaunchedEffect(uiState.saved) {
        if (uiState.saved) {
            snackbarHostState.showSnackbar(savedMessage)
            onSavedConsumed()
        }
    }

    Scaffold(
        modifier = modifier.testTag(SettingsTestTags.SCREEN),
        containerColor = MaterialTheme.colorScheme.surface,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    TextButton(onClick = onBack, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(stringResource(R.string.action_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = WarmSpacing.md, vertical = WarmSpacing.md),
            verticalArrangement = Arrangement.spacedBy(WarmSpacing.md),
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(WarmRadius.lg),
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
            ) {
                Column(
                    modifier = Modifier.padding(WarmSpacing.md),
                    verticalArrangement = Arrangement.spacedBy(WarmSpacing.sm),
                ) {
                    Text(
                        text = stringResource(
                            if (uiState.isConfigured) {
                                R.string.settings_api_key_state_configured
                            } else {
                                R.string.settings_api_key_state_missing
                            },
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        color = if (uiState.isConfigured) {
                            MaterialTheme.colorScheme.secondary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.testTag(SettingsTestTags.STATUS),
                    )
                    if (uiState.buildKeyActive) {
                        Text(
                            text = stringResource(R.string.settings_api_key_build_source),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.testTag(SettingsTestTags.BUILD_SOURCE),
                        )
                    }
                    OutlinedTextField(
                        value = uiState.fieldValue,
                        onValueChange = onKeyChange,
                        label = { Text(stringResource(R.string.settings_api_key_label)) },
                        placeholder = { Text(stringResource(R.string.settings_api_key_hint)) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(SettingsTestTags.KEY_FIELD),
                    )
                    Button(
                        onClick = onSave,
                        enabled = uiState.canSave,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .testTag(SettingsTestTags.SAVE),
                    ) {
                        Text(stringResource(R.string.settings_api_key_save))
                    }
                    if (uiState.canDelete) {
                        TextButton(
                            onClick = { showDeleteDialog = true },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.error,
                            ),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag(SettingsTestTags.DELETE),
                        ) {
                            Text(stringResource(R.string.settings_api_key_delete))
                        }
                    }
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(WarmRadius.lg),
                color = MaterialTheme.colorScheme.surfaceContainer,
            ) {
                Column(
                    modifier = Modifier
                        .padding(WarmSpacing.md)
                        .testTag(SettingsTestTags.LANGUAGE_SECTION),
                    verticalArrangement = Arrangement.spacedBy(WarmSpacing.sm),
                ) {
                    Text(
                        text = stringResource(R.string.settings_language_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    LanguageOptionRow(
                        label = stringResource(R.string.settings_language_spanish),
                        selected = uiState.language == AppLanguage.SPANISH,
                        onClick = { onLanguageSelected(AppLanguage.SPANISH) },
                        tag = SettingsTestTags.LANGUAGE_OPTION_ES,
                    )
                    LanguageOptionRow(
                        label = stringResource(R.string.settings_language_english),
                        selected = uiState.language == AppLanguage.ENGLISH,
                        onClick = { onLanguageSelected(AppLanguage.ENGLISH) },
                        tag = SettingsTestTags.LANGUAGE_OPTION_EN,
                    )
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(WarmRadius.lg),
                color = MaterialTheme.colorScheme.surfaceContainer,
            ) {
                Column(
                    modifier = Modifier.padding(WarmSpacing.md),
                    verticalArrangement = Arrangement.spacedBy(WarmSpacing.sm),
                ) {
                    Text(
                        text = stringResource(R.string.settings_privacy_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.testTag(SettingsTestTags.PRIVACY_TITLE),
                    )
                    Text(
                        text = stringResource(R.string.settings_privacy_notice),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag(SettingsTestTags.PRIVACY_NOTICE),
                    )
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            modifier = Modifier.testTag(SettingsTestTags.DELETE_DIALOG),
            title = { Text(stringResource(R.string.settings_api_key_delete_title)) },
            text = { Text(stringResource(R.string.settings_api_key_delete_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                    modifier = Modifier.testTag(SettingsTestTags.DELETE_CONFIRM),
                ) {
                    Text(stringResource(R.string.settings_api_key_delete_confirm))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false },
                    modifier = Modifier.testTag(SettingsTestTags.DELETE_CANCEL),
                ) {
                    Text(stringResource(R.string.settings_api_key_delete_cancel))
                }
            },
        )
    }
}

// Opción del selector de idioma (RF-IDI1): fila seleccionable de al menos 48 dp con
// radio de Material; las opciones son endónimos (D3) y el cambio es inmediato.
@Composable
private fun LanguageOptionRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    tag: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .testTag(tag),
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = WarmSpacing.sm),
        )
    }
}
