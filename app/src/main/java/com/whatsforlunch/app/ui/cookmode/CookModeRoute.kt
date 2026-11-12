package com.whatsforlunch.app.ui.cookmode

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.util.Locale
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

// Une el ViewModel (MVVM + StateFlow) con la pantalla sin estado. El id de la
// Receta y las Raciones heredadas del Detalle llegan por ruta
// (cook_mode/{recipeId}?servings={servings}, RF-CPP4) y se inyectan en el
// ViewModel vía Koin.
//
// La voz (RD24) vive aquí en la capa Android (D10): el locale normalizado es el de
// la app (D5), el permiso RECORD_AUDIO se pide solo al activar «Voz» la primera vez
// (D4), el ciclo de vida detiene/reanuda la escucha sin relocutar (D9) y el fin por
// voz (D3/D11) se traduce en `onFinish`.
@Composable
fun CookModeRoute(
    recipeId: String,
    servings: Int?,
    onBack: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CookModeViewModel = koinViewModel { parametersOf(recipeId, servings) },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val locale = voiceLocale(LocalConfiguration.current.locales[0])

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            viewModel.onVoiceEnabled(locale)
        } else {
            viewModel.onVoicePermissionDenied()
        }
    }

    val onVoiceToggle: () -> Unit = {
        val cooking = uiState as? CookModeUiState.Cooking
        if (cooking?.voice?.enabled == true) {
            viewModel.onVoiceDisabled()
        } else if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            viewModel.onVoiceEnabled(locale)
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                CookModeEvent.Finish -> onFinish()
            }
        }
    }

    // El micrófono y el TTS no viven en segundo plano (D9): al parar el host se
    // cortan y, al volver, si la voz seguía activa se reanuda la escucha. El permiso
    // se re-comprueba por si el usuario lo revocó en Ajustes mientras tanto.
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> viewModel.onHostStopped()
                Lifecycle.Event.ON_START -> {
                    val cooking = viewModel.uiState.value as? CookModeUiState.Cooking
                    if (cooking?.voice?.enabled == true) {
                        val granted = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO,
                        ) == PackageManager.PERMISSION_GRANTED
                        if (granted) {
                            viewModel.onHostStarted()
                        } else {
                            viewModel.onVoicePermissionDenied()
                        }
                    }
                }

                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    CookModeScreen(
        uiState = uiState,
        onBack = onBack,
        onNext = viewModel::next,
        onPrevious = viewModel::previous,
        onFinish = onFinish,
        onTimerStart = viewModel::onTimerStart,
        onTimerPause = viewModel::onTimerPause,
        onTimerResume = viewModel::onTimerResume,
        onTimerReset = viewModel::onTimerReset,
        onTimerAddMinute = viewModel::onTimerAddMinute,
        onTimerStop = viewModel::onTimerStop,
        onVoiceToggle = onVoiceToggle,
        onSpeakStep = { viewModel.onSpeakStep(locale) },
        modifier = modifier,
    )
}

// El idioma de voz es el de la app (D5): en inglés se usa inglés y cualquier otro
// idioma cae a español, igual que el parser de comandos.
private fun voiceLocale(configuration: Locale): Locale =
    if (configuration.language == "en") Locale.ENGLISH else Locale.forLanguageTag("es-ES")
