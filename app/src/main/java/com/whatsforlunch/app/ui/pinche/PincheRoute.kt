package com.whatsforlunch.app.ui.pinche

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel

// `mantener-y-limpiar-conversacion-del-pinche` (D8): en la app real el contexto
// es la Activity y el ViewModel se ancla a su ViewModelStore para sobrevivir a
// los cambios de pestaña —`openTab` destruye la entrada `pinche` del back stack
// y con ella se perdía la conversación—; al cerrar la app el store se vacía (D1:
// en memoria, sin persistir entre sesiones). Sin Activity (composiciones de test
// que proveen un LocalContext de app) se recurre al owner por defecto, como antes
// del cambio.
@Composable
private fun pincheViewModel(): PincheViewModel {
    val activity = LocalActivity.current as? ComponentActivity
    return if (activity != null) {
        koinViewModel(viewModelStoreOwner = activity)
    } else {
        koinViewModel()
    }
}

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
    viewModel: PincheViewModel = pincheViewModel(),
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
        // `anadir-faltantes-a-la-lista` (RF-AFL4): cableado obligatorio. El
        // parámetro ya no tiene valor por defecto en `PincheScreen`, de modo que
        // una pérdida del cableado no compila (regresión que lo dejó en no-op).
        onAddMissingToShoppingList = viewModel::addMissingToShoppingList,
        onClearNotice = viewModel::clearNotice,
        onClearConversation = viewModel::clearConversation,
        onOpenProfile = onOpenProfile,
        scrollToTopTick = scrollToTopTick,
        modifier = modifier,
    )
}
