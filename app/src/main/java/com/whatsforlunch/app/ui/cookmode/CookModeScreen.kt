package com.whatsforlunch.app.ui.cookmode

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.whatsforlunch.app.R
import com.whatsforlunch.app.domain.cookmode.StepTimerPhase
import com.whatsforlunch.app.domain.cookmode.StepTimerState
import com.whatsforlunch.app.domain.model.RecipeIngredient
import com.whatsforlunch.app.ui.components.unitLabel
import com.whatsforlunch.app.ui.recipedetail.ingredientLine
import com.whatsforlunch.app.ui.theme.WarmRadius
import com.whatsforlunch.app.ui.theme.WarmSpacing
import com.whatsforlunch.app.ui.theme.warmShadow

// Identificadores estables para las pruebas instrumentadas (E2E) del Modo Cocina.
object CookModeTestTags {
    const val SCREEN = "cook_mode_screen"
    const val PROGRESS = "cook_mode_progress"
    const val PROGRESS_BAR = "cook_mode_progress_bar"
    const val STEP_LABEL = "cook_mode_step_label"
    const val STEP_TEXT = "cook_mode_step_text"
    const val TIMER = "cook_mode_timer"
    const val TIMER_CLOCK = "cook_mode_timer_clock"
    const val TIMER_META = "cook_mode_timer_meta"
    const val TIMER_STATUS = "cook_mode_timer_status"
    const val TIMER_START = "cook_mode_timer_start"
    const val TIMER_PAUSE = "cook_mode_timer_pause"
    const val TIMER_RESUME = "cook_mode_timer_resume"
    const val TIMER_RESET = "cook_mode_timer_reset"
    const val TIMER_ADD_MINUTE = "cook_mode_timer_add_minute"
    const val TIMER_STOP = "cook_mode_timer_stop"
    const val INGREDIENTS = "cook_mode_ingredients"
    const val INGREDIENTS_TOGGLE = "cook_mode_ingredients_toggle"
    const val INGREDIENTS_EMPTY = "cook_mode_ingredients_empty"
    const val INGREDIENT_ROW = "cook_mode_ingredient_row"
    const val PREVIOUS_STEP = "cook_mode_previous_step"
    const val NEXT_STEP = "cook_mode_next_step"
    const val VOICE_TOGGLE = "cook_mode_voice_toggle"
    const val VOICE_LISTEN = "cook_mode_voice_listen"
    const val VOICE_CUE = "cook_mode_voice_cue"
    const val VOICE_NOTICE = "cook_mode_voice_notice"
    const val NOT_FOUND = "cook_mode_not_found"
    const val NOT_FOUND_BACK = "cook_mode_not_found_back"
}

private val CardShape = RoundedCornerShape(WarmRadius.md)

// Pantalla «Modo Cocina» (RF-CPP1 a RF-CPP4, RD22): guía paso a paso con alto
// contraste y tipografía grande (cuerpo ≥ 18 px, legible a 1,5 m), barra de avance
// «Paso X de Y», tarjeta del Paso activo, widget del temporizador del Paso
// (RF-GTP1, RD23) y el drawer colapsable «Ingredientes para este paso» de solo
// lectura (D5). El dock inferior navega con «Paso Anterior»/«Siguiente» y, en el
// último Paso, «Terminar» vuelve al Detalle (D2). La pantalla se mantiene
// encendida mientras el Modo Cocina está visible (D9). Sin reglas de negocio (D9):
// las filas del drawer las calcula `CookModeRules` y el temporizador `StepTimerRules`.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CookModeScreen(
    uiState: CookModeUiState,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onFinish: () -> Unit,
    onTimerStart: () -> Unit,
    onTimerPause: () -> Unit,
    onTimerResume: () -> Unit,
    onTimerReset: () -> Unit,
    onTimerAddMinute: () -> Unit,
    onTimerStop: () -> Unit,
    onVoiceToggle: () -> Unit,
    onSpeakStep: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // D9: con el Modo Cocina visible la pantalla no se apaga (la cuenta debe ser
    // legible a distancia); al salir se restaura el comportamiento del sistema.
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
    Scaffold(
        modifier = modifier.testTag(CookModeTestTags.SCREEN),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.cook_mode_title)) },
                navigationIcon = {
                    TextButton(
                        onClick = onBack,
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) {
                        Text(text = stringResource(R.string.action_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
        bottomBar = {
            if (uiState is CookModeUiState.Cooking) {
                CookingDock(
                    isFirstStep = uiState.stepIndex == 0,
                    isLastStep = uiState.stepIndex == uiState.totalSteps - 1,
                    onNext = onNext,
                    onPrevious = onPrevious,
                    onFinish = onFinish,
                )
            }
        },
    ) { innerPadding ->
        when (uiState) {
            CookModeUiState.Loading -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            CookModeUiState.NotFound -> NotFoundContent(
                onBack = onBack,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )

            is CookModeUiState.Cooking -> CookingContent(
                state = uiState,
                onTimerStart = onTimerStart,
                onTimerPause = onTimerPause,
                onTimerResume = onTimerResume,
                onTimerReset = onTimerReset,
                onTimerAddMinute = onTimerAddMinute,
                onTimerStop = onTimerStop,
                onVoiceToggle = onVoiceToggle,
                onSpeakStep = onSpeakStep,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        }
    }
}

// Estado «Receta no encontrada» con acción de vuelta (mismo patrón que el Detalle;
// también cubre la navegación directa a una Receta sin Pasos, D3).
@Composable
private fun NotFoundContent(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .padding(horizontal = WarmSpacing.lg)
            .testTag(CookModeTestTags.NOT_FOUND),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.recipe_detail_not_found),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Button(
            onClick = onBack,
            modifier = Modifier
                .padding(top = WarmSpacing.md)
                .heightIn(min = 52.dp)
                .testTag(CookModeTestTags.NOT_FOUND_BACK),
        ) {
            Text(text = stringResource(R.string.action_back))
        }
    }
}

@Composable
private fun CookingContent(
    state: CookModeUiState.Cooking,
    onTimerStart: () -> Unit,
    onTimerPause: () -> Unit,
    onTimerResume: () -> Unit,
    onTimerReset: () -> Unit,
    onTimerAddMinute: () -> Unit,
    onTimerStop: () -> Unit,
    onVoiceToggle: () -> Unit,
    onSpeakStep: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val step = state.recipe.steps[state.stepIndex]
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = WarmSpacing.md),
        verticalArrangement = Arrangement.spacedBy(WarmSpacing.md),
    ) {
        Text(
            text = state.recipe.title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        StepProgress(stepIndex = state.stepIndex, totalSteps = state.totalSteps)
        VoiceControls(
            voice = state.voice,
            onToggle = onVoiceToggle,
            onListen = onSpeakStep,
        )
        StepCard(
            stepNumber = state.stepIndex + 1,
            text = step.text,
        )
        state.timer?.let { timer ->
            StepTimerCard(
                timer = timer,
                onStart = onTimerStart,
                onPause = onTimerPause,
                onResume = onTimerResume,
                onReset = onTimerReset,
                onAddMinute = onTimerAddMinute,
                onStop = onTimerStop,
            )
        }
        StepIngredientsDrawer(rows = state.ingredientRows)
        Spacer(modifier = Modifier.height(WarmSpacing.md))
    }
}

// RF-CPP1: barra de avance con progreso visual y el texto «Paso X de Y».
@Composable
private fun StepProgress(stepIndex: Int, totalSteps: Int, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(WarmSpacing.xs),
    ) {
        LinearProgressIndicator(
            progress = { (stepIndex + 1f) / totalSteps },
            modifier = Modifier
                .fillMaxWidth()
                .testTag(CookModeTestTags.PROGRESS_BAR),
        )
        Text(
            text = stringResource(R.string.cook_mode_step_progress, stepIndex + 1, totalSteps),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.testTag(CookModeTestTags.PROGRESS),
        )
    }
}

// RD24 (D1/D4): controles de voz del Modo Cocina. «Voz» es un interruptor manual
// apagado por defecto; «Escuchar» locuta el Paso activo aunque la voz esté apagada.
// Debajo se muestra el aviso de degradación y, con la voz activa, la barra de
// comandos disponibles.
@Composable
private fun VoiceControls(
    voice: VoiceUiState,
    onToggle: () -> Unit,
    onListen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(WarmSpacing.sm),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(WarmSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilterChip(
                selected = voice.enabled,
                onClick = onToggle,
                label = {
                    Text(
                        text = stringResource(
                            if (voice.enabled) {
                                R.string.cook_mode_voice_toggle_active
                            } else {
                                R.string.cook_mode_voice_toggle
                            },
                        ),
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Mic,
                        contentDescription = null,
                    )
                },
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag(CookModeTestTags.VOICE_TOGGLE),
            )
            OutlinedButton(
                onClick = onListen,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag(CookModeTestTags.VOICE_LISTEN),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.VolumeUp,
                    contentDescription = null,
                )
                Spacer(modifier = Modifier.width(WarmSpacing.xs))
                Text(text = stringResource(R.string.cook_mode_voice_listen))
            }
        }
        voice.notice?.let { notice ->
            Surface(
                shape = CardShape,
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(CookModeTestTags.VOICE_NOTICE)
                    .semantics { liveRegion = LiveRegionMode.Polite },
            ) {
                Text(
                    text = voiceNoticeText(notice),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(WarmSpacing.md),
                )
            }
        }
        if (voice.enabled) {
            Surface(
                shape = CardShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(CookModeTestTags.VOICE_CUE),
            ) {
                Column(
                    modifier = Modifier.padding(WarmSpacing.md),
                    verticalArrangement = Arrangement.spacedBy(WarmSpacing.xs),
                ) {
                    Text(
                        text = stringResource(R.string.cook_mode_voice_commands_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    Text(
                        text = stringResource(R.string.cook_mode_voice_commands_hint),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }
        }
    }
}

@Composable
private fun voiceNoticeText(notice: VoiceNotice): String = stringResource(
    when (notice) {
        VoiceNotice.PERMISSION_DENIED -> R.string.cook_mode_voice_permission_denied
        VoiceNotice.VOICE_UNAVAILABLE -> R.string.cook_mode_voice_unavailable
        VoiceNotice.COMMANDS_UNAVAILABLE -> R.string.cook_mode_voice_commands_unavailable
        VoiceNotice.LANGUAGE_NOT_DOWNLOADED -> R.string.cook_mode_voice_language_not_downloaded
    },
)

// RF-CPP1: tarjeta del Paso activo con rótulo «PASO X» y su texto en tipografía
// grande (≥ 18 px). El temporizador del Paso ya no es un hint pasivo (D5): vive en
// su propio widget `StepTimerCard` (RF-GTP1).
@Composable
private fun StepCard(
    stepNumber: Int,
    text: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = CardShape,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = modifier
            .fillMaxWidth()
            .warmShadow(shape = CardShape),
    ) {
        Column(
            modifier = Modifier.padding(WarmSpacing.md),
            verticalArrangement = Arrangement.spacedBy(WarmSpacing.sm),
        ) {
            Text(
                text = stringResource(R.string.cook_mode_step_label, stepNumber),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag(CookModeTestTags.STEP_LABEL),
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.testTag(CookModeTestTags.STEP_TEXT),
            )
        }
    }
}

// RF-GTP1 a RF-GTP4 (RD23): widget del temporizador del Paso activo con anillo de
// progreso (restante / meta vigente), reloj digital mm:ss, línea «Meta: N min»,
// píldora de estado y sus controles por fase (D2). La composición sigue al
// prototipo `chefencasa_modo_cocina_paso_a_paso` (P16) y mandan los tokens (P17):
// los temporizadores usan el tertiary ámbar (DESIGN.md) y targets ≥ 48 dp. Sin
// reglas de negocio (P25): las transiciones viven en `StepTimerRules`.
@Composable
private fun StepTimerCard(
    timer: StepTimerState,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onReset: () -> Unit,
    onAddMinute: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = CardShape,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = modifier
            .fillMaxWidth()
            .warmShadow(shape = CardShape)
            .testTag(CookModeTestTags.TIMER),
    ) {
        Column(
            modifier = Modifier.padding(WarmSpacing.md),
            verticalArrangement = Arrangement.spacedBy(WarmSpacing.sm),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.cook_mode_timer_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.tertiary,
                )
                TimerStatusPill(phase = timer.phase)
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = WarmSpacing.sm),
                contentAlignment = Alignment.Center,
            ) {
                val progress = if (timer.totalSeconds == 0) {
                    0f
                } else {
                    timer.remainingSeconds.toFloat() / timer.totalSeconds
                }
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.size(112.dp),
                    color = MaterialTheme.colorScheme.tertiary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    strokeWidth = 8.dp,
                )
                Text(
                    text = timerClock(timer.remainingSeconds),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.testTag(CookModeTestTags.TIMER_CLOCK),
                )
            }
            Text(
                text = stringResource(R.string.cook_mode_timer_meta, timer.totalSeconds / SECONDS_PER_MINUTE),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.testTag(CookModeTestTags.TIMER_META),
            )
            TimerControls(
                phase = timer.phase,
                onStart = onStart,
                onPause = onPause,
                onResume = onResume,
                onReset = onReset,
                onAddMinute = onAddMinute,
                onStop = onStop,
            )
        }
    }
}

// Píldora de estado del temporizador (como el prototipo: texto en cápsula).
@Composable
private fun TimerStatusPill(phase: StepTimerPhase, modifier: Modifier = Modifier) {
    val label = when (phase) {
        StepTimerPhase.READY -> R.string.cook_mode_timer_status_ready
        StepTimerPhase.RUNNING -> R.string.cook_mode_timer_status_running
        StepTimerPhase.PAUSED -> R.string.cook_mode_timer_status_paused
        StepTimerPhase.FINISHED -> R.string.cook_mode_timer_status_finished
    }
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        modifier = modifier,
    ) {
        Text(
            text = stringResource(label),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier
                .padding(horizontal = WarmSpacing.sm, vertical = WarmSpacing.xs)
                .testTag(CookModeTestTags.TIMER_STATUS),
        )
    }
}

// Relleno horizontal reducido (los targets siguen en ≥ 48 dp): con tres botones
// por fila en pantallas de ~426 dp, «Reanudar» (la etiqueta más larga) cabe en
// una sola línea y ningún control parte su texto.
private val TimerButtonPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)

// Controles por fase (D2): «Iniciar»/«Pausar»/«Reanudar» como primario según el
// estado, «Reiniciar» y «+1 min» siempre visibles (targets ≥ 48 dp) y, en
// «Tiempo cumplido», «+1 min» (primario) y «Detener» (RF-GTP3/RF-GTP4).
@Composable
private fun TimerControls(
    phase: StepTimerPhase,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onReset: () -> Unit,
    onAddMinute: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(WarmSpacing.sm),
    ) {
        when (phase) {
            StepTimerPhase.READY -> Button(
                onClick = onStart,
                contentPadding = TimerButtonPadding,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .weight(1f)
                    .testTag(CookModeTestTags.TIMER_START),
            ) {
                Text(text = stringResource(R.string.cook_mode_timer_start))
            }

            StepTimerPhase.RUNNING -> Button(
                onClick = onPause,
                contentPadding = TimerButtonPadding,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .weight(1f)
                    .testTag(CookModeTestTags.TIMER_PAUSE),
            ) {
                Text(text = stringResource(R.string.cook_mode_timer_pause))
            }

            StepTimerPhase.PAUSED -> Button(
                onClick = onResume,
                contentPadding = TimerButtonPadding,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .weight(1f)
                    .testTag(CookModeTestTags.TIMER_RESUME),
            ) {
                Text(text = stringResource(R.string.cook_mode_timer_resume))
            }

            StepTimerPhase.FINISHED -> Button(
                onClick = onAddMinute,
                contentPadding = TimerButtonPadding,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .weight(1f)
                    .testTag(CookModeTestTags.TIMER_ADD_MINUTE),
            ) {
                Text(text = stringResource(R.string.cook_mode_timer_add_minute))
            }
        }
        if (phase == StepTimerPhase.FINISHED) {
            OutlinedButton(
                onClick = onStop,
                contentPadding = TimerButtonPadding,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .weight(1f)
                    .testTag(CookModeTestTags.TIMER_STOP),
            ) {
                Text(text = stringResource(R.string.cook_mode_timer_stop))
            }
        } else {
            OutlinedButton(
                onClick = onReset,
                contentPadding = TimerButtonPadding,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .weight(1f)
                    .testTag(CookModeTestTags.TIMER_RESET),
            ) {
                Text(text = stringResource(R.string.cook_mode_timer_reset))
            }
            OutlinedButton(
                onClick = onAddMinute,
                contentPadding = TimerButtonPadding,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .weight(1f)
                    .testTag(CookModeTestTags.TIMER_ADD_MINUTE),
            ) {
                Text(text = stringResource(R.string.cook_mode_timer_add_minute))
            }
        }
    }
}

// Reloj digital del temporizador en mm:ss (mm sin tope: «120:00» con meta ≥ 60 min).
private fun timerClock(totalSeconds: Int): String {
    val minutes = totalSeconds / SECONDS_PER_MINUTE
    val seconds = totalSeconds % SECONDS_PER_MINUTE
    return "%02d:%02d".format(minutes, seconds)
}

private const val SECONDS_PER_MINUTE = 60

// RF-CPP2: drawer colapsable «Ingredientes para este paso» con los Ingredientes
// vinculados del Paso activo (RD21) y su cantidad escalada (RD03), en formato del
// Detalle y de solo lectura (D5). Sin vínculos muestra «Sin ingredientes» (D1/D4),
// nunca todos los Ingredientes de la Receta.
@Composable
private fun StepIngredientsDrawer(
    rows: List<RecipeIngredient>,
    modifier: Modifier = Modifier,
) {
    val locale = LocalConfiguration.current.locales[0]
    var expanded by rememberSaveable { mutableStateOf(true) }

    Surface(
        shape = CardShape,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = modifier
            .fillMaxWidth()
            .warmShadow(shape = CardShape)
            .testTag(CookModeTestTags.INGREDIENTS),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .heightIn(min = 48.dp)
                    .padding(WarmSpacing.md)
                    .testTag(CookModeTestTags.INGREDIENTS_TOGGLE),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.cook_mode_ingredients_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            if (expanded) {
                if (rows.isEmpty()) {
                    Text(
                        text = stringResource(R.string.cook_mode_ingredients_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .padding(WarmSpacing.md)
                            .testTag(CookModeTestTags.INGREDIENTS_EMPTY),
                    )
                } else {
                    Column(
                        modifier = Modifier.padding(
                            start = WarmSpacing.md,
                            end = WarmSpacing.md,
                            bottom = WarmSpacing.md,
                        ),
                        verticalArrangement = Arrangement.spacedBy(WarmSpacing.sm),
                    ) {
                        rows.forEach { ingredient ->
                            Text(
                                text = ingredientLine(
                                    ingredient,
                                    locale,
                                    unit = unitLabel(ingredient.unit),
                                ),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.testTag(CookModeTestTags.INGREDIENT_ROW),
                            )
                        }
                    }
                }
            }
        }
    }
}

// RF-CPP3: dock inferior con «Paso Anterior» (deshabilitado en el Paso 1) y el
// botón primario «Siguiente», que pasa a «Terminar» en el último Paso (D2) y
// vuelve al Detalle. Targets accesibles: 48 dp / 52 dp (DESIGN.md).
@Composable
private fun CookingDock(
    isFirstStep: Boolean,
    isLastStep: Boolean,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        shadowElevation = 8.dp,
        modifier = modifier,
    ) {
        Box(modifier = Modifier.navigationBarsPadding()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = WarmSpacing.md, vertical = WarmSpacing.sm),
                horizontalArrangement = Arrangement.spacedBy(WarmSpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    onClick = onPrevious,
                    enabled = !isFirstStep,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .weight(1f)
                        .testTag(CookModeTestTags.PREVIOUS_STEP),
                ) {
                    Text(text = stringResource(R.string.cook_mode_previous_step))
                }
                Button(
                    onClick = { if (isLastStep) onFinish() else onNext() },
                    modifier = Modifier
                        .heightIn(min = 52.dp)
                        .weight(1f)
                        .testTag(CookModeTestTags.NEXT_STEP),
                ) {
                    Text(
                        text = stringResource(
                            if (isLastStep) R.string.cook_mode_finish else R.string.cook_mode_next_step,
                        ),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }
    }
}
