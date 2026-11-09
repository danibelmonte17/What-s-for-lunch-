package com.whatsforlunch.app.ui.createrecipe

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DragIndicator
import androidx.compose.material.icons.outlined.FormatListNumbered
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.RestaurantMenu
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.ShoppingBasket
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.whatsforlunch.app.R
import com.whatsforlunch.app.domain.createrecipe.RecipeRequirement
import com.whatsforlunch.app.domain.model.Difficulty
import com.whatsforlunch.app.domain.model.MealMoment
import com.whatsforlunch.app.ui.components.UnitDropdown
import com.whatsforlunch.app.ui.components.unitLabel
import com.whatsforlunch.app.ui.theme.FieldShape
import com.whatsforlunch.app.ui.theme.WarmRadius
import com.whatsforlunch.app.ui.theme.WarmSpacing
import com.whatsforlunch.app.ui.theme.warmFieldColors
import com.whatsforlunch.app.ui.theme.warmShadow

// Identificadores estables para las pruebas instrumentadas (E2E).
object CreateRecipeTestTags {
    const val SCREEN = "create_recipe_screen"
    const val TITLE_FIELD = "create_recipe_title"
    const val STORY_FIELD = "create_recipe_story"
    const val PREP_MINUTES_FIELD = "create_recipe_prep_minutes"
    const val COOK_MINUTES_FIELD = "create_recipe_cook_minutes"
    const val CALORIES_FIELD = "create_recipe_calories"
    const val SERVINGS_VALUE = "create_recipe_servings_value"
    const val SERVINGS_DECREASE = "create_recipe_servings_decrease"
    const val SERVINGS_INCREASE = "create_recipe_servings_increase"
    const val MOMENT_BREAKFAST = "create_recipe_moment_breakfast"
    const val MOMENT_LUNCH = "create_recipe_moment_lunch"
    const val MOMENT_DINNER = "create_recipe_moment_dinner"
    const val MOMENT_DESSERT = "create_recipe_moment_dessert"
    const val DIFFICULTY_EASY = "create_recipe_difficulty_easy"
    const val DIFFICULTY_MEDIUM = "create_recipe_difficulty_medium"
    const val DIFFICULTY_HARD = "create_recipe_difficulty_hard"
    const val PHOTO_PREVIEW = "create_recipe_photo_preview"
    const val PHOTO_GALLERY = "create_recipe_photo_gallery"
    const val PHOTO_CAMERA = "create_recipe_photo_camera"
    const val PHOTO_GENERATE = "create_recipe_photo_generate"
    const val PHOTO_REMOVE = "create_recipe_photo_remove"
    const val ADD_INGREDIENT = "create_recipe_add_ingredient"
    const val ADD_STEP = "create_recipe_add_step"
    const val FAVORITE_TOGGLE = "create_recipe_favorite"
    const val SAVE = "create_recipe_save"
    const val NOTICE = "create_recipe_notice"
    const val SAVE_ERRORS = "create_recipe_save_errors"

    // Bloques y controles del rediseño fiel al prototipo (RF-1 a RF-8, RF-21).
    // La cabecera ya no dibuja «Guardar» (RF-21): el tag se conserva para que los E2E
    // puedan afirmar su ausencia sin depender del idioma.
    const val HEADER_SAVE = "create_recipe_header_save"
    const val COVER = "create_recipe_cover"
    const val BASIC_SECTION = "create_recipe_basic_section"
    const val METRICS_GRID = "create_recipe_metrics_grid"
    const val METRICS_TIME = "create_recipe_metrics_time"
    const val METRICS_DIFFICULTY = "create_recipe_metrics_difficulty"
    const val METRICS_SERVINGS = "create_recipe_metrics_servings"
    const val INGREDIENTS_SECTION = "create_recipe_ingredients_section"
    const val STEPS_SECTION = "create_recipe_steps_section"
    const val FAVORITE_SECTION = "create_recipe_favorite_section"
    const val ACTION_DOCK = "create_recipe_action_dock"

    fun ingredientName(index: Int) = "create_recipe_ingredient_name_$index"

    fun ingredientQuantity(index: Int) = "create_recipe_ingredient_quantity_$index"

    fun ingredientUnit(index: Int) = "create_recipe_ingredient_unit_$index"

    fun ingredientOptional(index: Int) = "create_recipe_ingredient_optional_$index"

    fun removeIngredient(index: Int) = "create_recipe_remove_ingredient_$index"

    fun stepText(index: Int) = "create_recipe_step_text_$index"

    fun stepTimer(index: Int) = "create_recipe_step_timer_$index"

    fun removeStep(index: Int) = "create_recipe_remove_step_$index"

    fun stepNumber(index: Int) = "create_recipe_step_number_$index"

    // Vínculo Paso↔Ingrediente (RD21): control por fila de Paso, su diálogo de
    // selección y sus casillas (indexadas por la fila de Ingrediente de la Receta).
    const val STEP_INGREDIENTS_DIALOG = "create_recipe_step_ingredients_dialog"
    const val STEP_INGREDIENTS_ACCEPT = "create_recipe_step_ingredients_accept"
    const val STEP_INGREDIENTS_CANCEL = "create_recipe_step_ingredients_cancel"
    const val STEP_INGREDIENTS_EMPTY = "create_recipe_step_ingredients_empty"

    fun stepIngredients(index: Int) = "create_recipe_step_ingredients_$index"

    fun stepIngredientOption(index: Int) = "create_recipe_step_ingredient_option_$index"
}

// Acciones del formulario, agrupadas para no saturar la firma de la pantalla.
class CreateRecipeActions(
    val onBack: () -> Unit = {},
    val onTitleChange: (String) -> Unit = {},
    val onStoryChange: (String) -> Unit = {},
    val onMealMomentChange: (MealMoment) -> Unit = {},
    val onPrepMinutesChange: (String) -> Unit = {},
    val onCookMinutesChange: (String) -> Unit = {},
    val onDifficultyChange: (Difficulty) -> Unit = {},
    val onCaloriesChange: (String) -> Unit = {},
    val onServingsChange: (Int) -> Unit = {},
    val onFavoriteToggle: () -> Unit = {},
    val onAddIngredient: () -> Unit = {},
    val onIngredientNameChange: (Long, String) -> Unit = { _, _ -> },
    val onIngredientQuantityChange: (Long, String) -> Unit = { _, _ -> },
    val onIngredientUnitChange: (Long, String) -> Unit = { _, _ -> },
    val onIngredientOptionalChange: (Long, Boolean) -> Unit = { _, _ -> },
    val onRemoveIngredient: (Long) -> Unit = {},
    val onAddStep: () -> Unit = {},
    val onStepTextChange: (Long, String) -> Unit = { _, _ -> },
    val onStepTimerChange: (Long, String) -> Unit = { _, _ -> },
    val onStepIngredientToggle: (Long, String) -> Unit = { _, _ -> },
    val onRemoveStep: (Long) -> Unit = {},
    val onPickPhotoFromGallery: () -> Unit = {},
    val onTakePhoto: () -> Unit = {},
    val onGeneratePhoto: () -> Unit = {},
    val onRemovePhoto: () -> Unit = {},
    val onSave: () -> Unit = {},
    val onDismissNotice: () -> Unit = {},
)

// Formas del sistema visual: 1rem en secciones, 0.75rem en campos (FieldShape
// compartido en ui/theme/Field.kt con el desplegable de unidades) y píldora en
// chips y badges (RF-13).
private val SectionShape: Shape = RoundedCornerShape(WarmRadius.lg)
private val PillShape: Shape = RoundedCornerShape(50)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRecipeScreen(
    uiState: CreateRecipeUiState,
    actions: CreateRecipeActions,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.testTag(CreateRecipeTestTags.SCREEN),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = { FormHeader(uiState = uiState, actions = actions) },
        bottomBar = { ActionDock(uiState = uiState, actions = actions) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = WarmSpacing.md)
                .padding(bottom = WarmSpacing.md),
            verticalArrangement = Arrangement.spacedBy(WarmSpacing.md),
        ) {
            uiState.notice?.let { notice ->
                NoticeBanner(notice = notice, onDismiss = actions.onDismissNotice)
            }
            PhotoCover(uiState = uiState, actions = actions)
            BasicInfoSection(uiState = uiState, actions = actions)
            MetricsGrid(uiState = uiState, actions = actions)
            IngredientsSection(uiState = uiState, actions = actions)
            StepsSection(uiState = uiState, actions = actions)
            FavoriteSection(uiState = uiState, actions = actions)
        }
    }
}

// RF-3: cabecera con el título del formulario («Crear receta» en alta, «Editar Receta»
// al editar), sin distintivo de estado (el estado Borrador/Publicada ya no existe).
// RF-13, RF-14: la cabecera no dibuja «Guardar» en ningún modo: el único punto de
// guardado son las acciones del dock (decisión Q-editar-receta-5, 2026-10-19).
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FormHeader(uiState: CreateRecipeUiState, actions: CreateRecipeActions) {
    val editing = uiState.recipeId != null
    TopAppBar(
        navigationIcon = {
            TextButton(
                onClick = actions.onBack,
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text(text = stringResource(R.string.action_back))
            }
        },
        title = {
            Column {
                Text(
                    text = stringResource(
                        if (editing) R.string.create_recipe_edit_title else R.string.create_recipe_title,
                    ),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = stringResource(R.string.create_recipe_header_subtitle),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
        ),
    )
}

@Composable
private fun NoticeBanner(notice: CreateRecipeNotice, onDismiss: () -> Unit) {
    Surface(
        shape = SectionShape,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = Modifier
            .fillMaxWidth()
            .warmShadow(shape = SectionShape)
            .testTag(CreateRecipeTestTags.NOTICE),
    ) {
        Row(
            modifier = Modifier.padding(WarmSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(notice.messageRes()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(text = stringResource(R.string.action_dismiss))
            }
        }
    }
}

// RF-15: junto al dock se enumeran los requisitos incumplidos que impiden Guardar.
@Composable
private fun SaveErrors(errors: Set<RecipeRequirement>) {
    Column(modifier = Modifier.fillMaxWidth().testTag(CreateRecipeTestTags.SAVE_ERRORS)) {
        Text(
            text = stringResource(R.string.create_recipe_save_blocked),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.error,
        )
        errors.sortedBy { it.ordinal }.forEach { requirement ->
            Text(
                text = stringResource(requirement.messageRes()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

// RF-2: portada de foto a ancho completo con los controles de foto al pie.
@Composable
private fun PhotoCover(uiState: CreateRecipeUiState, actions: CreateRecipeActions) {
    val bitmap = rememberBitmap(uiState.photoPath)
    val coverLabel = stringResource(R.string.create_recipe_photo_section)
    Surface(
        shape = SectionShape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier
            .fillMaxWidth()
            .warmShadow(shape = SectionShape)
            .semantics { contentDescription = coverLabel }
            .testTag(CreateRecipeTestTags.COVER),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = stringResource(R.string.create_recipe_photo_preview),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(SectionShape)
                        .testTag(CreateRecipeTestTags.PHOTO_PREVIEW),
                )
                TextButton(
                    onClick = actions.onRemovePhoto,
                    colors = ButtonDefaults.textButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.9f),
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(WarmSpacing.sm)
                        .heightIn(min = 48.dp)
                        .testTag(CreateRecipeTestTags.PHOTO_REMOVE),
                ) {
                    Text(text = stringResource(R.string.create_recipe_photo_remove))
                }
            } else {
                Text(
                    text = stringResource(
                        if (uiState.isPhotoLoading) {
                            R.string.create_recipe_photo_loading
                        } else {
                            R.string.create_recipe_photo_none
                        },
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(WarmSpacing.sm),
                horizontalArrangement = Arrangement.spacedBy(WarmSpacing.sm),
            ) {
                PhotoControlButton(
                    labelRes = R.string.create_recipe_photo_gallery,
                    icon = Icons.Outlined.PhotoLibrary,
                    onClick = actions.onPickPhotoFromGallery,
                    testTag = CreateRecipeTestTags.PHOTO_GALLERY,
                    modifier = Modifier.weight(1f),
                )
                PhotoControlButton(
                    labelRes = R.string.create_recipe_photo_camera,
                    icon = Icons.Outlined.PhotoCamera,
                    onClick = actions.onTakePhoto,
                    testTag = CreateRecipeTestTags.PHOTO_CAMERA,
                    modifier = Modifier.weight(1f),
                )
                PhotoControlButton(
                    labelRes = R.string.create_recipe_photo_generate,
                    icon = Icons.Outlined.AutoAwesome,
                    onClick = actions.onGeneratePhoto,
                    testTag = CreateRecipeTestTags.PHOTO_GENERATE,
                    modifier = Modifier.weight(1.3f),
                    primary = true,
                )
            }
        }
    }
}

@Composable
private fun RowScope.PhotoControlButton(
    labelRes: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
) {
    Button(
        onClick = onClick,
        shape = FieldShape,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = WarmSpacing.xs,
            vertical = WarmSpacing.xs,
        ),
        colors = if (primary) {
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            )
        } else {
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                contentColor = MaterialTheme.colorScheme.onSurface,
            )
        },
        modifier = modifier.heightIn(min = 48.dp).testTag(testTag),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (primary) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.primary
            },
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(WarmSpacing.xs))
        Text(text = stringResource(labelRes), style = MaterialTheme.typography.labelMedium)
    }
}

// RF-4 e información básica: título, historia y fila de chips de momento culinario.
@Composable
private fun BasicInfoSection(uiState: CreateRecipeUiState, actions: CreateRecipeActions) {
    SectionCard(testTag = CreateRecipeTestTags.BASIC_SECTION) {
        Text(
            text = stringResource(R.string.create_recipe_basic_section),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.create_recipe_title_label),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.weight(1f))
            Surface(
                shape = PillShape,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ) {
                Text(
                    text = stringResource(R.string.create_recipe_required_badge),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(
                        horizontal = WarmSpacing.sm,
                        vertical = 2.dp,
                    ),
                )
            }
        }
        OutlinedTextField(
            value = uiState.title,
            onValueChange = actions.onTitleChange,
            placeholder = { Text(text = stringResource(R.string.create_recipe_title_placeholder)) },
            isError = uiState.titleError,
            supportingText = if (uiState.titleError) {
                { Text(text = stringResource(R.string.create_recipe_title_required)) }
            } else {
                null
            },
            singleLine = true,
            shape = FieldShape,
            colors = warmFieldColors(),
            modifier = Modifier.fillMaxWidth().testTag(CreateRecipeTestTags.TITLE_FIELD),
        )
        Text(
            text = stringResource(R.string.create_recipe_story_label),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        OutlinedTextField(
            value = uiState.story,
            onValueChange = actions.onStoryChange,
            placeholder = { Text(text = stringResource(R.string.create_recipe_story_placeholder)) },
            minLines = 2,
            shape = FieldShape,
            colors = warmFieldColors(),
            modifier = Modifier.fillMaxWidth().testTag(CreateRecipeTestTags.STORY_FIELD),
        )
        OutlinedTextField(
            value = uiState.caloriesPerServing,
            onValueChange = actions.onCaloriesChange,
            placeholder = { Text(text = stringResource(R.string.create_recipe_calories_label)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = FieldShape,
            colors = warmFieldColors(),
            modifier = Modifier.fillMaxWidth().testTag(CreateRecipeTestTags.CALORIES_FIELD),
        )
        Text(
            text = stringResource(R.string.create_recipe_moment_label),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(WarmSpacing.sm),
        ) {
            MealMoment.entries.forEach { moment ->
                SelectionChip(
                    selected = uiState.mealMoment == moment,
                    onClick = { actions.onMealMomentChange(moment) },
                    label = stringResource(moment.labelRes()),
                    testTag = moment.testTag(),
                )
            }
        }
    }
}

// RF-5: rejilla de tres bloques (tiempo, dificultad y raciones) con icono y control.
@Composable
private fun MetricsGrid(uiState: CreateRecipeUiState, actions: CreateRecipeActions) {
    Column(verticalArrangement = Arrangement.spacedBy(WarmSpacing.sm)) {
        Text(
            text = stringResource(R.string.create_recipe_metrics_section),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(CreateRecipeTestTags.METRICS_GRID),
            horizontalArrangement = Arrangement.spacedBy(WarmSpacing.sm),
        ) {
            TimeMetricBlock(uiState = uiState, actions = actions, modifier = Modifier.weight(1f))
            DifficultyMetricBlock(uiState = uiState, actions = actions, modifier = Modifier.weight(1f))
            ServingsMetricBlock(uiState = uiState, actions = actions, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun TimeMetricBlock(
    uiState: CreateRecipeUiState,
    actions: CreateRecipeActions,
    modifier: Modifier = Modifier,
) {
    MetricBlock(
        label = R.string.create_recipe_metrics_time,
        icon = Icons.Outlined.Timer,
        iconTint = MaterialTheme.colorScheme.primary,
        testTag = CreateRecipeTestTags.METRICS_TIME,
        modifier = modifier,
    ) {
        Text(
            text = stringResource(R.string.create_recipe_prep_short_label),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = uiState.prepMinutes,
            onValueChange = actions.onPrepMinutesChange,
            placeholder = { Text(text = "0") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = FieldShape,
            colors = warmFieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .testTag(CreateRecipeTestTags.PREP_MINUTES_FIELD),
        )
        Text(
            text = stringResource(R.string.create_recipe_cook_short_label),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = uiState.cookMinutes,
            onValueChange = actions.onCookMinutesChange,
            placeholder = { Text(text = "0") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = FieldShape,
            colors = warmFieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .testTag(CreateRecipeTestTags.COOK_MINUTES_FIELD),
        )
        Text(
            text = stringResource(R.string.create_recipe_metrics_time_unit),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DifficultyMetricBlock(
    uiState: CreateRecipeUiState,
    actions: CreateRecipeActions,
    modifier: Modifier = Modifier,
) {
    MetricBlock(
        label = R.string.create_recipe_metrics_difficulty,
        icon = Icons.Outlined.BarChart,
        iconTint = MaterialTheme.colorScheme.secondary,
        testTag = CreateRecipeTestTags.METRICS_DIFFICULTY,
        modifier = modifier,
    ) {
        Difficulty.entries.forEach { difficulty ->
            SelectionChip(
                selected = uiState.difficulty == difficulty,
                onClick = { actions.onDifficultyChange(difficulty) },
                label = stringResource(difficulty.labelRes()),
                testTag = difficulty.testTag(),
            )
        }
    }
}

@Composable
private fun ServingsMetricBlock(
    uiState: CreateRecipeUiState,
    actions: CreateRecipeActions,
    modifier: Modifier = Modifier,
) {
    MetricBlock(
        label = R.string.create_recipe_metrics_servings,
        icon = Icons.Outlined.Groups,
        iconTint = MaterialTheme.colorScheme.tertiary,
        testTag = CreateRecipeTestTags.METRICS_SERVINGS,
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = uiState.baseServings.toString(),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.testTag(CreateRecipeTestTags.SERVINGS_VALUE),
            )
            Text(
                text = stringResource(R.string.create_recipe_metrics_servings_unit),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            StepperButton(
                symbol = stringResource(R.string.create_recipe_servings_decrease),
                testTag = CreateRecipeTestTags.SERVINGS_DECREASE,
                onClick = { actions.onServingsChange(uiState.baseServings - 1) },
            )
            StepperButton(
                symbol = stringResource(R.string.create_recipe_servings_increase),
                testTag = CreateRecipeTestTags.SERVINGS_INCREASE,
                onClick = { actions.onServingsChange(uiState.baseServings + 1) },
            )
        }
    }
}

@Composable
private fun MetricBlock(
    label: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    testTag: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        shape = SectionShape,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = modifier
            .warmShadow(shape = SectionShape)
            .testTag(testTag),
    ) {
        Column(
            modifier = Modifier.padding(WarmSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(WarmSpacing.xs),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = stringResource(label),
                    tint = iconTint,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(modifier = Modifier.width(WarmSpacing.xs))
                Text(
                    text = stringResource(label),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            content()
        }
    }
}

@Composable
private fun StepperButton(symbol: String, testTag: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick)
            .testTag(testTag),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = symbol,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

// Chip de selección con los roles de token del prototipo: seleccionado en
// primary/on-primary y no seleccionado en surface-container-low (RF-10, RF-11).
@Composable
private fun SelectionChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    testTag: String,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
            )
        },
        shape = PillShape,
        border = null,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        modifier = Modifier.heightIn(min = 48.dp).testTag(testTag),
    )
}

// RF-6: cada fila de Ingrediente en línea (asa decorativa sin reordenado,
// Q-afinar-fidelidad-visual-del-formulario-1).
@Composable
private fun IngredientsSection(uiState: CreateRecipeUiState, actions: CreateRecipeActions) {
    SectionCard(testTag = CreateRecipeTestTags.INGREDIENTS_SECTION) {
        SectionHeader(
            icon = Icons.Outlined.ShoppingBasket,
            iconTint = MaterialTheme.colorScheme.secondary,
            title = R.string.create_recipe_ingredients_title,
            trailing = {
                CountBadge(
                    text = stringResource(R.string.create_recipe_ingredients_count, uiState.ingredients.size),
                )
            },
        )
        uiState.ingredients.forEachIndexed { index, row ->
            IngredientRow(row = row, index = index, actions = actions)
        }
        AddRowButton(
            onClick = actions.onAddIngredient,
            label = R.string.create_recipe_add_ingredient,
            testTag = CreateRecipeTestTags.ADD_INGREDIENT,
        )
    }
}

@Composable
private fun IngredientRow(row: IngredientFieldUiState, index: Int, actions: CreateRecipeActions) {
    val optionalLabel = stringResource(R.string.create_recipe_ingredient_optional)
    val quantityLabel = stringResource(R.string.create_recipe_ingredient_quantity)
    Column(verticalArrangement = Arrangement.spacedBy(WarmSpacing.xs)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(WarmSpacing.xs),
        ) {
            Icon(
                imageVector = Icons.Outlined.DragIndicator,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(18.dp),
            )
            OutlinedTextField(
                value = row.name,
                onValueChange = { actions.onIngredientNameChange(row.key, it) },
                placeholder = { Text(text = stringResource(R.string.create_recipe_ingredient_name)) },
                singleLine = true,
                shape = FieldShape,
                colors = warmFieldColors(),
                modifier = Modifier
                    .weight(1.2f)
                    .heightIn(min = 48.dp)
                    .testTag(CreateRecipeTestTags.ingredientName(index)),
            )
            OutlinedTextField(
                value = row.quantity,
                onValueChange = { actions.onIngredientQuantityChange(row.key, it) },
                placeholder = { Text(text = "0") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = FieldShape,
                colors = warmFieldColors(),
                modifier = Modifier
                    .weight(0.7f)
                    .heightIn(min = 48.dp)
                    .semantics { contentDescription = quantityLabel }
                    .testTag(CreateRecipeTestTags.ingredientQuantity(index)),
            )
            // Unidad de lista cerrada (unificar-unidades, D1): desplegable con los
            // cinco valores del enum; una unidad legada entra como opción extra. El
            // peso 1.1 deja la etiqueta «Unidad» y valores legados de una línea.
            UnitDropdown(
                value = row.unit,
                onValueChange = { actions.onIngredientUnitChange(row.key, it) },
                testTag = CreateRecipeTestTags.ingredientUnit(index),
                label = { Text(text = stringResource(R.string.create_recipe_ingredient_unit)) },
                modifier = Modifier.weight(1.1f),
            )
            Checkbox(
                checked = row.optional,
                onCheckedChange = { actions.onIngredientOptionalChange(row.key, it) },
                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .semantics { contentDescription = optionalLabel }
                    .testTag(CreateRecipeTestTags.ingredientOptional(index)),
            )
            IconButton(
                onClick = { actions.onRemoveIngredient(row.key) },
                modifier = Modifier.testTag(CreateRecipeTestTags.removeIngredient(index)),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = stringResource(R.string.create_recipe_remove),
                    tint = MaterialTheme.colorScheme.outline,
                )
            }
        }
        if (!row.isValid) {
            Text(
                text = stringResource(R.string.create_recipe_ingredient_invalid),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

// RF-7: cada Paso con su número en un círculo destacado, su texto y su temporizador.
@Composable
private fun StepsSection(uiState: CreateRecipeUiState, actions: CreateRecipeActions) {
    SectionCard(testTag = CreateRecipeTestTags.STEPS_SECTION) {
        SectionHeader(
            icon = Icons.Outlined.FormatListNumbered,
            iconTint = MaterialTheme.colorScheme.primary,
            title = R.string.create_recipe_steps_title,
            trailing = {
                CountBadge(
                    text = stringResource(R.string.create_recipe_steps_count, uiState.steps.size),
                )
            },
        )
        uiState.steps.forEachIndexed { index, row ->
            StepRow(row = row, index = index, ingredients = uiState.ingredients, actions = actions)
        }
        AddRowButton(
            onClick = actions.onAddStep,
            label = R.string.create_recipe_add_step,
            testTag = CreateRecipeTestTags.ADD_STEP,
        )
    }
}

@Composable
private fun StepRow(
    row: StepFieldUiState,
    index: Int,
    ingredients: List<IngredientFieldUiState>,
    actions: CreateRecipeActions,
) {
    val stepLabel = stringResource(R.string.create_recipe_step_number, index + 1)
    var showIngredientsDialog by remember(row.key) { mutableStateOf(false) }
    Surface(
        shape = FieldShape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(WarmSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(WarmSpacing.xs),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(WarmSpacing.sm),
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .semantics { contentDescription = stepLabel }
                        .testTag(CreateRecipeTestTags.stepNumber(index)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = (index + 1).toString(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
                OutlinedTextField(
                    value = row.text,
                    onValueChange = { actions.onStepTextChange(row.key, it) },
                    placeholder = { Text(text = stringResource(R.string.create_recipe_step_text)) },
                    minLines = 2,
                    shape = FieldShape,
                    colors = warmFieldColors(),
                    modifier = Modifier
                        .weight(1f)
                        .testTag(CreateRecipeTestTags.stepText(index)),
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(WarmSpacing.sm),
            ) {
                OutlinedTextField(
                    value = row.timerMinutes,
                    onValueChange = { actions.onStepTimerChange(row.key, it) },
                    placeholder = { Text(text = stringResource(R.string.create_recipe_step_timer)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = FieldShape,
                    colors = warmFieldColors(),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag(CreateRecipeTestTags.stepTimer(index)),
                )
                IconButton(
                    onClick = { actions.onRemoveStep(row.key) },
                    modifier = Modifier.testTag(CreateRecipeTestTags.removeStep(index)),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = stringResource(R.string.create_recipe_remove),
                        tint = MaterialTheme.colorScheme.outline,
                    )
                }
            }
            // RF-VPI2 (RD21): «Ingredientes de este paso», debajo del temporizador.
            // El control solo se muestra con ≥1 fila de Ingrediente (D3).
            if (ingredients.isNotEmpty()) {
                StepIngredientsControl(
                    row = row,
                    index = index,
                    onClick = { showIngredientsDialog = true },
                )
            }
            if (!row.isValid) {
                Text(
                    text = stringResource(R.string.create_recipe_step_invalid),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
    if (showIngredientsDialog) {
        StepIngredientsDialog(
            row = row,
            ingredients = ingredients,
            onToggle = { actions.onStepIngredientToggle(row.key, it) },
            onDismiss = { showIngredientsDialog = false },
        )
    }
}

// Control «Ingredientes de este paso»: resumen de la selección del Paso («Sin
// ingredientes» o «N ingredientes») que abre el diálogo de casillas (RF-VPI2).
@Composable
private fun StepIngredientsControl(
    row: StepFieldUiState,
    index: Int,
    onClick: () -> Unit,
) {
    Surface(
        shape = FieldShape,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(WarmSpacing.sm),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clickable(onClick = onClick)
                .padding(horizontal = WarmSpacing.sm)
                .testTag(CreateRecipeTestTags.stepIngredients(index)),
        ) {
            Icon(
                imageVector = Icons.Outlined.ShoppingBasket,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = stringResource(R.string.create_recipe_step_ingredients),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stepIngredientsSummary(row.ingredientIds.size),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

// Resumen de los vínculos de un Paso en su control (RF-VPI2).
@Composable
private fun stepIngredientsSummary(count: Int): String =
    if (count == 0) {
        stringResource(R.string.create_recipe_step_ingredients_none)
    } else {
        pluralStringResource(R.plurals.create_recipe_step_ingredients_count, count, count)
    }

// Etiqueta de una opción del diálogo: «nombre (cantidad unidad)» (RF-VPI2); la
// unidad llega presentada con `unitLabel` (unificar-unidades, D4).
private fun ingredientOptionLabel(ingredient: IngredientFieldUiState, unit: String): String {
    val details = listOf(ingredient.quantity, unit)
        .filter { it.isNotBlank() }
        .joinToString(" ")
    return if (details.isEmpty()) ingredient.name else "${ingredient.name} ($details)"
}

// Diálogo de selección de los Ingredientes de un Paso (N:M, D2): lista las filas de
// Ingrediente de la Receta en orden con una casilla por fila (D7). La selección se
// confirma con «Aceptar»; «Cancelar» no toca la Receta. El estado vacío es defensivo:
// el control no se renderiza sin ingredientes (D3).
@Composable
private fun StepIngredientsDialog(
    row: StepFieldUiState,
    ingredients: List<IngredientFieldUiState>,
    onToggle: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val initial = remember(row.key) { row.ingredientIds.toSet() }
    var selection by remember(row.key) { mutableStateOf(row.ingredientIds.toSet()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag(CreateRecipeTestTags.STEP_INGREDIENTS_DIALOG),
        title = { Text(stringResource(R.string.create_recipe_step_ingredients)) },
        text = {
            if (ingredients.isEmpty()) {
                Text(
                    text = stringResource(R.string.create_recipe_step_ingredients_empty),
                    modifier = Modifier.testTag(CreateRecipeTestTags.STEP_INGREDIENTS_EMPTY),
                )
            } else {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(WarmSpacing.xs),
                ) {
                    ingredients.forEachIndexed { ingredientIndex, ingredient ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = ingredient.id in selection,
                                onCheckedChange = {
                                    selection = if (ingredient.id in selection) {
                                        selection - ingredient.id
                                    } else {
                                        selection + ingredient.id
                                    }
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = MaterialTheme.colorScheme.primary,
                                ),
                                modifier = Modifier.testTag(
                                    CreateRecipeTestTags.stepIngredientOption(ingredientIndex),
                                ),
                            )
                            Text(
                                text = ingredientOptionLabel(ingredient, unitLabel(ingredient.unit)),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    // Se aplica solo el diff sobre la instantánea de apertura: un toggle
                    // por id cambiado; el ViewModel guarda la selección en orden (D7).
                    (initial + selection).forEach { id ->
                        if ((id in initial) != (id in selection)) onToggle(id)
                    }
                    onDismiss()
                },
                modifier = Modifier.heightIn(min = 48.dp).testTag(CreateRecipeTestTags.STEP_INGREDIENTS_ACCEPT),
            ) {
                Text(stringResource(R.string.create_recipe_step_ingredients_accept))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = 48.dp).testTag(CreateRecipeTestTags.STEP_INGREDIENTS_CANCEL),
            ) {
                Text(stringResource(R.string.create_recipe_step_ingredients_cancel))
            }
        },
    )
}

// Bloque de favorito con su control de marcado (RF-1; control según el prototipo).
@Composable
private fun FavoriteSection(uiState: CreateRecipeUiState, actions: CreateRecipeActions) {
    SectionCard(testTag = CreateRecipeTestTags.FAVORITE_SECTION) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.Favorite,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(WarmSpacing.sm))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.create_recipe_favorite),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.create_recipe_favorite_subtitle),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Checkbox(
                checked = uiState.favorite,
                onCheckedChange = { actions.onFavoriteToggle() },
                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.testTag(CreateRecipeTestTags.FAVORITE_TOGGLE),
            )
        }
    }
}

// RF-EBF2/RF-EBF3: dock siempre visible con el único botón de guardado («Guardar»
// en el alta, «Guardar cambios» en edición), deshabilitado (estilo distinguible)
// mientras falten requisitos, que se enumeran junto al dock.
@Composable
private fun ActionDock(uiState: CreateRecipeUiState, actions: CreateRecipeActions) {
    val editing = uiState.recipeId != null
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = Modifier
            .fillMaxWidth()
            .warmShadow(shape = RoundedCornerShape(0.dp))
            .testTag(CreateRecipeTestTags.ACTION_DOCK),
    ) {
        Column(
            modifier = Modifier
                // El dock se dibuja bajo la barra de navegación; su contenido
                // (acciones y errores) queda siempre visible por encima.
                .navigationBarsPadding()
                .padding(WarmSpacing.md),
            verticalArrangement = Arrangement.spacedBy(WarmSpacing.sm),
        ) {
            if (uiState.saveErrors.isNotEmpty()) {
                SaveErrors(errors = uiState.saveErrors)
            }
            Button(
                onClick = actions.onSave,
                enabled = uiState.canSave,
                shape = FieldShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .testTag(CreateRecipeTestTags.SAVE),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Save,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(WarmSpacing.xs))
                Text(
                    text = stringResource(
                        if (editing) R.string.create_recipe_save_changes else R.string.create_recipe_save,
                    ),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Composable
private fun SectionCard(testTag: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = SectionShape,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = Modifier
            .fillMaxWidth()
            .warmShadow(shape = SectionShape)
            .testTag(testTag),
    ) {
        Column(
            modifier = Modifier.padding(WarmSpacing.md),
            verticalArrangement = Arrangement.spacedBy(WarmSpacing.sm),
            content = content,
        )
    }
}

@Composable
private fun SectionHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: Int,
    trailing: @Composable () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = stringResource(title),
            tint = iconTint,
            modifier = Modifier.size(22.dp),
        )
        Spacer(modifier = Modifier.width(WarmSpacing.sm))
        Text(
            text = stringResource(title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        trailing()
    }
}

@Composable
private fun CountBadge(text: String) {
    Surface(
        shape = PillShape,
        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.10f),
        contentColor = MaterialTheme.colorScheme.secondary,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = WarmSpacing.sm, vertical = 2.dp),
        )
    }
}

@Composable
private fun AddRowButton(onClick: () -> Unit, label: Int, testTag: String) {
    Button(
        onClick = onClick,
        shape = FieldShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.primary,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .testTag(testTag),
    ) {
        Icon(
            imageVector = Icons.Outlined.AddCircle,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(WarmSpacing.sm))
        Text(text = stringResource(label), style = MaterialTheme.typography.labelLarge)
    }
}

// Campos rellenos: warmFieldColors vive en ui/theme/Field.kt (compartido con el
// desplegable de unidades).

@Composable
private fun rememberBitmap(path: String?): ImageBitmap? = remember(path) {
    path?.let { file ->
        runCatching { BitmapFactory.decodeFile(file)?.asImageBitmap() }.getOrNull()
    }
}

private fun MealMoment.labelRes(): Int = when (this) {
    MealMoment.BREAKFAST -> R.string.meal_moment_breakfast
    MealMoment.LUNCH -> R.string.meal_moment_lunch
    MealMoment.DINNER -> R.string.meal_moment_dinner
    MealMoment.DESSERT -> R.string.meal_moment_dessert
}

private fun MealMoment.testTag(): String = when (this) {
    MealMoment.BREAKFAST -> CreateRecipeTestTags.MOMENT_BREAKFAST
    MealMoment.LUNCH -> CreateRecipeTestTags.MOMENT_LUNCH
    MealMoment.DINNER -> CreateRecipeTestTags.MOMENT_DINNER
    MealMoment.DESSERT -> CreateRecipeTestTags.MOMENT_DESSERT
}

private fun Difficulty.labelRes(): Int = when (this) {
    Difficulty.EASY -> R.string.difficulty_easy
    Difficulty.MEDIUM -> R.string.difficulty_medium
    Difficulty.HARD -> R.string.difficulty_hard
}

private fun Difficulty.testTag(): String = when (this) {
    Difficulty.EASY -> CreateRecipeTestTags.DIFFICULTY_EASY
    Difficulty.MEDIUM -> CreateRecipeTestTags.DIFFICULTY_MEDIUM
    Difficulty.HARD -> CreateRecipeTestTags.DIFFICULTY_HARD
}

private fun CreateRecipeNotice.messageRes(): Int = when (this) {
    CreateRecipeNotice.AI_KEY_MISSING -> R.string.create_recipe_notice_ai_key_missing
    CreateRecipeNotice.AI_GENERATION_FAILED -> R.string.create_recipe_notice_ai_failed
    CreateRecipeNotice.AI_GENERATION_FAILED_NO_CONNECTION -> R.string.create_recipe_notice_ai_failed_no_connection
    CreateRecipeNotice.AI_GENERATION_FAILED_PROVIDER_ERROR -> R.string.create_recipe_notice_ai_failed_provider
    CreateRecipeNotice.AI_GENERATION_FAILED_INVALID_RESPONSE -> R.string.create_recipe_notice_ai_failed_invalid_response
    CreateRecipeNotice.CAMERA_PERMISSION_DENIED -> R.string.create_recipe_notice_camera_denied
}

private fun RecipeRequirement.messageRes(): Int = when (this) {
    RecipeRequirement.TITLE -> R.string.create_recipe_save_error_title
    RecipeRequirement.INGREDIENT -> R.string.create_recipe_save_error_ingredient
    RecipeRequirement.STEP -> R.string.create_recipe_save_error_step
    RecipeRequirement.INVALID_INGREDIENT -> R.string.create_recipe_save_error_invalid_ingredient
    RecipeRequirement.INVALID_STEP -> R.string.create_recipe_save_error_invalid_step
}
