package com.whatsforlunch.app.ui.myrecipes

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BakeryDining
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Equalizer
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Icecream
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.RamenDining
import androidx.compose.material.icons.outlined.RestaurantMenu
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.foundation.selection.selectable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.whatsforlunch.app.R
import com.whatsforlunch.app.domain.myrecipes.MyRecipesState
import com.whatsforlunch.app.domain.match.RecipeMatch
import com.whatsforlunch.app.domain.model.Difficulty
import com.whatsforlunch.app.domain.model.MealMoment
import com.whatsforlunch.app.domain.model.Recipe
import com.whatsforlunch.app.domain.model.RecipeStatus
import com.whatsforlunch.app.ui.components.MatchBadge
import com.whatsforlunch.app.ui.components.MomentBadge
import com.whatsforlunch.app.ui.components.SkilletIcon
import com.whatsforlunch.app.ui.theme.WarmRadius
import com.whatsforlunch.app.ui.theme.WarmSpacing
import com.whatsforlunch.app.ui.theme.warmShadow

// Identificadores estables para las pruebas instrumentadas (E2E).
object MyRecipesTestTags {
    const val SCREEN = "my_recipes_screen"
    const val EMPTY_TITLE = "my_recipes_empty_title"
    const val EMPTY_MESSAGE = "my_recipes_empty_message"
    const val CREATE_RECIPE = "action_create_recipe"
    const val SCAN_PANTRY = "action_scan_pantry"
    const val RECIPE_LIST = "my_recipes_list"
    const val RECIPE_CARD = "my_recipes_card"
    const val RECIPE_BODY = "my_recipes_card_body"
    const val RECIPE_STATUS = "my_recipes_card_status"
    const val RECIPE_MOMENT = "my_recipes_card_moment"
    const val RECIPE_MATCH = "my_recipes_card_match"
    const val RECIPE_PHOTO = "my_recipes_card_photo"
    const val RECIPE_TITLE = "my_recipes_card_title"
    const val RECIPE_TIME = "my_recipes_card_time"
    const val RECIPE_DIFFICULTY = "my_recipes_card_difficulty"
    const val RECIPE_SERVINGS = "my_recipes_card_servings"
    const val RECIPE_EDIT = "my_recipes_card_edit"
    const val RECIPE_DELETE = "my_recipes_card_delete"
    const val SEARCH_FIELD = "my_recipes_search_field"
    const val SEARCH_ICON = "my_recipes_search_icon"
    const val FILTER_ROW = "my_recipes_moment_filter"
    const val FILTER_CHIP_ALL = "my_recipes_filter_all"
    const val FILTER_CHIP_ALL_ICON = "my_recipes_filter_all_icon"
    const val FILTER_CHIP = "my_recipes_filter"
    const val FILTER_CHIP_ICON = "my_recipes_filter_icon"
    const val NO_RESULTS = "my_recipes_no_results"
    const val NO_RESULTS_CARD = "my_recipes_no_results_card"
    const val NO_RESULTS_ICON = "my_recipes_no_results_icon"
    const val NO_RESULTS_ICON_CONTAINER = "my_recipes_no_results_icon_container"
    const val RESET_FILTERS = "my_recipes_reset_filters"
    const val DELETE_DIALOG = "my_recipes_delete_dialog"
    const val DELETE_CONFIRM = "my_recipes_delete_confirm"
    const val DELETE_CANCEL = "my_recipes_delete_cancel"
    const val OPEN_PANTRY = "action_open_pantry"
    const val OPEN_SETTINGS = "action_open_settings"

    fun filterChip(moment: MealMoment): String = "${FILTER_CHIP}_${moment.name.lowercase()}"
    fun filterChipIcon(moment: MealMoment): String = "${FILTER_CHIP_ICON}_${moment.name.lowercase()}"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyRecipesScreen(
    uiState: MyRecipesUiState,
    onCreateRecipe: () -> Unit,
    onScanPantry: () -> Unit,
    onEditRecipe: (String) -> Unit = {},
    onOpenRecipe: (String) -> Unit = {},
    onOpenPantry: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onSearchQueryChanged: (String) -> Unit = {},
    onMealMomentSelected: (MealMoment?) -> Unit = {},
    onResetFilters: () -> Unit = {},
    onDeleteRecipe: (String) -> Unit = {},
    onDeleteErrorConsumed: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var recipeToDelete by remember { mutableStateOf<Recipe?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val deleteErrorMessage = stringResource(R.string.my_recipes_delete_error)

    LaunchedEffect(uiState.deleteError) {
        if (uiState.deleteError) {
            snackbarHostState.showSnackbar(deleteErrorMessage)
            onDeleteErrorConsumed()
        }
    }

    Scaffold(
        modifier = modifier.testTag(MyRecipesTestTags.SCREEN),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.my_recipes_title)) },
                actions = {
                    IconButton(
                        onClick = onOpenPantry,
                        modifier = Modifier.testTag(MyRecipesTestTags.OPEN_PANTRY),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Inventory2,
                            contentDescription = stringResource(R.string.pantry_open_accessibility),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.testTag(MyRecipesTestTags.OPEN_SETTINGS),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = stringResource(R.string.settings_open_accessibility),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
    ) { innerPadding ->
        when (uiState.state) {
            MyRecipesState.Empty -> EmptyMyRecipes(
                onCreateRecipe = onCreateRecipe,
                onScanPantry = onScanPantry,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
            MyRecipesState.WithRecipes -> RecipeList(
                recipes = uiState.visibleRecipes,
                matches = uiState.matches,
                onCreateRecipe = onCreateRecipe,
                onEditRecipe = onEditRecipe,
                onOpenRecipe = onOpenRecipe,
                searchQuery = uiState.searchQuery,
                selectedMoment = uiState.mealMomentFilter,
                onSearchQueryChanged = onSearchQueryChanged,
                onMealMomentSelected = onMealMomentSelected,
                onDeleteRecipe = { recipeToDelete = it },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
            MyRecipesState.NoResults -> NoResults(
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
            modifier = Modifier.testTag(MyRecipesTestTags.DELETE_DIALOG),
            title = { Text(text = stringResource(R.string.my_recipes_delete_title)) },
            text = {
                Text(text = stringResource(R.string.my_recipes_delete_message, recipe.title))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        recipeToDelete = null
                        onDeleteRecipe(recipe.id)
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag(MyRecipesTestTags.DELETE_CONFIRM),
                ) {
                    Text(text = stringResource(R.string.my_recipes_delete_confirm))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { recipeToDelete = null },
                    modifier = Modifier.testTag(MyRecipesTestTags.DELETE_CANCEL),
                ) {
                    Text(text = stringResource(R.string.my_recipes_delete_cancel))
                }
            },
        )
    }
}

@Composable
private fun EmptyMyRecipes(
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
            text = stringResource(R.string.my_recipes_empty_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag(MyRecipesTestTags.EMPTY_TITLE),
        )
        Text(
            text = stringResource(R.string.my_recipes_empty_message),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(top = WarmSpacing.sm)
                .testTag(MyRecipesTestTags.EMPTY_MESSAGE),
        )
        Button(
            onClick = onCreateRecipe,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = WarmSpacing.xl)
                .heightIn(min = 52.dp)
                .testTag(MyRecipesTestTags.CREATE_RECIPE),
        ) {
            Text(text = stringResource(R.string.action_create_recipe))
        }
        FilledTonalButton(
            onClick = onScanPantry,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = WarmSpacing.md)
                .heightIn(min = 48.dp)
                .testTag(MyRecipesTestTags.SCAN_PANTRY),
        ) {
            Text(text = stringResource(R.string.action_scan_pantry))
        }
    }
}

@Composable
private fun RecipeList(
    recipes: List<Recipe>,
    matches: Map<String, RecipeMatch>,
    onCreateRecipe: () -> Unit,
    onEditRecipe: (String) -> Unit,
    onOpenRecipe: (String) -> Unit,
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
                .testTag(MyRecipesTestTags.RECIPE_LIST),
            verticalArrangement = Arrangement.spacedBy(WarmSpacing.md),
        ) {
            items(items = recipes, key = { it.id }) { recipe ->
                RecipeCard(
                    recipe = recipe,
                    match = matches[recipe.id],
                    onOpen = { onOpenRecipe(recipe.id) },
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
                        .testTag(MyRecipesTestTags.CREATE_RECIPE),
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
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
            .warmShadow(shape = RoundedCornerShape(WarmRadius.md)),
        shape = RoundedCornerShape(WarmRadius.md),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        androidx.compose.foundation.text.BasicTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChanged,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .testTag(MyRecipesTestTags.SEARCH_FIELD),
            textStyle = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onSurface,
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            singleLine = true,
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier.padding(
                        start = 12.dp,
                        end = 16.dp,
                        top = 12.dp,
                        bottom = 12.dp,
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(20.dp)
                            .testTag(MyRecipesTestTags.SEARCH_ICON),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = stringResource(R.string.my_recipes_search_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.60f),
                            )
                        }
                        innerTextField()
                    }
                }
            },
        )
    }
    val filterDescription = stringResource(R.string.my_recipes_filter_label)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = WarmSpacing.sm)
            .horizontalScroll(rememberScrollState())
            .semantics { contentDescription = filterDescription }
            .testTag(MyRecipesTestTags.FILTER_ROW),
        horizontalArrangement = Arrangement.spacedBy(WarmSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MomentFilterChip(
            label = stringResource(R.string.my_recipes_filter_all),
            icon = Icons.Outlined.RestaurantMenu,
            selected = selectedMoment == null,
            iconTint = MaterialTheme.colorScheme.primary,
            testTag = MyRecipesTestTags.FILTER_CHIP_ALL,
            iconTestTag = MyRecipesTestTags.FILTER_CHIP_ALL_ICON,
            onClick = { onMealMomentSelected(null) },
        )
        MealMoment.entries.forEach { moment ->
            MomentFilterChip(
                label = stringResource(moment.labelRes()),
                icon = moment.icon(),
                selected = selectedMoment == moment,
                iconTint = moment.inactiveTint(),
                testTag = MyRecipesTestTags.filterChip(moment),
                iconTestTag = MyRecipesTestTags.filterChipIcon(moment),
                onClick = { onMealMomentSelected(moment) },
            )
        }
    }
}

@Composable
private fun MomentFilterChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    iconTint: androidx.compose.ui.graphics.Color,
    testTag: String,
    iconTestTag: String,
    onClick: () -> Unit,
) {
    val shape = CircleShape
    Box(
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .testTag(testTag),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = shape,
            color = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.surfaceContainerLowest,
            modifier = Modifier.warmShadow(shape = shape),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (selected) MaterialTheme.colorScheme.onPrimary else iconTint,
                    modifier = Modifier
                        .size(18.dp)
                        .testTag(iconTestTag),
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (selected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

private fun MealMoment.icon() = when (this) {
    MealMoment.BREAKFAST -> Icons.Outlined.BakeryDining
    MealMoment.LUNCH -> SkilletIcon
    MealMoment.DINNER -> Icons.Outlined.RamenDining
    MealMoment.DESSERT -> Icons.Outlined.Icecream
}

@Composable
private fun MealMoment.inactiveTint() = when (this) {
    MealMoment.BREAKFAST,
    MealMoment.LUNCH,
    MealMoment.DINNER,
    MealMoment.DESSERT -> MaterialTheme.colorScheme.primary
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoResults(
    searchQuery: String,
    selectedMoment: MealMoment?,
    onSearchQueryChanged: (String) -> Unit,
    onMealMomentSelected: (MealMoment?) -> Unit,
    onResetFilters: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cardShape = RoundedCornerShape(WarmRadius.md)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = WarmSpacing.md)
            .testTag(MyRecipesTestTags.NO_RESULTS),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SearchAndFilterControls(
            searchQuery = searchQuery,
            selectedMoment = selectedMoment,
            onSearchQueryChanged = onSearchQueryChanged,
            onMealMomentSelected = onMealMomentSelected,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .warmShadow(shape = cardShape)
                    .testTag(MyRecipesTestTags.NO_RESULTS_CARD),
                shape = cardShape,
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(WarmSpacing.xl),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(WarmSpacing.sm),
                ) {
                    Surface(
                        modifier = Modifier
                            .size(56.dp)
                            .testTag(MyRecipesTestTags.NO_RESULTS_ICON_CONTAINER),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainer,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.SearchOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .size(28.dp)
                                    .testTag(MyRecipesTestTags.NO_RESULTS_ICON),
                            )
                        }
                    }
                    Text(
                        text = stringResource(R.string.my_recipes_no_results_title),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = stringResource(R.string.my_recipes_no_results_message),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.widthIn(max = 320.dp),
                    )
                    Box(
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .clickable(role = Role.Button, onClick = onResetFilters)
                            .testTag(MyRecipesTestTags.RESET_FILTERS),
                        contentAlignment = Alignment.Center,
                    ) {
                        Surface(
                            modifier = Modifier.height(36.dp),
                            shape = cardShape,
                            color = MaterialTheme.colorScheme.secondary,
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = WarmSpacing.md),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = stringResource(R.string.my_recipes_reset_filters),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSecondary,
                                )
                            }
                        }
                    }
                }
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
private fun RecipeCard(
    recipe: Recipe,
    match: RecipeMatch?,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        shape = CardShape,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = Modifier
            .fillMaxWidth()
            .warmShadow(shape = CardShape)
            .testTag(MyRecipesTestTags.RECIPE_CARD),
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
                            .testTag(MyRecipesTestTags.RECIPE_PHOTO),
                    )
                    MomentBadge(
                        mealMoment = recipe.mealMoment,
                        testTag = MyRecipesTestTags.RECIPE_MOMENT,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(WarmSpacing.md),
                    )
                    match?.let {
                        MatchBadge(
                            match = it,
                            testTag = MyRecipesTestTags.RECIPE_MATCH,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(WarmSpacing.md),
                        )
                    }
                }
            }
            // RF-DR1 (D1): el cuerpo de la tarjeta abre el Detalle. La capa clicable
            // va detrás del contenido (patrón «background clickable») para no fusionar
            // la semántica de los hijos ni romper «Editar»/«Eliminar», que quedan
            // fuera de esta zona con sus propios testTags.
            Box(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable(role = Role.Button, onClick = onOpen)
                        .testTag(MyRecipesTestTags.RECIPE_BODY),
                )
                Column(
                    modifier = Modifier.padding(WarmSpacing.md),
                    verticalArrangement = Arrangement.spacedBy(WarmSpacing.sm),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (bitmap == null) {
                            // RF-22: sin foto, el badge del momento va junto al título.
                            MomentBadge(
                                mealMoment = recipe.mealMoment,
                                testTag = MyRecipesTestTags.RECIPE_MOMENT,
                            )
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
                                .testTag(MyRecipesTestTags.RECIPE_TITLE),
                        )
                        Spacer(modifier = Modifier.width(WarmSpacing.sm))
                        if (bitmap == null) {
                            match?.let {
                                MatchBadge(match = it, testTag = MyRecipesTestTags.RECIPE_MATCH)
                            }
                        }
                        if (match != null) Spacer(modifier = Modifier.width(WarmSpacing.sm))
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
                }
            }
            CardActions(
                onEdit = onEdit,
                onDelete = onDelete,
                modifier = Modifier.padding(
                    start = WarmSpacing.md,
                    end = WarmSpacing.md,
                    top = WarmSpacing.xs,
                    bottom = WarmSpacing.md,
                ),
            )
        }
    }
}

// Ambas acciones del pie de tarjeta usan botones primarios compactos.
@Composable
private fun CardActions(onEdit: () -> Unit, onDelete: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
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
    val label = stringResource(R.string.my_recipes_delete_action)
    Button(
        onClick = onClick,
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .heightIn(min = 36.dp)
            .semantics { contentDescription = label }
            .testTag(MyRecipesTestTags.RECIPE_DELETE),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.Delete,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(WarmSpacing.xs))
        Text(
            text = stringResource(R.string.my_recipes_delete_short),
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
    val label = stringResource(R.string.my_recipes_action_edit)
    Button(
        onClick = onClick,
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .heightIn(min = 36.dp)
            .semantics { contentDescription = label }
            .testTag(MyRecipesTestTags.RECIPE_EDIT),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.Edit,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(WarmSpacing.xs))
        Text(
            text = stringResource(R.string.my_recipes_action_edit_short),
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
                R.string.my_recipes_card_time,
                recipe.prepMinutes + recipe.cookMinutes,
            ),
            testTag = MyRecipesTestTags.RECIPE_TIME,
        )
        MetadataItem(
            icon = Icons.Outlined.Equalizer,
            iconTint = MaterialTheme.colorScheme.secondary,
            text = stringResource(recipe.difficulty.labelRes()),
            testTag = MyRecipesTestTags.RECIPE_DIFFICULTY,
        )
        MetadataItem(
            icon = Icons.Outlined.Group,
            iconTint = MaterialTheme.colorScheme.primary,
            text = stringResource(R.string.my_recipes_card_servings, recipe.baseServings),
            testTag = MyRecipesTestTags.RECIPE_SERVINGS,
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

// Badge de momento culinario en píldora (superpuesto a la portada o junto al título):
// se muestra con el componente compartido MomentBadge de ui/components.

// RF-19: distintivo de estado (Borrador/Publicada) como píldora.
@Composable
private fun StatusPill(status: RecipeStatus) {
    Surface(
        shape = PillShape,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
        contentColor = MaterialTheme.colorScheme.primary,
        modifier = Modifier.testTag(MyRecipesTestTags.RECIPE_STATUS),
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
    MealMoment.BREAKFAST -> R.string.meal_moment_breakfast
    MealMoment.LUNCH -> R.string.meal_moment_lunch
    MealMoment.DINNER -> R.string.meal_moment_dinner
    MealMoment.DESSERT -> R.string.meal_moment_dessert
}

private fun Difficulty.labelRes(): Int = when (this) {
    Difficulty.EASY -> R.string.difficulty_easy
    Difficulty.MEDIUM -> R.string.difficulty_medium
    Difficulty.HARD -> R.string.difficulty_hard
}

private fun RecipeStatus.labelRes(): Int = when (this) {
    RecipeStatus.DRAFT -> R.string.my_recipes_status_draft
    RecipeStatus.PUBLISHED -> R.string.my_recipes_status_published
}
