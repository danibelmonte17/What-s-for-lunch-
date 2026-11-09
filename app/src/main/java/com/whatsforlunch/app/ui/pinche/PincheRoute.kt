package com.whatsforlunch.app.ui.pinche

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel

// Une el ViewModel (MVVM + StateFlow) con la pantalla sin estado. El idioma de la
// app viaja al prompt de la IA (D12) y «Abrir Ajustes» navega al destino Settings;
// «Abrir Mi perfil» salta a la pestaña del perfil (`crear-mi-perfil`, D8).
@Composable
fun PincheRoute(
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    // `crear-mi-perfil` (D8): acción del mensaje que pide el perfil pendiente.
    onOpenProfile: () -> Unit = {},
    scrollToTopTick: Int = 0,
    viewModel: PincheViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val language = LocalConfiguration.current.locales[0].language

    PincheScreen(
        uiState = uiState,
        onOpenSettings = onOpenSettings,
        onInputChange = viewModel::onInputChange,
        onSend = { viewModel.send(language) },
        onQuickSuggestion = { text -> viewModel.sendQuickSuggestion(text, language) },
        onRetry = { viewModel.retry(language) },
        onSaveProposal = viewModel::saveProposal,
        onClearNotice = viewModel::clearNotice,
        onOpenProfile = onOpenProfile,
        scrollToTopTick = scrollToTopTick,
        modifier = modifier,
    )
}
