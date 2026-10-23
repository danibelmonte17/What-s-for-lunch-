package com.whatsforlunch.app.ui.aisuggestions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel

// Une el ViewModel (MVVM + StateFlow) con la pantalla sin estado. El idioma de la
// app viaja al prompt de la IA (D9) y «Abrir Ajustes» navega al destino Settings.
@Composable
fun AiSuggestionsRoute(
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AiSuggestionsViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val language = LocalConfiguration.current.locales[0].language
    AiSuggestionsScreen(
        uiState = uiState,
        onBack = onBack,
        onOpenSettings = onOpenSettings,
        onToggleFood = viewModel::toggleFood,
        onRequest = { viewModel.requestSuggestions(language) },
        onRetry = viewModel::retry,
        modifier = modifier,
    )
}
