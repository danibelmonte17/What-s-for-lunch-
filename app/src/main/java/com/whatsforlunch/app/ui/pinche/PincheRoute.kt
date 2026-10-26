package com.whatsforlunch.app.ui.pinche

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel

// Une el ViewModel (MVVM + StateFlow) con la pantalla sin estado. El idioma de la
// app viaja al prompt de la IA (D12) y «Abrir Ajustes» navega al destino Settings.
@Composable
fun PincheRoute(
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PincheViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val language = LocalConfiguration.current.locales[0].language
    PincheScreen(
        uiState = uiState,
        onBack = onBack,
        onOpenSettings = onOpenSettings,
        onInputChange = viewModel::onInputChange,
        onSend = { viewModel.send(language) },
        onQuickSuggestion = { text -> viewModel.sendQuickSuggestion(text, language) },
        onRetry = { viewModel.retry(language) },
        onSaveProposal = viewModel::saveProposal,
        onClearNotice = viewModel::clearNotice,
        modifier = modifier,
    )
}
