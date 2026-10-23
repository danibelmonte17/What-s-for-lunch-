package com.whatsforlunch.app.ui.recipedetail

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Equalizer
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.whatsforlunch.app.R
import com.whatsforlunch.app.domain.match.RecipeMatch
import com.whatsforlunch.app.domain.model.Difficulty
import com.whatsforlunch.app.domain.model.Recipe
import com.whatsforlunch.app.domain.model.RecipeIngredient
import com.whatsforlunch.app.ui.components.MatchBadge
import com.whatsforlunch.app.ui.components.MomentBadge
import com.whatsforlunch.app.ui.components.SkilletIcon
import com.whatsforlunch.app.ui.theme.WarmRadius
import com.whatsforlunch.app.ui.theme.WarmSpacing
import com.whatsforlunch.app.ui.theme.warmShadow
import java.text.NumberFormat
import java.util.Locale

// Identificadores estables para las pruebas instrumentadas (E2E) del Detalle.
object RecipeDetailTestTags {
    const val SCREEN = "recipe_detail_screen"
    const val HERO = "recipe_detail_hero"
    const val HERO_MOMENT = "recipe_detail_hero_moment"
    const val MATCH = "recipe_detail_match"
    const val METRICS = "recipe_detail_metrics"
    const val TOTAL_TIME = "recipe_detail_total_time"
    const val DIFFICULTY = "recipe_detail_difficulty"
    const val CALORIES = "recipe_detail_calories"
    const val SERVINGS = "recipe_detail_servings"
    const val INGREDIENTS = "recipe_detail_ingredients"
    const val INGREDIENT_ROW = "recipe_detail_ingredient_row"
    const val INGREDIENT_CHECK = "recipe_detail_ingredient_check"
    const val INGREDIENTS_COUNT = "recipe_detail_ingredients_count"
    const val STEPS = "recipe_detail_steps"
    const val STEP_ROW = "recipe_detail_step_row"
    const val STEP_TIMER = "recipe_detail_step_timer"
    const val START_COOKING = "recipe_detail_start_cooking"
    const val NOT_FOUND = "recipe_detail_not_found"
    const val NOT_FOUND_BACK = "recipe_detail_not_found_back"
}

private val CardShape = RoundedCornerShape(WarmRadius.md)
private val PillShape = RoundedCornerShape(50)

// D7: cantidad con el separador decimal del idioma activo y sin ceros sobrantes
// (0,5 en es; 0.5 en en; los enteros pierden el decimal).
internal fun formatQuantity(quantity: Double, locale: Locale): String =
    NumberFormat.getNumberInstance(locale).format(quantity)

// Pantalla del Detalle de Receta (ver-detalle-de-receta): hero con foto y badges,
// métricas 2×2, checklist de marcado local y pasos resumidos, con la CTA fija
// «Empezar Modo Cocina» (RF-DR3 a RF-DR7). Sin reglas de negocio (D3).
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeDetailScreen(
    uiState: RecipeDetailUiState,
    onBack: () -> Unit,
    onStartCooking: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.testTag(RecipeDetailTestTags.SCREEN),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = {},
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
            if (uiState is RecipeDetailUiState.Detail) {
                StartCookingBar(onStartCooking = onStartCooking)
            }
        },
    ) { innerPadding ->
        when (uiState) {
            RecipeDetailUiState.Loading -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            RecipeDetailUiState.NotFound -> NotFoundContent(
                onBack = onBack,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )

            is RecipeDetailUiState.Detail -> RecipeDetailContent(
                recipe = uiState.recipe,
                match = uiState.match,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        }
    }
}

// RF-DR8: estado «Receta no encontrada» con acción de vuelta en lugar de ficha vacía.
@Composable
private fun NotFoundContent(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .padding(horizontal = WarmSpacing.lg)
            .testTag(RecipeDetailTestTags.NOT_FOUND),
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
                .testTag(RecipeDetailTestTags.NOT_FOUND_BACK),
        ) {
            Text(text = stringResource(R.string.action_back))
        }
    }
}

@Composable
private fun RecipeDetailContent(
    recipe: Recipe,
    match: RecipeMatch?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(WarmSpacing.md),
    ) {
        RecipeHero(recipe = recipe, match = match)
        MetricsGrid(
            prepMinutes = recipe.prepMinutes,
            cookMinutes = recipe.cookMinutes,
            difficulty = recipe.difficulty,
            caloriesPerServing = recipe.caloriesPerServing,
            baseServings = recipe.baseServings,
        )
        IngredientChecklist(recipe = recipe)
        StepsSummary(recipe = recipe)
        Spacer(modifier = Modifier.height(WarmSpacing.md))
    }
}

// RF-DR3 y RF-DR2: foto con badge de momento y badge de coincidencia superpuestos;
// sin foto, placeholder de superficie y badges junto al título.
@Composable
private fun RecipeHero(recipe: Recipe, match: RecipeMatch?) {
    val bitmap = rememberBitmap(recipe.photoPath)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = WarmSpacing.md)
            .testTag(RecipeDetailTestTags.HERO),
    ) {
        if (bitmap != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(272.dp)
                    .warmShadow(shape = RoundedCornerShape(WarmRadius.xl)),
            ) {
                Image(
                    bitmap = bitmap,
                    contentDescription = recipe.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(WarmRadius.xl)),
                )
                MomentBadge(
                    mealMoment = recipe.mealMoment,
                    testTag = RecipeDetailTestTags.HERO_MOMENT,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(WarmSpacing.md),
                )
                match?.let {
                    MatchBadge(
                        match = it,
                        testTag = RecipeDetailTestTags.MATCH,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(WarmSpacing.md),
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(272.dp)
                    .warmShadow(shape = RoundedCornerShape(WarmRadius.xl))
                    .clip(RoundedCornerShape(WarmRadius.xl))
                    .background(MaterialTheme.colorScheme.surfaceContainer),
            )
        }
        Spacer(modifier = Modifier.height(WarmSpacing.md))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (bitmap == null) {
                // Sin foto, los badges van junto al título (caso límite de la spec).
                MomentBadge(
                    mealMoment = recipe.mealMoment,
                    testTag = RecipeDetailTestTags.HERO_MOMENT,
                )
                Spacer(modifier = Modifier.width(WarmSpacing.sm))
            }
            Text(
                text = recipe.title,
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (bitmap == null && match != null) {
                Spacer(modifier = Modifier.width(WarmSpacing.sm))
                MatchBadge(match = match, testTag = RecipeDetailTestTags.MATCH)
            }
        }
        if (recipe.story.isNotBlank()) {
            Text(
                text = recipe.story,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = WarmSpacing.xs),
            )
        }
    }
}

// RF-DR4: métricas 2×2 —Tiempo total con desglose, Dificultad, Calorías por
// porción («—» si es null) y Raciones base— sin precios (P12, ⚠ S02).
@Composable
private fun MetricsGrid(
    prepMinutes: Int,
    cookMinutes: Int,
    difficulty: Difficulty,
    caloriesPerServing: Int?,
    baseServings: Int,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = WarmSpacing.md)
            .testTag(RecipeDetailTestTags.METRICS),
        verticalArrangement = Arrangement.spacedBy(WarmSpacing.sm),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(WarmSpacing.sm)) {
            MetricCard(
                icon = Icons.Outlined.Schedule,
                iconTint = MaterialTheme.colorScheme.tertiary,
                label = stringResource(R.string.recipe_detail_metric_time),
                value = stringResource(R.string.my_recipes_card_time, prepMinutes + cookMinutes),
                sublabel = stringResource(
                    R.string.recipe_detail_metric_time_breakdown,
                    prepMinutes,
                    cookMinutes,
                ),
                testTag = RecipeDetailTestTags.TOTAL_TIME,
                modifier = Modifier.weight(1f),
            )
            MetricCard(
                icon = Icons.Outlined.Equalizer,
                iconTint = MaterialTheme.colorScheme.secondary,
                label = stringResource(R.string.recipe_detail_metric_difficulty),
                value = stringResource(difficulty.labelRes()),
                testTag = RecipeDetailTestTags.DIFFICULTY,
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(WarmSpacing.sm)) {
            MetricCard(
                icon = Icons.Outlined.LocalFireDepartment,
                iconTint = MaterialTheme.colorScheme.primary,
                label = stringResource(R.string.recipe_detail_metric_calories),
                value = caloriesPerServing?.toString() ?: "—",
                testTag = RecipeDetailTestTags.CALORIES,
                modifier = Modifier.weight(1f),
            )
            MetricCard(
                icon = Icons.Outlined.Group,
                iconTint = MaterialTheme.colorScheme.primary,
                label = stringResource(R.string.recipe_detail_metric_servings),
                value = baseServings.toString(),
                testTag = RecipeDetailTestTags.SERVINGS,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun MetricCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: androidx.compose.ui.graphics.Color,
    label: String,
    value: String,
    testTag: String,
    modifier: Modifier = Modifier,
    sublabel: String? = null,
) {
    Surface(
        shape = CardShape,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = modifier
            .warmShadow(shape = CardShape)
            .testTag(testTag),
    ) {
        Column(
            modifier = Modifier.padding(WarmSpacing.md),
            verticalArrangement = Arrangement.spacedBy(WarmSpacing.xs),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (sublabel != null) {
                Text(
                    text = sublabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

// RF-DR5 y D3: checklist con marcado local (se reinicia al salir), obligatorios
// primero y opcionales con su píldora (D6), y contador «N de M».
@Composable
private fun IngredientChecklist(recipe: Recipe) {
    val locale = LocalConfiguration.current.locales[0]
    var checkedIds by rememberSaveable(
        recipe.id,
        stateSaver = listSaver(
            save = { it.toList() },
            restore = { it.toSet() },
        ),
    ) { mutableStateOf(setOf<String>()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = WarmSpacing.md)
            .testTag(RecipeDetailTestTags.INGREDIENTS),
        verticalArrangement = Arrangement.spacedBy(WarmSpacing.sm),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.recipe_detail_ingredients_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Surface(
                shape = PillShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.testTag(RecipeDetailTestTags.INGREDIENTS_COUNT),
            ) {
                Text(
                    text = stringResource(
                        R.string.recipe_detail_ingredients_count,
                        recipe.ingredients.count { ingredient ->
                            ingredient.key() in checkedIds
                        },
                        recipe.ingredients.size,
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(
                        horizontal = WarmSpacing.sm,
                        vertical = 2.dp,
                    ),
                )
            }
        }

        if (recipe.ingredients.isEmpty()) {
            Text(
                text = stringResource(R.string.recipe_detail_ingredients_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // D6: primero los obligatorios en su orden de modelo y luego los opcionales.
        recipe.ingredients
            .sortedBy { it.optional }
            .forEach { ingredient ->
                IngredientRow(
                    ingredient = ingredient,
                    checked = ingredient.key() in checkedIds,
                    onCheckedChange = { checked ->
                        checkedIds = if (checked) {
                            checkedIds + ingredient.key()
                        } else {
                            checkedIds - ingredient.key()
                        }
                    },
                    locale = locale,
                )
            }
    }
}

@Composable
private fun IngredientRow(
    ingredient: RecipeIngredient,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    locale: Locale,
) {
    Surface(
        shape = CardShape,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = Modifier
            .fillMaxWidth()
            .warmShadow(shape = CardShape)
            .testTag(RecipeDetailTestTags.INGREDIENT_ROW),
    ) {
        Row(
            modifier = Modifier.padding(WarmSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.secondary,
                ),
                modifier = Modifier.testTag(RecipeDetailTestTags.INGREDIENT_CHECK),
            )
            Spacer(modifier = Modifier.width(WarmSpacing.sm))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = ingredientLine(ingredient, locale),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (ingredient.optional) {
                    Surface(
                        shape = PillShape,
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = WarmSpacing.xs),
                    ) {
                        Text(
                            text = stringResource(R.string.recipe_detail_ingredient_optional),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(
                                horizontal = WarmSpacing.sm,
                                vertical = 2.dp,
                            ),
                        )
                    }
                }
            }
        }
    }
}

// «cantidad unidad nombre» (D7: formato del idioma activo sin ceros sobrantes).
private fun ingredientLine(ingredient: RecipeIngredient, locale: Locale): String {
    val quantity = if (ingredient.quantity > 0.0) {
        formatQuantity(ingredient.quantity, locale)
    } else {
        ""
    }
    return listOf(quantity, ingredient.unit, ingredient.name)
        .filter { it.isNotBlank() }
        .joinToString(" ")
}

// Identidad estable de la fila para el marcado local del checklist (D3).
private fun RecipeIngredient.key(): String = id.ifBlank { "$name-$quantity-$unit" }

// RF-DR6: pasos resumidos con número de paso, texto completo y chip «N min» cuando
// el Paso tiene temporizador; cabecera con el número de pasos.
@Composable
private fun StepsSummary(recipe: Recipe) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = WarmSpacing.md)
            .testTag(RecipeDetailTestTags.STEPS),
        verticalArrangement = Arrangement.spacedBy(WarmSpacing.sm),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.recipe_detail_steps_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Surface(
                shape = PillShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ) {
                Text(
                    text = stringResource(R.string.recipe_detail_steps_count, recipe.steps.size),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = WarmSpacing.sm, vertical = 2.dp),
                )
            }
        }

        if (recipe.steps.isEmpty()) {
            Text(
                text = stringResource(R.string.recipe_detail_steps_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        recipe.steps.forEachIndexed { index, step ->
            Surface(
                shape = CardShape,
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                modifier = Modifier
                    .fillMaxWidth()
                    .warmShadow(shape = CardShape)
                    .testTag(RecipeDetailTestTags.STEP_ROW),
            ) {
                Row(
                    modifier = Modifier.padding(WarmSpacing.md),
                    verticalAlignment = Alignment.Top,
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primary,
                                shape = CircleShape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = (index + 1).toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                    Spacer(modifier = Modifier.width(WarmSpacing.sm))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = step.text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        step.timerMinutes?.let { minutes ->
                            Surface(
                                shape = PillShape,
                                color = MaterialTheme.colorScheme.tertiary,
                                contentColor = MaterialTheme.colorScheme.onTertiary,
                                modifier = Modifier
                                    .padding(top = WarmSpacing.xs)
                                    .testTag(RecipeDetailTestTags.STEP_TIMER),
                            ) {
                                Text(
                                    text = stringResource(R.string.my_recipes_card_time, minutes),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(
                                        horizontal = WarmSpacing.sm,
                                        vertical = 2.dp,
                                    ),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// RF-DR7 y D4: CTA destacada fija sobre la superficie, siempre habilitada, que
// navega al destino provisional de Modo Cocina.
@Composable
private fun StartCookingBar(onStartCooking: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        shadowElevation = 8.dp,
    ) {
        Box(modifier = Modifier.navigationBarsPadding()) {
            Button(
                onClick = onStartCooking,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = WarmSpacing.md, vertical = WarmSpacing.sm)
                    .heightIn(min = 52.dp)
                    .testTag(RecipeDetailTestTags.START_COOKING),
            ) {
                Icon(
                    imageVector = SkilletIcon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(WarmSpacing.sm))
                Text(
                    text = stringResource(R.string.recipe_detail_start_cooking),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}

@Composable
private fun rememberBitmap(path: String?): ImageBitmap? = remember(path) {
    path?.let { file ->
        runCatching { BitmapFactory.decodeFile(file)?.asImageBitmap() }.getOrNull()
    }
}

private fun Difficulty.labelRes(): Int = when (this) {
    Difficulty.EASY -> R.string.difficulty_easy
    Difficulty.MEDIUM -> R.string.difficulty_medium
    Difficulty.HARD -> R.string.difficulty_hard
}
