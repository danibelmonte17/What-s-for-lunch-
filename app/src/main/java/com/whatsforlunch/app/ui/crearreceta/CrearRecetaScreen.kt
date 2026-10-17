package com.whatsforlunch.app.ui.crearreceta

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.whatsforlunch.app.R
import com.whatsforlunch.app.domain.crearreceta.RecipeRequirement
import com.whatsforlunch.app.domain.model.Difficulty
import com.whatsforlunch.app.domain.model.MealMoment
import com.whatsforlunch.app.ui.theme.WarmSpacing

// Identificadores estables para las pruebas instrumentadas (E2E).
object CrearRecetaTestTags {
    const val SCREEN = "crear_receta_screen"
    const val TITLE_FIELD = "crear_receta_title"
    const val STORY_FIELD = "crear_receta_story"
    const val PREP_MINUTES_FIELD = "crear_receta_prep_minutes"
    const val COOK_MINUTES_FIELD = "crear_receta_cook_minutes"
    const val CALORIES_FIELD = "crear_receta_calories"
    const val SERVINGS_VALUE = "crear_receta_servings_value"
    const val SERVINGS_DECREASE = "crear_receta_servings_decrease"
    const val SERVINGS_INCREASE = "crear_receta_servings_increase"
    const val MOMENT_BREAKFAST = "crear_receta_moment_breakfast"
    const val MOMENT_LUNCH = "crear_receta_moment_lunch"
    const val MOMENT_DINNER = "crear_receta_moment_dinner"
    const val MOMENT_DESSERT = "crear_receta_moment_dessert"
    const val DIFFICULTY_EASY = "crear_receta_difficulty_easy"
    const val DIFFICULTY_MEDIUM = "crear_receta_difficulty_medium"
    const val DIFFICULTY_HARD = "crear_receta_difficulty_hard"
    const val PHOTO_PREVIEW = "crear_receta_photo_preview"
    const val PHOTO_GALLERY = "crear_receta_photo_gallery"
    const val PHOTO_CAMERA = "crear_receta_photo_camera"
    const val PHOTO_GENERATE = "crear_receta_photo_generate"
    const val PHOTO_REMOVE = "crear_receta_photo_remove"
    const val ADD_INGREDIENT = "crear_receta_add_ingredient"
    const val ADD_STEP = "crear_receta_add_step"
    const val FAVORITE_TOGGLE = "crear_receta_favorite"
    const val SAVE_DRAFT = "crear_receta_save_draft"
    const val PUBLISH = "crear_receta_publish"
    const val NOTICE = "crear_receta_notice"
    const val PUBLISH_ERRORS = "crear_receta_publish_errors"

    fun ingredientName(index: Int) = "crear_receta_ingredient_name_$index"

    fun ingredientQuantity(index: Int) = "crear_receta_ingredient_quantity_$index"

    fun ingredientUnit(index: Int) = "crear_receta_ingredient_unit_$index"

    fun ingredientOptional(index: Int) = "crear_receta_ingredient_optional_$index"

    fun removeIngredient(index: Int) = "crear_receta_remove_ingredient_$index"

    fun stepText(index: Int) = "crear_receta_step_text_$index"

    fun stepTimer(index: Int) = "crear_receta_step_timer_$index"

    fun removeStep(index: Int) = "crear_receta_remove_step_$index"
}

// Acciones del formulario, agrupadas para no saturar la firma de la pantalla.
class CrearRecetaActions(
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
    val onRemoveStep: (Long) -> Unit = {},
    val onPickPhotoFromGallery: () -> Unit = {},
    val onTakePhoto: () -> Unit = {},
    val onGeneratePhoto: () -> Unit = {},
    val onRemovePhoto: () -> Unit = {},
    val onSaveDraft: () -> Unit = {},
    val onPublish: () -> Unit = {},
    val onDismissNotice: () -> Unit = {},
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrearRecetaScreen(
    uiState: CrearRecetaUiState,
    actions: CrearRecetaActions,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.testTag(CrearRecetaTestTags.SCREEN),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.crear_receta_title)) },
                navigationIcon = {
                    TextButton(
                        onClick = actions.onBack,
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
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = WarmSpacing.md)
                .padding(bottom = WarmSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(WarmSpacing.md),
        ) {
            uiState.notice?.let { notice ->
                NoticeBanner(notice = notice, onDismiss = actions.onDismissNotice)
            }
            if (uiState.publishErrors.isNotEmpty()) {
                PublishErrors(errors = uiState.publishErrors)
            }
            PhotoSection(uiState = uiState, actions = actions)
            BasicInfoSection(uiState = uiState, actions = actions)
            MetricsSection(uiState = uiState, actions = actions)
            IngredientsSection(uiState = uiState, actions = actions)
            StepsSection(uiState = uiState, actions = actions)
            FavoriteSection(uiState = uiState, actions = actions)
            ActionDock(actions = actions)
        }
    }
}

@Composable
private fun NoticeBanner(notice: CrearRecetaNotice, onDismiss: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().testTag(CrearRecetaTestTags.NOTICE)) {
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

@Composable
private fun PublishErrors(errors: Set<RecipeRequirement>) {
    Column(modifier = Modifier.fillMaxWidth().testTag(CrearRecetaTestTags.PUBLISH_ERRORS)) {
        Text(
            text = stringResource(R.string.crear_receta_publish_blocked),
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

@Composable
private fun PhotoSection(uiState: CrearRecetaUiState, actions: CrearRecetaActions) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(WarmSpacing.md),
            verticalArrangement = Arrangement.spacedBy(WarmSpacing.sm),
        ) {
            Text(
                text = stringResource(R.string.crear_receta_photo_section),
                style = MaterialTheme.typography.headlineSmall,
            )
            val bitmap = rememberBitmap(uiState.photoPath)
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = stringResource(R.string.crear_receta_photo_preview),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 160.dp)
                        .clip(RoundedCornerShape(WarmSpacing.sm))
                        .testTag(CrearRecetaTestTags.PHOTO_PREVIEW),
                )
            } else {
                Text(
                    text = stringResource(
                        if (uiState.isPhotoLoading) {
                            R.string.crear_receta_photo_loading
                        } else {
                            R.string.crear_receta_photo_none
                        },
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(WarmSpacing.sm),
            ) {
                FilledTonalButton(
                    onClick = actions.onPickPhotoFromGallery,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag(CrearRecetaTestTags.PHOTO_GALLERY),
                ) {
                    Text(text = stringResource(R.string.crear_receta_photo_gallery))
                }
                FilledTonalButton(
                    onClick = actions.onTakePhoto,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag(CrearRecetaTestTags.PHOTO_CAMERA),
                ) {
                    Text(text = stringResource(R.string.crear_receta_photo_camera))
                }
            }
            Button(
                onClick = actions.onGeneratePhoto,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag(CrearRecetaTestTags.PHOTO_GENERATE),
            ) {
                Text(text = stringResource(R.string.crear_receta_photo_generate))
            }
            if (uiState.photoPath != null) {
                TextButton(
                    onClick = actions.onRemovePhoto,
                    modifier = Modifier.heightIn(min = 48.dp).testTag(CrearRecetaTestTags.PHOTO_REMOVE),
                ) {
                    Text(text = stringResource(R.string.crear_receta_photo_remove))
                }
            }
        }
    }
}

@Composable
private fun BasicInfoSection(uiState: CrearRecetaUiState, actions: CrearRecetaActions) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(WarmSpacing.md),
            verticalArrangement = Arrangement.spacedBy(WarmSpacing.sm),
        ) {
            Text(
                text = stringResource(R.string.crear_receta_basic_section),
                style = MaterialTheme.typography.headlineSmall,
            )
            OutlinedTextField(
                value = uiState.title,
                onValueChange = actions.onTitleChange,
                label = { Text(text = stringResource(R.string.crear_receta_title_label)) },
                isError = uiState.titleError,
                supportingText = if (uiState.titleError) {
                    { Text(text = stringResource(R.string.crear_receta_title_required)) }
                } else {
                    null
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag(CrearRecetaTestTags.TITLE_FIELD),
            )
            OutlinedTextField(
                value = uiState.story,
                onValueChange = actions.onStoryChange,
                label = { Text(text = stringResource(R.string.crear_receta_story_label)) },
                minLines = 2,
                modifier = Modifier.fillMaxWidth().testTag(CrearRecetaTestTags.STORY_FIELD),
            )
            Text(
                text = stringResource(R.string.crear_receta_moment_label),
                style = MaterialTheme.typography.labelLarge,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(WarmSpacing.sm),
            ) {
                MealMoment.entries.forEach { moment ->
                    FilterChip(
                        selected = uiState.mealMoment == moment,
                        onClick = { actions.onMealMomentChange(moment) },
                        label = { Text(text = stringResource(moment.labelRes())) },
                        modifier = Modifier.heightIn(min = 48.dp).testTag(moment.testTag()),
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricsSection(uiState: CrearRecetaUiState, actions: CrearRecetaActions) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(WarmSpacing.md),
            verticalArrangement = Arrangement.spacedBy(WarmSpacing.sm),
        ) {
            Text(
                text = stringResource(R.string.crear_receta_metrics_section),
                style = MaterialTheme.typography.headlineSmall,
            )
            OutlinedTextField(
                value = uiState.prepMinutes,
                onValueChange = actions.onPrepMinutesChange,
                label = { Text(text = stringResource(R.string.crear_receta_prep_label)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth().testTag(CrearRecetaTestTags.PREP_MINUTES_FIELD),
            )
            OutlinedTextField(
                value = uiState.cookMinutes,
                onValueChange = actions.onCookMinutesChange,
                label = { Text(text = stringResource(R.string.crear_receta_cook_label)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth().testTag(CrearRecetaTestTags.COOK_MINUTES_FIELD),
            )
            Text(
                text = stringResource(R.string.crear_receta_difficulty_label),
                style = MaterialTheme.typography.labelLarge,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(WarmSpacing.sm)) {
                Difficulty.entries.forEach { difficulty ->
                    FilterChip(
                        selected = uiState.difficulty == difficulty,
                        onClick = { actions.onDifficultyChange(difficulty) },
                        label = { Text(text = stringResource(difficulty.labelRes())) },
                        modifier = Modifier.heightIn(min = 48.dp).testTag(difficulty.testTag()),
                    )
                }
            }
            Text(
                text = stringResource(R.string.crear_receta_servings_label),
                style = MaterialTheme.typography.labelLarge,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(
                    onClick = {
                        actions.onServingsChange(uiState.baseServings - 1)
                    },
                    modifier = Modifier.heightIn(min = 48.dp).testTag(CrearRecetaTestTags.SERVINGS_DECREASE),
                ) {
                    Text(text = stringResource(R.string.crear_receta_servings_decrease))
                }
                Text(
                    text = uiState.baseServings.toString(),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(horizontal = WarmSpacing.md).testTag(CrearRecetaTestTags.SERVINGS_VALUE),
                )
                TextButton(
                    onClick = {
                        actions.onServingsChange(uiState.baseServings + 1)
                    },
                    modifier = Modifier.heightIn(min = 48.dp).testTag(CrearRecetaTestTags.SERVINGS_INCREASE),
                ) {
                    Text(text = stringResource(R.string.crear_receta_servings_increase))
                }
            }
            OutlinedTextField(
                value = uiState.caloriesPerServing,
                onValueChange = actions.onCaloriesChange,
                label = { Text(text = stringResource(R.string.crear_receta_calories_label)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth().testTag(CrearRecetaTestTags.CALORIES_FIELD),
            )
        }
    }
}

@Composable
private fun IngredientsSection(uiState: CrearRecetaUiState, actions: CrearRecetaActions) {
    val showRowError = RecipeRequirement.INVALID_INGREDIENT in uiState.publishErrors
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(WarmSpacing.md),
            verticalArrangement = Arrangement.spacedBy(WarmSpacing.sm),
        ) {
            Text(
                text = stringResource(R.string.crear_receta_ingredients_title),
                style = MaterialTheme.typography.headlineSmall,
            )
            uiState.ingredients.forEachIndexed { index, row ->
                IngredientRow(
                    row = row,
                    index = index,
                    showError = showRowError && !row.isValid,
                    actions = actions,
                )
            }
            FilledTonalButton(
                onClick = actions.onAddIngredient,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag(CrearRecetaTestTags.ADD_INGREDIENT),
            ) {
                Text(text = stringResource(R.string.crear_receta_add_ingredient))
            }
        }
    }
}

@Composable
private fun IngredientRow(
    row: IngredientFieldUiState,
    index: Int,
    showError: Boolean,
    actions: CrearRecetaActions,
) {
    Card {
        Column(
            modifier = Modifier.padding(WarmSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(WarmSpacing.xs),
        ) {
            OutlinedTextField(
                value = row.name,
                onValueChange = { actions.onIngredientNameChange(row.key, it) },
                label = { Text(text = stringResource(R.string.crear_receta_ingredient_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag(CrearRecetaTestTags.ingredientName(index)),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(WarmSpacing.sm)) {
                OutlinedTextField(
                    value = row.quantity,
                    onValueChange = { actions.onIngredientQuantityChange(row.key, it) },
                    label = { Text(text = stringResource(R.string.crear_receta_ingredient_quantity)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f).testTag(CrearRecetaTestTags.ingredientQuantity(index)),
                )
                OutlinedTextField(
                    value = row.unit,
                    onValueChange = { actions.onIngredientUnitChange(row.key, it) },
                    label = { Text(text = stringResource(R.string.crear_receta_ingredient_unit)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f).testTag(CrearRecetaTestTags.ingredientUnit(index)),
                )
            }
            if (showError) {
                Text(
                    text = stringResource(R.string.crear_receta_ingredient_invalid),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = row.optional,
                    onCheckedChange = { actions.onIngredientOptionalChange(row.key, it) },
                    modifier = Modifier.testTag(CrearRecetaTestTags.ingredientOptional(index)),
                )
                Text(
                    text = stringResource(R.string.crear_receta_ingredient_optional),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.weight(1f))
                TextButton(
                    onClick = { actions.onRemoveIngredient(row.key) },
                    modifier = Modifier.heightIn(min = 48.dp).testTag(CrearRecetaTestTags.removeIngredient(index)),
                ) {
                    Text(text = stringResource(R.string.crear_receta_remove))
                }
            }
        }
    }
}

@Composable
private fun StepsSection(uiState: CrearRecetaUiState, actions: CrearRecetaActions) {
    val showRowError = RecipeRequirement.INVALID_STEP in uiState.publishErrors
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(WarmSpacing.md),
            verticalArrangement = Arrangement.spacedBy(WarmSpacing.sm),
        ) {
            Text(
                text = stringResource(R.string.crear_receta_steps_title),
                style = MaterialTheme.typography.headlineSmall,
            )
            uiState.steps.forEachIndexed { index, row ->
                StepRow(
                    row = row,
                    index = index,
                    showError = showRowError && !row.isValid,
                    actions = actions,
                )
            }
            FilledTonalButton(
                onClick = actions.onAddStep,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag(CrearRecetaTestTags.ADD_STEP),
            ) {
                Text(text = stringResource(R.string.crear_receta_add_step))
            }
        }
    }
}

@Composable
private fun StepRow(
    row: StepFieldUiState,
    index: Int,
    showError: Boolean,
    actions: CrearRecetaActions,
) {
    Card {
        Column(
            modifier = Modifier.padding(WarmSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(WarmSpacing.xs),
        ) {
            Text(
                text = stringResource(R.string.crear_receta_step_number, index + 1),
                style = MaterialTheme.typography.labelLarge,
            )
            OutlinedTextField(
                value = row.text,
                onValueChange = { actions.onStepTextChange(row.key, it) },
                label = { Text(text = stringResource(R.string.crear_receta_step_text)) },
                minLines = 2,
                modifier = Modifier.fillMaxWidth().testTag(CrearRecetaTestTags.stepText(index)),
            )
            OutlinedTextField(
                value = row.timerMinutes,
                onValueChange = { actions.onStepTimerChange(row.key, it) },
                label = { Text(text = stringResource(R.string.crear_receta_step_timer)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth().testTag(CrearRecetaTestTags.stepTimer(index)),
            )
            if (showError) {
                Text(
                    text = stringResource(R.string.crear_receta_step_invalid),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            TextButton(
                onClick = { actions.onRemoveStep(row.key) },
                modifier = Modifier.heightIn(min = 48.dp).testTag(CrearRecetaTestTags.removeStep(index)),
            ) {
                Text(text = stringResource(R.string.crear_receta_remove))
            }
        }
    }
}

@Composable
private fun FavoriteSection(uiState: CrearRecetaUiState, actions: CrearRecetaActions) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(WarmSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.crear_receta_favorite),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = uiState.favorite,
                onCheckedChange = { actions.onFavoriteToggle() },
                modifier = Modifier.testTag(CrearRecetaTestTags.FAVORITE_TOGGLE),
            )
        }
    }
}

@Composable
private fun ActionDock(actions: CrearRecetaActions) {
    Column(verticalArrangement = Arrangement.spacedBy(WarmSpacing.sm)) {
        FilledTonalButton(
            onClick = actions.onSaveDraft,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag(CrearRecetaTestTags.SAVE_DRAFT),
        ) {
            Text(text = stringResource(R.string.crear_receta_save_draft))
        }
        Button(
            onClick = actions.onPublish,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag(CrearRecetaTestTags.PUBLISH),
        ) {
            Text(text = stringResource(R.string.crear_receta_publish))
        }
    }
}

@Composable
private fun rememberBitmap(path: String?): ImageBitmap? = remember(path) {
    path?.let { file ->
        runCatching { BitmapFactory.decodeFile(file)?.asImageBitmap() }.getOrNull()
    }
}

private fun MealMoment.labelRes(): Int = when (this) {
    MealMoment.BREAKFAST -> R.string.momento_desayuno
    MealMoment.LUNCH -> R.string.momento_almuerzo
    MealMoment.DINNER -> R.string.momento_cena
    MealMoment.DESSERT -> R.string.momento_postre
}

private fun MealMoment.testTag(): String = when (this) {
    MealMoment.BREAKFAST -> CrearRecetaTestTags.MOMENT_BREAKFAST
    MealMoment.LUNCH -> CrearRecetaTestTags.MOMENT_LUNCH
    MealMoment.DINNER -> CrearRecetaTestTags.MOMENT_DINNER
    MealMoment.DESSERT -> CrearRecetaTestTags.MOMENT_DESSERT
}

private fun Difficulty.labelRes(): Int = when (this) {
    Difficulty.EASY -> R.string.dificultad_facil
    Difficulty.MEDIUM -> R.string.dificultad_media
    Difficulty.HARD -> R.string.dificultad_dificil
}

private fun Difficulty.testTag(): String = when (this) {
    Difficulty.EASY -> CrearRecetaTestTags.DIFFICULTY_EASY
    Difficulty.MEDIUM -> CrearRecetaTestTags.DIFFICULTY_MEDIUM
    Difficulty.HARD -> CrearRecetaTestTags.DIFFICULTY_HARD
}

private fun CrearRecetaNotice.messageRes(): Int = when (this) {
    CrearRecetaNotice.AI_KEY_MISSING -> R.string.crear_receta_notice_ai_key_missing
    CrearRecetaNotice.AI_GENERATION_FAILED -> R.string.crear_receta_notice_ai_failed
    CrearRecetaNotice.CAMERA_PERMISSION_DENIED -> R.string.crear_receta_notice_camera_denied
}

private fun RecipeRequirement.messageRes(): Int = when (this) {
    RecipeRequirement.TITLE -> R.string.crear_receta_publish_error_title
    RecipeRequirement.PHOTO -> R.string.crear_receta_publish_error_photo
    RecipeRequirement.INGREDIENT -> R.string.crear_receta_publish_error_ingredient
    RecipeRequirement.STEP -> R.string.crear_receta_publish_error_step
    RecipeRequirement.INVALID_INGREDIENT -> R.string.crear_receta_publish_error_invalid_ingredient
    RecipeRequirement.INVALID_STEP -> R.string.crear_receta_publish_error_invalid_step
}
