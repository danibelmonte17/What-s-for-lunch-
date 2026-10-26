package com.whatsforlunch.app.ui.shoppinglist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.whatsforlunch.app.R
import com.whatsforlunch.app.domain.model.ShoppingListItem
import com.whatsforlunch.app.ui.components.SkilletIcon
import com.whatsforlunch.app.ui.recipedetail.formatQuantity
import com.whatsforlunch.app.ui.theme.WarmRadius
import com.whatsforlunch.app.ui.theme.WarmSpacing
import com.whatsforlunch.app.ui.theme.warmShadow

// Identificadores estables para las pruebas instrumentadas (E2E) de la Lista.
object ShoppingListTestTags {
    const val SCREEN = "shopping_list_screen"
    const val BACK = "shopping_list_back"
    const val TITLE = "shopping_list_title"
    const val LIST = "shopping_list_items"
    const val ITEM = "shopping_list_item"
    const val EMPTY = "shopping_list_empty"
    const val EMPTY_CTA = "shopping_list_scan_pantry"
    const val SNACKBAR = "shopping_list_snackbar"
    const val UNDO = "shopping_list_undo"

    fun item(id: String) = "${ITEM}_$id"

    fun delete(id: String) = "shopping_list_delete_$id"

    fun origin(id: String) = "shopping_list_origin_$id"
}

private val CardShape = RoundedCornerShape(WarmRadius.md)
private val PillShape = RoundedCornerShape(50)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingListScreen(
    uiState: ShoppingListUiState,
    onBack: () -> Unit,
    onScanPantry: () -> Unit,
    onDeleteItem: (String) -> Unit,
    onUndoDelete: () -> Unit,
    onUndoExpired: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val deletedMessage = stringResource(R.string.shopping_list_item_deleted)
    val undoLabel = stringResource(R.string.shopping_list_undo)
    val pendingUndo = uiState.pendingUndo

    LaunchedEffect(pendingUndo?.id) {
        if (pendingUndo == null) return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = deletedMessage,
            actionLabel = undoLabel,
        )
        if (result == SnackbarResult.ActionPerformed) {
            onUndoDelete()
        } else {
            onUndoExpired()
        }
    }

    Scaffold(
        modifier = modifier.testTag(ShoppingListTestTags.SCREEN),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.shopping_list_title),
                        modifier = Modifier.testTag(ShoppingListTestTags.TITLE),
                    )
                },
                navigationIcon = {
                    TextButton(
                        onClick = onBack,
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag(ShoppingListTestTags.BACK),
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
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    modifier = Modifier.testTag(ShoppingListTestTags.SNACKBAR),
                    action = {
                        data.visuals.actionLabel?.let { label ->
                            TextButton(
                                onClick = { data.performAction() },
                                modifier = Modifier.testTag(ShoppingListTestTags.UNDO),
                            ) {
                                Text(label)
                            }
                        }
                    },
                ) {
                    Text(data.visuals.message)
                }
            }
        },
    ) { innerPadding ->
        if (uiState.items.isEmpty()) {
            EmptyShoppingList(
                onScanPantry = onScanPantry,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = WarmSpacing.md)
                    .testTag(ShoppingListTestTags.LIST),
                contentPadding = PaddingValues(vertical = WarmSpacing.md),
                verticalArrangement = Arrangement.spacedBy(WarmSpacing.sm),
            ) {
                items(uiState.items, key = { it.id }) { item ->
                    ShoppingItemRow(
                        item = item,
                        onDelete = { onDeleteItem(item.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ShoppingItemRow(
    item: ShoppingListItem,
    onDelete: () -> Unit,
) {
    val locale = LocalConfiguration.current.locales[0]
    val quantity = item.quantity?.takeIf { it > 0.0 }?.let { formatQuantity(it, locale) }
    val detail = listOfNotNull(quantity, item.unit.takeIf { it.isNotBlank() })
        .joinToString(" ")
    val line = if (detail.isBlank()) {
        item.name
    } else {
        stringResource(R.string.shopping_list_item_line, item.name, detail)
    }

    Surface(
        shape = CardShape,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = Modifier
            .fillMaxWidth()
            .warmShadow(shape = CardShape)
            .testTag(ShoppingListTestTags.ITEM),
    ) {
        Row(
            modifier = Modifier.padding(
                start = WarmSpacing.md,
                top = WarmSpacing.sm,
                bottom = WarmSpacing.sm,
                end = WarmSpacing.xs,
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .testTag(ShoppingListTestTags.item(item.id)),
                verticalArrangement = Arrangement.spacedBy(WarmSpacing.xs),
            ) {
                Text(
                    text = line,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (item.sources.isNotEmpty()) {
                    OriginBadge(item = item)
                }
            }
            Spacer(modifier = Modifier.width(WarmSpacing.xs))
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(48.dp)
                    .testTag(ShoppingListTestTags.delete(item.id)),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = stringResource(R.string.shopping_list_delete),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun OriginBadge(item: ShoppingListItem) {
    val titles = item.sources.map { it.sourceTitle }
    val label = if (titles.size <= 1) {
        stringResource(R.string.shopping_list_origin_single, titles.firstOrNull().orEmpty())
    } else {
        pluralStringResource(
            R.plurals.shopping_list_origin_multiple,
            titles.size,
            titles.size,
        )
    }
    val accessibility = stringResource(
        R.string.shopping_list_origin_accessibility,
        titles.joinToString(", "),
    )
    Surface(
        shape = PillShape,
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        modifier = Modifier
            .semantics { contentDescription = accessibility }
            .testTag(ShoppingListTestTags.origin(item.id)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = WarmSpacing.sm, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(WarmSpacing.xs),
        ) {
            Icon(
                imageVector = SkilletIcon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun EmptyShoppingList(
    onScanPantry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .padding(horizontal = WarmSpacing.lg)
            .testTag(ShoppingListTestTags.EMPTY),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(WarmRadius.lg),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = SkilletIcon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp),
            )
        }
        Text(
            text = stringResource(R.string.shopping_list_empty_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = WarmSpacing.md),
        )
        Text(
            text = stringResource(R.string.shopping_list_empty_message),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = WarmSpacing.sm),
        )
        Button(
            onClick = onScanPantry,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = WarmSpacing.xl)
                .heightIn(min = 52.dp)
                .testTag(ShoppingListTestTags.EMPTY_CTA),
        ) {
            Text(stringResource(R.string.action_scan_pantry))
        }
    }
}
