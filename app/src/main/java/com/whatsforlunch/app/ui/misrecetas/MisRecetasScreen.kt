package com.whatsforlunch.app.ui.misrecetas

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.whatsforlunch.app.R
import com.whatsforlunch.app.domain.misrecetas.MisRecetasState
import com.whatsforlunch.app.domain.model.MealMoment
import com.whatsforlunch.app.domain.model.Recipe
import com.whatsforlunch.app.domain.model.RecipeStatus
import com.whatsforlunch.app.ui.theme.WarmSpacing

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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MisRecetasScreen(
    uiState: MisRecetasUiState,
    onCreateRecipe: () -> Unit,
    onScanPantry: () -> Unit,
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
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .padding(horizontal = WarmSpacing.md)
            .testTag(MisRecetasTestTags.RECIPE_LIST),
        verticalArrangement = Arrangement.spacedBy(WarmSpacing.md),
    ) {
        items(items = recipes, key = { it.id }) { recipe ->
            RecipeCard(recipe = recipe)
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

@Composable
private fun RecipeCard(recipe: Recipe) {
    Card(modifier = Modifier.fillMaxWidth().testTag(MisRecetasTestTags.RECIPE_CARD)) {
        Column(modifier = Modifier.padding(WarmSpacing.md)) {
            val bitmap = rememberBitmap(recipe.photoPath)
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = recipe.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 140.dp)
                        .clip(RoundedCornerShape(WarmSpacing.sm)),
                )
            }
            Text(
                text = recipe.title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = WarmSpacing.xs),
            )
            Row(
                modifier = Modifier.padding(top = WarmSpacing.xs),
                horizontalArrangement = Arrangement.spacedBy(WarmSpacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(recipe.mealMoment.labelRes()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag(MisRecetasTestTags.RECIPE_MOMENT),
                )
                Text(
                    text = stringResource(recipe.status.labelRes()),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag(MisRecetasTestTags.RECIPE_STATUS),
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

private fun MealMoment.labelRes(): Int = when (this) {
    MealMoment.BREAKFAST -> R.string.momento_desayuno
    MealMoment.LUNCH -> R.string.momento_almuerzo
    MealMoment.DINNER -> R.string.momento_cena
    MealMoment.DESSERT -> R.string.momento_postre
}

private fun RecipeStatus.labelRes(): Int = when (this) {
    RecipeStatus.DRAFT -> R.string.mis_recetas_status_draft
    RecipeStatus.PUBLISHED -> R.string.mis_recetas_status_published
}
