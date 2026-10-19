package com.whatsforlunch.app.ui.misrecetas

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Equalizer
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.whatsforlunch.app.R
import com.whatsforlunch.app.domain.misrecetas.MisRecetasState
import com.whatsforlunch.app.domain.model.Difficulty
import com.whatsforlunch.app.domain.model.MealMoment
import com.whatsforlunch.app.domain.model.Recipe
import com.whatsforlunch.app.domain.model.RecipeStatus
import com.whatsforlunch.app.ui.theme.WarmRadius
import com.whatsforlunch.app.ui.theme.WarmSpacing
import com.whatsforlunch.app.ui.theme.warmShadow

// Identificadores estables para las pruebas instrumentadas (E2E).
object MisRecetasTestTags {
    const val SCREEN = "mis_recetas_screen"
    const val EMPTY_TITLE = "mis_recetas_empty_title"
    const val EMPTY_MESSAGE = "mis_recetas_empty_message"
    const val CREATE_RECIPE = "action_create_recipe"
    const val SCAN_PANTRY = "action_scan_pantry"
    const val RECIPE_LIST = "mis_recetas_list"
    const val RECIPE_CARD = "mis_recetas_card"
    const val RECIPE_STATUS = "mis_recetas_card_status"
    const val RECIPE_MOMENT = "mis_recetas_card_moment"
    const val RECIPE_PHOTO = "mis_recetas_card_photo"
    const val RECIPE_TITLE = "mis_recetas_card_title"
    const val RECIPE_TIME = "mis_recetas_card_time"
    const val RECIPE_DIFFICULTY = "mis_recetas_card_difficulty"
    const val RECIPE_SERVINGS = "mis_recetas_card_servings"
    const val RECIPE_EDIT = "mis_recetas_card_edit"
    const val RECIPE_DELETE = "mis_recetas_card_delete"
    const val SEARCH_FIELD = "mis_recetas_search_field"
    const val FILTER_ROW = "mis_recetas_moment_filter"
    const val FILTER_CHIP_ALL = "mis_recetas_filter_all"
    const val FILTER_CHIP = "mis_recetas_filter"
    const val NO_RESULTS = "mis_recetas_no_results"
    const val RESET_FILTERS = "mis_recetas_reset_filters"
    const val DELETE_DIALOG = "mis_recetas_delete_dialog"
    const val DELETE_CONFIRM = "mis_recetas_delete_confirm"
    const val DELETE_CANCEL = "mis_recetas_delete_cancel"

    fun filterChip(moment: MealMoment): String = "${FILTER_CHIP}_${moment.name.lowercase()}"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MisRecetasScreen(
    uiState: MisRecetasUiState,
    onCreateRecipe: () -> Unit,
    onScanPantry: () -> Unit,
    onEditRecipe: (String) -> Unit = {},
    onSearchQueryChanged: (String) -> Unit = {},
    onMealMomentSelected: (MealMoment?) -> Unit = {},
    onResetFilters: () -> Unit = {},
    onDeleteRecipe: (String) -> Unit = {},
    onDeleteErrorConsumed: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var recipeToDelete by remember { mutableStateOf<Recipe?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val deleteErrorMessage = stringResource(R.string.mis_recetas_delete_error)

    LaunchedEffect(uiState.deleteError) {
        if (uiState.deleteError) {
            snackbarHostState.showSnackbar(deleteErrorMessage)
            onDeleteErrorConsumed()
        }
    }

    Scaffold(
        modifier = modifier.testTag(MisRecetasTestTags.SCREEN),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.mis_recetas_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
    ) { innerPadding ->
        when (uiState.state) {
            MisRecetasState.Empty -> EmptyMisRecetas(
                onCreateRecipe = onCreateRecipe,
                onScanPantry = onScanPantry,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
            MisRecetasState.WithRecipes -> RecipeList(
                recipes = uiState.visibleRecipes,
                onCreateRecipe = onCreateRecipe,
                onEditRecipe = onEditRecipe,
                searchQuery = uiState.searchQuery,
                selectedMoment = uiState.mealMomentFilter,
                onSearchQueryChanged = onSearchQueryChanged,
                onMealMomentSelected = onMealMomentSelected,
                onDeleteRecipe = { recipeToDelete = it },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
            MisRecetasState.NoResults -> NoResults(
                searchQuery = uiState.searchQuery,
                selectedMoment = uiState.mealMomentFilter,
                onSearchQueryChanged = onSearchQueryChanged,
                onMealMomentSelected = onMealMomentSelected,
                onResetFilters = onResetFilters,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        }
    }

    recipeToDelete?.let { recipe ->
        AlertDialog(
            onDismissRequest = { recipeToDelete = null },
            modifier = Modifier.testTag(MisRecetasTestTags.DELETE_DIALOG),
            title = { Text(text = stringResource(R.string.mis_recetas_delete_title)) },
            text = {
                Text(text = stringResource(R.string.mis_recetas_delete_message, recipe.title))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        recipeToDelete = null
                        onDeleteRecipe(recipe.id)
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag(MisRecetasTestTags.DELETE_CONFIRM),
                ) {
                    Text(text = stringResource(R.string.mis_recetas_delete_confirm))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { recipeToDelete = null },
                    modifier = Modifier.testTag(MisRecetasTestTags.DELETE_CANCEL),
                ) {
                    Text(text = stringResource(R.string.mis_recetas_delete_cancel))
                }
            },
        )
    }
}

@Composable
private fun EmptyMisRecetas(
    onCreateRecipe: () -> Unit,
    onScanPantry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = WarmSpacing.lg),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.mis_recetas_empty_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag(MisRecetasTestTags.EMPTY_TITLE),
        )
        Text(
            text = stringResource(R.string.mis_recetas_empty_message),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(top = WarmSpacing.sm)
                .testTag(MisRecetasTestTags.EMPTY_MESSAGE),
        )
        Button(
            onClick = onCreateRecipe,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = WarmSpacing.xl)
                .heightIn(min = 52.dp)
                .testTag(MisRecetasTestTags.CREATE_RECIPE),
        ) {
            Text(text = stringResource(R.string.action_create_recipe))
        }
        FilledTonalButton(
            onClick = onScanPantry,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = WarmSpacing.md)
                .heightIn(min = 48.dp)
                .testTag(MisRecetasTestTags.SCAN_PANTRY),
        ) {
            Text(text = stringResource(R.string.action_scan_pantry))
        }
    }
}

@Composable
private fun RecipeList(
    recipes: List<Recipe>,
    onCreateRecipe: () -> Unit,
    onEditRecipe: (String) -> Unit,
    searchQuery: String,
    selectedMoment: MealMoment?,
    onSearchQueryChanged: (String) -> Unit,
    onMealMomentSelected: (MealMoment?) -> Unit,
    onDeleteRecipe: (Recipe) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(horizontal = WarmSpacing.md)) {
        SearchAndFilterControls(
            searchQuery = searchQuery,
            selectedMoment = selectedMoment,
            onSearchQueryChanged = onSearchQueryChanged,
            onMealMomentSelected = onMealMomentSelected,
        )
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag(MisRecetasTestTags.RECIPE_LIST),
            verticalArrangement = Arrangement.spacedBy(WarmSpacing.md),
        ) {
            items(items = recipes, key = { it.id }) { recipe ->
                RecipeCard(
                    recipe = recipe,
                    onEdit = { onEditRecipe(recipe.id) },
                    onDelete = { onDeleteRecipe(recipe) },
                )
            }
            item {
                Button(
                    onClick = onCreateRecipe,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .testTag(MisRecetasTestTags.CREATE_RECIPE),
                ) {
                    Text(text = stringResource(R.string.action_create_recipe))
                }
            }
        }
    }
}

@Composable
private fun SearchAndFilterControls(
    searchQuery: String,
    selectedMoment: MealMoment?,
    onSearchQueryChanged: (String) -> Unit,
    onMealMomentSelected: (MealMoment?) -> Unit,
) {
    OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchQueryChanged,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .testTag(MisRecetasTestTags.SEARCH_FIELD),
        placeholder = { Text(text = stringResource(R.string.mis_recetas_search_hint)) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
            )
        },
        singleLine = true,
    )
    val filterDescription = stringResource(R.string.mis_recetas_filter_label)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .semantics { contentDescription = filterDescription }
            .testTag(MisRecetasTestTags.FILTER_ROW),
        horizontalArrangement = Arrangement.spacedBy(WarmSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilterChip(
            selected = selectedMoment == null,
            onClick = { onMealMomentSelected(null) },
            label = { Text(text = stringResource(R.string.mis_recetas_filter_all)) },
            colors = momentFilterChipColors(),
            modifier = Modifier
                .heightIn(min = 48.dp)
                .testTag(MisRecetasTestTags.FILTER_CHIP_ALL),
        )
        MealMoment.entries.forEach { moment ->
            FilterChip(
                selected = selectedMoment == moment,
                onClick = { onMealMomentSelected(moment) },
                label = { Text(text = stringResource(moment.labelRes())) },
                colors = momentFilterChipColors(),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag(MisRecetasTestTags.filterChip(moment)),
            )
        }
    }
}

@Composable
private fun momentFilterChipColors() = FilterChipDefaults.filterChipColors(
    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    selectedContainerColor = MaterialTheme.colorScheme.primary,
    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
)

@Composable
private fun NoResults(
    searchQuery: String,
    selectedMoment: MealMoment?,
    onSearchQueryChanged: (String) -> Unit,
    onMealMomentSelected: (MealMoment?) -> Unit,
    onResetFilters: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = WarmSpacing.md)
            .testTag(MisRecetasTestTags.NO_RESULTS),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SearchAndFilterControls(
            searchQuery = searchQuery,
            selectedMoment = selectedMoment,
            onSearchQueryChanged = onSearchQueryChanged,
            onMealMomentSelected = onMealMomentSelected,
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = WarmSpacing.md),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Outlined.SearchOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(48.dp),
            )
            Text(
                text = stringResource(R.string.mis_recetas_no_results_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = WarmSpacing.md),
            )
            Text(
                text = stringResource(R.string.mis_recetas_no_results_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = WarmSpacing.xs),
            )
            Button(
                onClick = onResetFilters,
                modifier = Modifier
                    .padding(top = WarmSpacing.lg)
                    .heightIn(min = 48.dp)
                    .testTag(MisRecetasTestTags.RESET_FILTERS),
            ) {
                Text(text = stringResource(R.string.mis_recetas_reset_filters))
            }
        }
    }
}

// RF-17 a RF-20 y RF-22: tarjeta con portada y badge de momento superpuesto (o
// junto al título sin foto), título, metadatos con iconos, píldora de estado y el
// pie de acciones sobre surface-container-lowest con esquinas de 0.75rem y sombra cálida.
private val CardShape: Shape = RoundedCornerShape(WarmRadius.md)
private val PillShape: Shape = RoundedCornerShape(50)

@Composable
private fun RecipeCard(recipe: Recipe, onEdit: () -> Unit, onDelete: () -> Unit) {
    Surface(
        shape = CardShape,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = Modifier
            .fillMaxWidth()
            .warmShadow(shape = CardShape)
            .testTag(MisRecetasTestTags.RECIPE_CARD),
    ) {
        Column {
            val bitmap = rememberBitmap(recipe.photoPath)
            if (bitmap != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(176.dp),
                ) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = recipe.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag(MisRecetasTestTags.RECIPE_PHOTO),
                    )
                    MomentBadge(
                        mealMoment = recipe.mealMoment,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(WarmSpacing.md),
                    )
                }
            }
            Column(
                modifier = Modifier.padding(WarmSpacing.md),
                verticalArrangement = Arrangement.spacedBy(WarmSpacing.sm),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (bitmap == null) {
                        // RF-22: sin foto, el badge del momento va junto al título.
                        MomentBadge(mealMoment = recipe.mealMoment)
                        Spacer(modifier = Modifier.width(WarmSpacing.sm))
                    }
                    Text(
                        text = recipe.title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .testTag(MisRecetasTestTags.RECIPE_TITLE),
                    )
                    Spacer(modifier = Modifier.width(WarmSpacing.sm))
                    StatusPill(status = recipe.status)
                }
                if (recipe.story.isNotBlank()) {
                    Text(
                        text = recipe.story,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                MetadataRow(recipe = recipe)
                CardActions(onEdit = onEdit, onDelete = onDelete)
            }
        }
    }
}

// Ambas acciones del pie de tarjeta usan botones primarios compactos.
@Composable
private fun CardActions(onEdit: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = WarmSpacing.xs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EditRecipeButton(onClick = onEdit)
        DeleteRecipeButton(onClick = onDelete)
    }
}

// Q-eliminar-receta-4 y Q-editar-receta-6: botón primario compacto con la misma
// estructura que «Editar» —icono de 16 dp y etiqueta «Eliminar» en `labelMedium`—
// y altura visual de 36 dp; el área táctil la mantiene el mínimo interactivo de
// Material 3 (≥ 48 dp).
@Composable
private fun DeleteRecipeButton(onClick: () -> Unit) {
    val label = stringResource(R.string.mis_recetas_delete_action)
    Button(
        onClick = onClick,
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .heightIn(min = 36.dp)
            .semantics { contentDescription = label }
            .testTag(MisRecetasTestTags.RECIPE_DELETE),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.Delete,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(WarmSpacing.xs))
        Text(
            text = stringResource(R.string.mis_recetas_delete_short),
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

// RF-4 y excepción de diseño aprobada por el usuario (2026-10-19): la acción «Editar
// receta» del pie de la tarjeta es un botón primario compacto —M3 `Button` relleno con
// el color primario, como «Crear receta»— con icono `edit` de 16 dp y etiqueta «Editar»
// en `labelMedium`; nombre accesible «Editar receta». Reducido a petición del usuario
// (2026-10-19): altura visual 36 dp y ancho al contenido; el área táctil la mantiene el
// mínimo interactivo de Material 3 (≥ 48 dp).
@Composable
private fun EditRecipeButton(onClick: () -> Unit) {
    val label = stringResource(R.string.mis_recetas_action_edit)
    Button(
        onClick = onClick,
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .heightIn(min = 36.dp)
            .semantics { contentDescription = label }
            .testTag(MisRecetasTestTags.RECIPE_EDIT),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.Edit,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(WarmSpacing.xs))
        Text(
            text = stringResource(R.string.mis_recetas_action_edit_short),
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

// RF-18: fila de metadatos con el tiempo total (preparación + cocción), la
// dificultad y las raciones, cada uno con su icono. Siempre visible.
@Composable
private fun MetadataRow(recipe: Recipe) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(WarmSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MetadataItem(
            icon = Icons.Outlined.Schedule,
            iconTint = MaterialTheme.colorScheme.tertiary,
            text = stringResource(
                R.string.mis_recetas_card_time,
                recipe.prepMinutes + recipe.cookMinutes,
            ),
            testTag = MisRecetasTestTags.RECIPE_TIME,
        )
        MetadataItem(
            icon = Icons.Outlined.Equalizer,
            iconTint = MaterialTheme.colorScheme.secondary,
            text = stringResource(recipe.difficulty.labelRes()),
            testTag = MisRecetasTestTags.RECIPE_DIFFICULTY,
        )
        MetadataItem(
            icon = Icons.Outlined.Group,
            iconTint = MaterialTheme.colorScheme.primary,
            text = stringResource(R.string.mis_recetas_card_servings, recipe.baseServings),
            testTag = MisRecetasTestTags.RECIPE_SERVINGS,
        )
    }
}

@Composable
private fun MetadataItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: androidx.compose.ui.graphics.Color,
    text: String,
    testTag: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.testTag(testTag),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(WarmSpacing.xs))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// Badge de momento culinario en píldora (superpuesto a la portada o junto al título).
@Composable
private fun MomentBadge(mealMoment: MealMoment, modifier: Modifier = Modifier) {
    Surface(
        shape = PillShape,
        color = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.95f),
        contentColor = MaterialTheme.colorScheme.primary,
        modifier = modifier.testTag(MisRecetasTestTags.RECIPE_MOMENT),
    ) {
        Text(
            text = stringResource(mealMoment.labelRes()),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = WarmSpacing.sm, vertical = 2.dp),
        )
    }
}

// RF-19: distintivo de estado (Borrador/Publicada) como píldora.
@Composable
private fun StatusPill(status: RecipeStatus) {
    Surface(
        shape = PillShape,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
        contentColor = MaterialTheme.colorScheme.primary,
        modifier = Modifier.testTag(MisRecetasTestTags.RECIPE_STATUS),
    ) {
        Text(
            text = stringResource(status.labelRes()),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = WarmSpacing.sm, vertical = 2.dp),
        )
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

private fun Difficulty.labelRes(): Int = when (this) {
    Difficulty.EASY -> R.string.dificultad_facil
    Difficulty.MEDIUM -> R.string.dificultad_media
    Difficulty.HARD -> R.string.dificultad_dificil
}

private fun RecipeStatus.labelRes(): Int = when (this) {
    RecipeStatus.DRAFT -> R.string.mis_recetas_status_draft
    RecipeStatus.PUBLISHED -> R.string.mis_recetas_status_published
}
