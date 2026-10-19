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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Equalizer
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MisRecetasScreen(
    uiState: MisRecetasUiState,
    onCreateRecipe: () -> Unit,
    onScanPantry: () -> Unit,
    onEditRecipe: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.testTag(MisRecetasTestTags.SCREEN),
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
                recipes = uiState.recipes,
                onCreateRecipe = onCreateRecipe,
                onEditRecipe = onEditRecipe,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        }
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
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .padding(horizontal = WarmSpacing.md)
            .testTag(MisRecetasTestTags.RECIPE_LIST),
        verticalArrangement = Arrangement.spacedBy(WarmSpacing.md),
    ) {
        items(items = recipes, key = { it.id }) { recipe ->
            RecipeCard(recipe = recipe, onEdit = { onEditRecipe(recipe.id) })
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

// RF-17 a RF-20 y RF-22: tarjeta con portada y badge de momento superpuesto (o
// junto al título sin foto), título, metadatos con iconos, píldora de estado y el
// pie de acciones sobre surface-container-lowest con esquinas de 0.75rem y sombra
// cálida. RF-4: el pie solo dibuja «Editar receta» (Q-editar-receta-4).
private val CardShape: Shape = RoundedCornerShape(WarmRadius.md)
private val PillShape: Shape = RoundedCornerShape(50)

@Composable
private fun RecipeCard(recipe: Recipe, onEdit: () -> Unit) {
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
                CardActions(onEdit = onEdit)
            }
        }
    }
}

// RF-4: pie de acciones de la tarjeta con «Editar receta» en la posición izquierda
// del prototipo. «Eliminar receta» y «Cocinar Ahora» quedan fuera de esta feature
// (Q-editar-receta-4).
@Composable
private fun CardActions(onEdit: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = WarmSpacing.xs),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EditRecipeButton(onClick = onEdit)
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
