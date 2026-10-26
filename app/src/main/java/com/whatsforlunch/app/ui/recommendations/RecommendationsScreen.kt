package com.whatsforlunch.app.ui.recommendations

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.whatsforlunch.app.R
import com.whatsforlunch.app.domain.match.CompatibleRecipe
import com.whatsforlunch.app.domain.model.RecipeIngredient
import com.whatsforlunch.app.ui.components.MatchBadge
import com.whatsforlunch.app.ui.components.SkilletIcon
import com.whatsforlunch.app.ui.theme.WarmRadius
import com.whatsforlunch.app.ui.theme.WarmSpacing

// Identificadores estables para las pruebas instrumentadas (E2E) de la pantalla
// «Recetas con mi Despensa» (RF-SDR1, RF-SDR5).
object RecommendationsTestTags {
    const val SCREEN = "recommendations_screen"
    const val BACK = "recommendations_back"
    const val TITLE = "recommendations_title"
    const val SECTION_TITLE = "recommendations_section_title"
    const val LIST = "recommendations_list"
    const val CARD_PREFIX = "recommendations_card"
    const val MATCH_PREFIX = "recommendations_match"
    const val INGREDIENTS_PREFIX = "recommendations_ingredients"
    const val MISSING_PREFIX = "recommendations_missing"
    const val ADD_MISSING_PREFIX = "recommendations_add_missing"
    const val EMPTY = "recommendations_empty"
    const val SUGGEST_AI = "recommendations_suggest_ai"

    fun card(id: String) = "${CARD_PREFIX}_$id"
    fun match(id: String) = "${MATCH_PREFIX}_$id"
    fun ingredients(id: String) = "${INGREDIENTS_PREFIX}_$id"
    fun missing(id: String) = "${MISSING_PREFIX}_$id"
    fun addMissing(id: String) = "${ADD_MISSING_PREFIX}_$id"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecommendationsScreen(
    uiState: RecommendationsUiState,
    onBack: () -> Unit,
    onOpenRecipe: (String) -> Unit,
    onSuggestWithAi: () -> Unit,
    modifier: Modifier = Modifier,
    onAddMissingToShoppingList: (String) -> Unit = {},
) {
    Scaffold(
        modifier = modifier.testTag(RecommendationsTestTags.SCREEN),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.recommendations_title),
                        modifier = Modifier.testTag(RecommendationsTestTags.TITLE),
                    )
                },
                navigationIcon = {
                    TextButton(
                        onClick = onBack,
                        modifier = Modifier.heightIn(min = 48.dp).testTag(RecommendationsTestTags.BACK),
                    ) {
                        Text(stringResource(R.string.action_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
        bottomBar = {
            // RF-SDR4: la sugerencia con IA siempre visible; solo se habilita con la
            // Despensa persistida no vacía (D2).
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .navigationBarsPadding()
                    .padding(horizontal = WarmSpacing.md, vertical = WarmSpacing.md),
            ) {
                Button(
                    onClick = onSuggestWithAi,
                    enabled = uiState.hasFoods,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
                        .testTag(RecommendationsTestTags.SUGGEST_AI),
                ) {
                    Text(stringResource(R.string.recommendations_suggest_ai))
                }
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding)
                .padding(horizontal = WarmSpacing.md)
                .testTag(RecommendationsTestTags.LIST),
            contentPadding = PaddingValues(vertical = WarmSpacing.md),
            verticalArrangement = Arrangement.spacedBy(WarmSpacing.md),
        ) {
            item(key = "section_title") {
                Text(
                    stringResource(R.string.recommendations_section_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.testTag(RecommendationsTestTags.SECTION_TITLE),
                )
            }
            if (uiState.compatibleRecipes.isEmpty()) {
                item(key = "empty") {
                    Text(
                        stringResource(R.string.recommendations_empty),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag(RecommendationsTestTags.EMPTY),
                    )
                }
            } else {
                items(uiState.compatibleRecipes, key = { it.recipe.id }) { compatible ->
                    CompatibleRecipeCard(
                        compatible = compatible,
                        missing = uiState.missingByRecipe[compatible.recipe.id].orEmpty(),
                        onOpen = { onOpenRecipe(compatible.recipe.id) },
                        onAddMissing = { onAddMissingToShoppingList(compatible.recipe.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun CompatibleRecipeCard(
    compatible: CompatibleRecipe,
    missing: List<RecipeIngredient>,
    onOpen: () -> Unit,
    onAddMissing: () -> Unit,
) {
    val recipe = compatible.recipe
    val match = compatible.match
    val shape = RoundedCornerShape(WarmRadius.lg)
    val accessibilityDescription = stringResource(
        R.string.recommendations_card_accessibility,
        recipe.title,
        match.percentage,
    )
    Surface(
        modifier = Modifier.fillMaxWidth().clip(shape),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        // Capa clicable de fondo (patrón de la sesión 48): conserva la semántica de
        // los hijos y hace que toda la tarjeta abra el Detalle (RF-SDR5).
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier.matchParentSize()
                    .clickable(role = Role.Button, onClick = onOpen)
                    .semantics { contentDescription = accessibilityDescription }
                    .testTag(RecommendationsTestTags.card(recipe.id)),
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(WarmSpacing.md),
                horizontalArrangement = Arrangement.spacedBy(WarmSpacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CompatibleRecipeThumbnail(recipe.photoPath)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(WarmSpacing.sm),
                ) {
                    MatchBadge(
                        match = match,
                        testTag = RecommendationsTestTags.match(recipe.id),
                    )
                    Text(
                        recipe.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        stringResource(
                            R.string.recommendations_ingredients,
                            match.availableRequired,
                            match.totalRequired,
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag(RecommendationsTestTags.ingredients(recipe.id)),
                    )
                    if (missing.isNotEmpty()) {
                        Text(
                            stringResource(
                                R.string.recommendations_missing,
                                missing.joinToString(", ") { it.name },
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.testTag(
                                RecommendationsTestTags.missing(recipe.id),
                            ),
                        )
                    }
                }
                if (missing.isNotEmpty()) {
                    IconButton(
                        onClick = onAddMissing,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag(RecommendationsTestTags.addMissing(recipe.id)),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AddShoppingCart,
                            contentDescription = stringResource(
                                R.string.recommendations_add_missing,
                                recipe.title,
                            ),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CompatibleRecipeThumbnail(photoPath: String?) {
    val bitmap = rememberBitmap(photoPath)
    val shape = RoundedCornerShape(WarmRadius.md)
    Box(
        modifier = Modifier.size(96.dp).clip(shape).background(MaterialTheme.colorScheme.surfaceContainer),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                imageVector = SkilletIcon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp),
            )
        }
    }
}

@Composable
private fun rememberBitmap(path: String?): ImageBitmap? = remember(path) {
    path?.let { file ->
        runCatching { BitmapFactory.decodeFile(file)?.asImageBitmap() }.getOrNull()
    }
}
