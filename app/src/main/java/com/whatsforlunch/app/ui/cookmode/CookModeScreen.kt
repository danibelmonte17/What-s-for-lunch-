package com.whatsforlunch.app.ui.cookmode

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.whatsforlunch.app.R
import com.whatsforlunch.app.domain.model.RecipeIngredient
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
    const val STEP_TIMER = "cook_mode_step_timer"
    const val INGREDIENTS = "cook_mode_ingredients"
    const val INGREDIENTS_TOGGLE = "cook_mode_ingredients_toggle"
    const val INGREDIENTS_EMPTY = "cook_mode_ingredients_empty"
    const val INGREDIENT_ROW = "cook_mode_ingredient_row"
    const val PREVIOUS_STEP = "cook_mode_previous_step"
    const val NEXT_STEP = "cook_mode_next_step"
    const val NOT_FOUND = "cook_mode_not_found"
    const val NOT_FOUND_BACK = "cook_mode_not_found_back"
}

private val CardShape = RoundedCornerShape(WarmRadius.md)

// Pantalla «Modo Cocina» (RF-CPP1 a RF-CPP4, RD22): guía paso a paso con alto
// contraste y tipografía grande (cuerpo ≥ 18 px, legible a 1,5 m), barra de avance
// «Paso X de Y», tarjeta del Paso activo con el hint pasivo «⏱ N min» (D6) y el
// drawer colapsable «Ingredientes para este paso» de solo lectura (D5). El dock
// inferior navega con «Paso Anterior»/«Siguiente» y, en el último Paso, «Terminar»
// vuelve al Detalle (D2). Sin reglas de negocio (D9): las filas del drawer las
// calcula `CookModeRules`.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CookModeScreen(
    uiState: CookModeUiState,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
        StepCard(
            stepNumber = state.stepIndex + 1,
            text = step.text,
            timerMinutes = step.timerMinutes,
        )
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

// RF-CPP1 y RF-CPP6: tarjeta del Paso activo con rótulo «PASO X», texto en
// tipografía grande (≥ 18 px) y el hint pasivo «⏱ N min» si el Paso tiene
// temporizador (D6): sin cuenta atrás, alarma ni controles de tiempo.
@Composable
private fun StepCard(
    stepNumber: Int,
    text: String,
    timerMinutes: Int?,
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
            timerMinutes?.let { minutes ->
                Text(
                    text = stringResource(R.string.cook_mode_step_timer_hint, minutes),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.testTag(CookModeTestTags.STEP_TIMER),
                )
            }
        }
    }
}

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
                                text = ingredientLine(ingredient, locale),
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
