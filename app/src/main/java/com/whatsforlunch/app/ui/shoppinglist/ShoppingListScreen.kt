package com.whatsforlunch.app.ui.shoppinglist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.whatsforlunch.app.R
import com.whatsforlunch.app.domain.model.Aisle
import com.whatsforlunch.app.domain.model.IngredientUnit
import com.whatsforlunch.app.domain.model.ShoppingListItem
import com.whatsforlunch.app.domain.shoppinglist.ShoppingGroup
import com.whatsforlunch.app.domain.shoppinglist.ShoppingGroupMode
import com.whatsforlunch.app.domain.shoppinglist.ShoppingListRules
import com.whatsforlunch.app.domain.shoppinglist.ShoppingProgress
import com.whatsforlunch.app.domain.shoppinglist.aisleKey
import com.whatsforlunch.app.ui.components.UnitDropdown
import com.whatsforlunch.app.ui.components.unitLabel
import com.whatsforlunch.app.ui.recipedetail.formatQuantity
import com.whatsforlunch.app.ui.theme.WarmRadius
import com.whatsforlunch.app.ui.theme.WarmSpacing
import com.whatsforlunch.app.ui.theme.warmShadow
import java.math.BigDecimal
import java.text.DecimalFormatSymbols
import java.util.Locale

// Identificadores estables para las pruebas instrumentadas (E2E) de la Lista.
object ShoppingListTestTags {
    const val SCREEN = "shopping_list_screen"
    const val TITLE = "shopping_list_title"
    const val LIST = "shopping_list_items"
    const val ITEM = "shopping_list_item"
    const val EMPTY = "shopping_list_empty"
    const val EMPTY_CTA = "shopping_list_scan_pantry"
    const val EMPTY_ADD_CTA = "shopping_list_empty_add_item"
    const val SNACKBAR = "shopping_list_snackbar"
    const val UNDO = "shopping_list_undo"
    const val ADD_ITEM = "shopping_list_add_item"
    const val EDIT_DIALOG = "shopping_list_edit_dialog"
    const val NAME_FIELD = "shopping_list_name_field"
    const val NAME_ERROR = "shopping_list_name_error"
    const val QUANTITY_FIELD = "shopping_list_quantity_field"
    const val QUANTITY_ERROR = "shopping_list_quantity_error"
    const val UNIT_FIELD = "shopping_list_unit_field"
    const val AISLE_FIELD = "shopping_list_aisle_field"
    const val SAVE_ITEM = "shopping_list_save_item"
    const val CANCEL_EDIT = "shopping_list_cancel_edit"
    const val PROGRESS_CARD = "shopping_list_progress_card"
    const val PROGRESS_TEXT = "shopping_list_progress_text"
    const val PROGRESS_PERCENT = "shopping_list_progress_percent"
    const val PROGRESS_BAR = "shopping_list_progress_bar"
    const val GROUP_BY_LABEL = "shopping_list_group_by_label"
    const val GROUP_MODE_AISLE = "shopping_list_group_mode_aisle"
    const val GROUP_MODE_RECIPE = "shopping_list_group_mode_recipe"
    const val CLEAR_COMPLETED = "shopping_list_clear_completed"

    fun item(id: String) = "${ITEM}_$id"

    fun check(id: String) = "shopping_list_check_$id"

    fun delete(id: String) = "shopping_list_delete_$id"

    fun origin(id: String) = "shopping_list_origin_$id"

    fun edit(id: String) = "shopping_list_edit_$id"

    fun aisle(id: String) = "shopping_list_aisle_$id"

    fun aisleOption(aisle: String?) = "shopping_list_aisle_option_${aisle ?: "none"}"

    fun groupHeader(key: String) = "shopping_list_group_header_${key.ifBlank { "none" }}"

    fun groupCount(key: String) = "shopping_list_group_count_${key.ifBlank { "none" }}"
}

private val CardShape = RoundedCornerShape(WarmRadius.md)
private val PillShape = RoundedCornerShape(50)

// D5: la fila completada se atenúa (el prototipo la muestra al 60 %) y su
// título se tacha, sin sustituir el nombre por «En la cesta».
private const val COMPLETED_ROW_ALPHA = 0.6f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingListScreen(
    uiState: ShoppingListUiState,
    onDeleteItem: (String) -> Unit,
    onUndoDelete: () -> Unit,
    onUndoExpired: () -> Unit,
    onAddItem: () -> Unit,
    onEditItem: (String) -> Unit,
    onDismissEditor: () -> Unit,
    onSaveItem: (String, Double?, String, Aisle?) -> Unit,
    onToggleCompleted: (String) -> Unit,
    onSetGroupMode: (ShoppingGroupMode) -> Unit,
    onClearCompleted: () -> Unit,
    modifier: Modifier = Modifier,
    scrollToTopTick: Int = 0,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val deletedMessage = stringResource(R.string.shopping_list_item_deleted)
    val completedRemovedMessage = stringResource(R.string.shopping_list_items_removed)
    val undoLabel = stringResource(R.string.shopping_list_undo)
    val pendingUndo = uiState.pendingUndo
    // RF-MOB6: tocar la pestaña activa devuelve el contenido a su primera posición.
    val itemListState = rememberLazyListState()

    LaunchedEffect(pendingUndo) {
        if (pendingUndo == null) return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = if (pendingUndo.bulk) completedRemovedMessage else deletedMessage,
            actionLabel = undoLabel,
        )
        if (result == SnackbarResult.ActionPerformed) {
            onUndoDelete()
        } else {
            onUndoExpired()
        }
    }
    // RF-MOB6: el trigger lo eleva la barra (extraída a WhatsForLunchApp); el
    // valor inicial no desplaza nada.
    LaunchedEffect(scrollToTopTick) {
        if (scrollToTopTick > 0) itemListState.animateScrollToItem(0)
    }

    Scaffold(
        modifier = modifier.testTag(ShoppingListTestTags.SCREEN),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            // RF-MOB5: la Lista de la Compra es destino de primer nivel y ya no
            // muestra «Volver»; conserva sus acciones propias.
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.shopping_list_title),
                        modifier = Modifier.testTag(ShoppingListTestTags.TITLE),
                    )
                },
                actions = {
                    // RF-AGL7 (D7): «Limpiar comprados» solo aparece cuando hay
                    // al menos un ítem completado.
                    if (uiState.items.any { it.completed }) {
                        IconButton(
                            onClick = onClearCompleted,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag(ShoppingListTestTags.CLEAR_COMPLETED),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteSweep,
                                contentDescription = stringResource(R.string.shopping_list_clear_completed),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },

        floatingActionButton = {
            // FAB redondo (CircleShape) de solo icono con los colores primarios:
            // el texto queda como contentDescription para TalkBack y las pruebas
            // siguen por testTag.
            FloatingActionButton(
                onClick = onAddItem,
                modifier = Modifier.testTag(ShoppingListTestTags.ADD_ITEM),
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(
                    Icons.Outlined.Add,
                    contentDescription = stringResource(R.string.shopping_list_add_item),
                )
            }
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
                onAddItem = onAddItem,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        } else {
            Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                ShoppingProgressCard(
                    progress = uiState.progress,
                    mode = uiState.mode,
                    onModeChange = onSetGroupMode,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = WarmSpacing.md)
                        .padding(top = WarmSpacing.md),
                )
                LazyColumn(
                    state = itemListState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = WarmSpacing.md)
                        .testTag(ShoppingListTestTags.LIST),
                    contentPadding = PaddingValues(vertical = WarmSpacing.md),
                    verticalArrangement = Arrangement.spacedBy(WarmSpacing.sm),
                ) {
                    uiState.groups.forEach { group ->
                        item(key = "group:${group.key}") {
                            ShoppingGroupHeader(group = group, mode = uiState.mode)
                        }
                        items(group.items, key = { "item:${group.key}:${it.id}" }) { item ->
                            ShoppingItemRow(
                                item = item,
                                mode = uiState.mode,
                                onToggleCompleted = { onToggleCompleted(item.id) },
                                onEdit = { onEditItem(item.id) },
                                onDelete = { onDeleteItem(item.id) },
                            )
                        }
                    }
                }
            }
        }
    }

    if (uiState.editorVisible) {
        ShoppingItemDialog(
            item = uiState.editing,
            onDismiss = onDismissEditor,
            onSave = onSaveItem,
        )
    }
}

@Composable
private fun ShoppingItemRow(
    item: ShoppingListItem,
    mode: ShoppingGroupMode,
    onToggleCompleted: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val locale = LocalConfiguration.current.locales[0]
    val quantity = item.quantity?.takeIf { it > 0.0 }?.let { formatQuantity(it, locale) }
    // Unidad presentada con el vocabulario cerrado (D4): los códigos del enum con
    // sus textos es/en y la legada como texto; la vacía se omite (RD19).
    val unit = unitLabel(item.unit)
    val detail = listOfNotNull(quantity, unit.takeIf { it.isNotBlank() })
        .joinToString(" ")
    val line = if (detail.isBlank()) {
        item.name
    } else {
        stringResource(R.string.shopping_list_item_line, item.name, detail)
    }

    Surface(
        shape = CardShape,
        color = if (item.completed) {
            MaterialTheme.colorScheme.surfaceContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerLowest
        },
        modifier = Modifier
            .fillMaxWidth()
            .warmShadow(shape = CardShape)
            .testTag(ShoppingListTestTags.ITEM),
    ) {
        Row(
            modifier = Modifier.padding(
                start = WarmSpacing.xs,
                top = WarmSpacing.sm,
                bottom = WarmSpacing.sm,
                end = WarmSpacing.xs,
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // RF-AGL5 (D4/D5): checkbox reversible; al marcar, la fila queda con
            // el título tachado y atenuada, sin snackbar.
            Checkbox(
                checked = item.completed,
                onCheckedChange = { onToggleCompleted() },
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.secondary,
                    checkmarkColor = MaterialTheme.colorScheme.onSecondary,
                    uncheckedColor = MaterialTheme.colorScheme.outline,
                ),
                modifier = Modifier.testTag(ShoppingListTestTags.check(item.id)),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .alpha(if (item.completed) COMPLETED_ROW_ALPHA else 1f)
                    .testTag(ShoppingListTestTags.item(item.id)),
                verticalArrangement = Arrangement.spacedBy(WarmSpacing.xs),
            ) {
                Text(
                    text = line,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (item.completed) TextDecoration.LineThrough else null,
                )
                // RF-AGL6 (D6): la etiqueta de Pasillo solo se muestra en «Por
                // Receta»; en «Pasillo» la da la cabecera del grupo.
                if (mode == ShoppingGroupMode.BY_RECIPE) {
                    Text(
                        text = stringResource(item.aisle.labelRes()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag(ShoppingListTestTags.aisle(item.id)),
                    )
                }
                // RF-AGL6 (D6): el badge de una sola fuente es redundante en
                // «Por Receta»; «Para: N recetas» y «Despensa baja» se
                // conservan en ambos modos.
                if (mode == ShoppingGroupMode.BY_AISLE || item.sources.size != 1) {
                    OriginBadge(item = item)
                }
            }
            Spacer(modifier = Modifier.width(WarmSpacing.xs))
            IconButton(
                onClick = onEdit,
                modifier = Modifier
                    .size(48.dp)
                    .testTag(ShoppingListTestTags.edit(item.id)),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = stringResource(R.string.shopping_list_edit_item),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
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

// Tarjeta de progreso y selector «Agrupar por» (RD20, RF-AGL1/RF-AGL2): solo se
// muestra con ítems en la Lista (D9); el porcentaje es del entero más próximo.
@Composable
private fun ShoppingProgressCard(
    progress: ShoppingProgress,
    mode: ShoppingGroupMode,
    onModeChange: (ShoppingGroupMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = CardShape,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = modifier
            .warmShadow(shape = CardShape)
            .testTag(ShoppingListTestTags.PROGRESS_CARD),
    ) {
        Column(
            modifier = Modifier.padding(WarmSpacing.md),
            verticalArrangement = Arrangement.spacedBy(WarmSpacing.sm),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = pluralStringResource(
                        R.plurals.shopping_list_progress,
                        progress.total,
                        progress.completed,
                        progress.total,
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .weight(1f)
                        .testTag(ShoppingListTestTags.PROGRESS_TEXT),
                )
                Text(
                    text = stringResource(
                        R.string.shopping_list_progress_percent,
                        progress.percent,
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.testTag(ShoppingListTestTags.PROGRESS_PERCENT),
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(PillShape)
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .semantics {
                        progressBarRangeInfo = ProgressBarRangeInfo(
                            current = progress.percent / 100f,
                            range = 0f..1f,
                        )
                    }
                    .testTag(ShoppingListTestTags.PROGRESS_BAR),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.percent / 100f)
                        .height(8.dp)
                        .background(MaterialTheme.colorScheme.secondary, PillShape),
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(WarmSpacing.xs)) {
                Text(
                    text = stringResource(R.string.shopping_list_group_by_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag(ShoppingListTestTags.GROUP_BY_LABEL),
                )
                Surface(
                    shape = PillShape,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                ) {
                    Row(modifier = Modifier.padding(2.dp)) {
                        GroupModeButton(
                            selected = mode == ShoppingGroupMode.BY_AISLE,
                            label = stringResource(R.string.shopping_list_group_aisle),
                            testTag = ShoppingListTestTags.GROUP_MODE_AISLE,
                            onClick = { onModeChange(ShoppingGroupMode.BY_AISLE) },
                        )
                        GroupModeButton(
                            selected = mode == ShoppingGroupMode.BY_RECIPE,
                            label = stringResource(R.string.shopping_list_group_recipe),
                            testTag = ShoppingListTestTags.GROUP_MODE_RECIPE,
                            onClick = { onModeChange(ShoppingGroupMode.BY_RECIPE) },
                        )
                    }
                }
            }
        }
    }
}

// Segmento del selector «Agrupar por» (D1): el elegido queda en superficie
// clara dentro de la píldora del contenedor.
@Composable
private fun GroupModeButton(
    selected: Boolean,
    label: String,
    testTag: String,
    onClick: () -> Unit,
) {
    Surface(
        shape = PillShape,
        color = if (selected) {
            MaterialTheme.colorScheme.surfaceContainerLowest
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        modifier = Modifier
            .toggleable(value = selected, role = Role.Tab, onValueChange = { onClick() })
            .testTag(testTag),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = WarmSpacing.md, vertical = WarmSpacing.xs),
        )
    }
}

// Cabecera de grupo (RD20, RF-AGL3/RF-AGL4): título del Pasillo o de la fuente
// con el contador de filas mostradas en el grupo.
@Composable
private fun ShoppingGroupHeader(
    group: ShoppingGroup,
    mode: ShoppingGroupMode,
) {
    val title = when (mode) {
        ShoppingGroupMode.BY_AISLE -> stringResource(group.aisleKey().labelRes())
        ShoppingGroupMode.BY_RECIPE -> group.title.ifBlank {
            stringResource(R.string.shopping_list_group_no_recipe)
        }
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = WarmSpacing.sm),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.testTag(ShoppingListTestTags.groupHeader(group.key)),
        )
        Spacer(modifier = Modifier.width(WarmSpacing.sm))
        Surface(shape = PillShape, color = MaterialTheme.colorScheme.surfaceContainer) {
            Text(
                text = pluralStringResource(
                    R.plurals.shopping_list_group_count,
                    group.items.size,
                    group.items.size,
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(horizontal = WarmSpacing.sm, vertical = 2.dp)
                    .testTag(ShoppingListTestTags.groupCount(group.key)),
            )
        }
    }
}

// Pasillo de la fila o «Sin clasificar» cuando el ítem no tiene (RD19, D4).
@Composable
private fun Aisle?.labelRes(): Int = when (this) {
    null -> R.string.shopping_list_aisle_none
    Aisle.PRODUCE -> R.string.shopping_list_aisle_produce
    Aisle.MEAT_FISH -> R.string.shopping_list_aisle_meat_fish
    Aisle.DAIRY -> R.string.shopping_list_aisle_dairy
    Aisle.PANTRY_OILS -> R.string.shopping_list_aisle_pantry_oils
}

@Composable
private fun OriginBadge(item: ShoppingListItem) {
    val titles = item.sources.map { it.sourceTitle }
    // RD09/D5: sin fuentes el Origen es «Despensa baja» (alta manual).
    val manual = titles.isEmpty()
    val label = when {
        manual -> stringResource(R.string.shopping_list_manual_origin)
        titles.size <= 1 -> stringResource(R.string.shopping_list_origin_single, titles.firstOrNull().orEmpty())
        else -> pluralStringResource(
            R.plurals.shopping_list_origin_multiple,
            titles.size,
            titles.size,
        )
    }
    val accessibility = stringResource(
        R.string.shopping_list_origin_accessibility,
        if (manual) stringResource(R.string.shopping_list_manual_origin) else titles.joinToString(", "),
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
                imageVector = Icons.Outlined.ShoppingCart,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                // Una sola línea con ellipsis: el título largo de la Receta no
                // estira la fila en «Pasillo de Super» (el texto completo sigue
                // disponible para TalkBack vía el contentDescription de la píldora).
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun EmptyShoppingList(
    onAddItem: () -> Unit,
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
                imageVector = Icons.Outlined.ShoppingCart,
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
        // D2 (mover-escaneo-a-la-despensa): el CTA «Escanear la Despensa» se retira
        // de la Lista; «Añadir ítem» pasa a botón primario en su hueco y el tag
        // EMPTY_CTA queda sin uso en la UI (los tests lo aseguran ausente).
        Button(
            onClick = onAddItem,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = WarmSpacing.xl)
                .heightIn(min = 52.dp)
                .testTag(ShoppingListTestTags.EMPTY_ADD_CTA),
        ) {
            Text(stringResource(R.string.shopping_list_add_item))
        }
    }
}

// Repair H1 (validación sesión 88): el prellenado del campo Cantidad necesita un
// formato estable, sin agrupación de millares ni redondeo, para que el
// ida-y-vuelta con parseManualQuantity conserve cantidades ≥ 1000 y de más de 3
// decimales (formatQuantity agrupa y recorta: 1200 → «1.200» → 1.2 y 1234.5 no
// parsea). La fila de la lista y el Detalle de Receta siguen con formatQuantity.
// D6 admite coma o punto: se usa la coma del locale cuando existe para que el
// prellenado coincida con el formato de lectura; cualquier otro caso cae en
// punto, siempre parseable.
internal fun formatEditableQuantity(quantity: Double, locale: Locale): String {
    val plain = BigDecimal.valueOf(quantity).stripTrailingZeros().toPlainString()
    return if (DecimalFormatSymbols.getInstance(locale).decimalSeparator == ',') {
        plain.replace('.', ',')
    } else {
        plain
    }
}

// Diálogo único de alta/edición (RD19, D2): nombre (de solo lectura al editar,
// D7), cantidad decimal opcional, unidad opcional y Pasillo opcional (D4). La
// validación vive en ShoppingListRules (RF-AIL5).
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ShoppingItemDialog(
    item: ShoppingListItem?,
    onDismiss: () -> Unit,
    onSave: (String, Double?, String, Aisle?) -> Unit,
) {
    val locale = LocalConfiguration.current.locales[0]
    var name by remember(item?.id) { mutableStateOf(item?.name.orEmpty()) }
    var quantityText by remember(item?.id) {
        mutableStateOf(item?.quantity?.let { formatEditableQuantity(it, locale) }.orEmpty())
    }
    // D1: los ítems nuevos nacen con «ud»; una unidad legada (incluida la vacía)
    // se conserva tal cual y entra como opción extra del desplegable (D5).
    var unit by remember(item?.id) {
        mutableStateOf(item?.unit ?: IngredientUnit.UD.code)
    }
    var aisle by remember(item?.id) { mutableStateOf(item?.aisle) }
    var showError by remember(item?.id) { mutableStateOf(false) }

    val quantity = if (quantityText.isBlank()) {
        null
    } else {
        ShoppingListRules.parseManualQuantity(quantityText)
    }
    val nameInvalid = showError && !ShoppingListRules.isManualNameValid(name)
    val quantityInvalid = showError && quantityText.isNotBlank() &&
        (quantity == null || !ShoppingListRules.isManualQuantityValid(quantity))

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag(ShoppingListTestTags.EDIT_DIALOG),
        title = {
            Text(
                stringResource(
                    if (item == null) R.string.shopping_list_add_item else R.string.shopping_list_edit_item,
                ),
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(WarmSpacing.sm),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.shopping_list_name_label)) },
                    singleLine = true,
                    readOnly = item != null,
                    isError = nameInvalid,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(ShoppingListTestTags.NAME_FIELD),
                )
                if (nameInvalid) {
                    Text(
                        text = stringResource(R.string.shopping_list_name_error),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.testTag(ShoppingListTestTags.NAME_ERROR),
                    )
                }
                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { quantityText = it },
                    label = { Text(stringResource(R.string.shopping_list_quantity_label)) },
                    singleLine = true,
                    isError = quantityInvalid,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(ShoppingListTestTags.QUANTITY_FIELD),
                )
                if (quantityInvalid) {
                    Text(
                        text = stringResource(R.string.shopping_list_quantity_error),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.testTag(ShoppingListTestTags.QUANTITY_ERROR),
                    )
                }
                // Unidad de lista cerrada (unificar-unidades, D1): desplegable con
                // «ud» preseleccionado en ítems nuevos y la legada como opción extra.
                // Se conserva la estética M3 de los campos vecinos del diálogo.
                UnitDropdown(
                    value = unit,
                    onValueChange = { unit = it },
                    testTag = ShoppingListTestTags.UNIT_FIELD,
                    label = { Text(stringResource(R.string.shopping_list_unit_label)) },
                    shape = RoundedCornerShape(WarmRadius.sm),
                    colors = OutlinedTextFieldDefaults.colors(),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = stringResource(R.string.shopping_list_aisle_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(ShoppingListTestTags.AISLE_FIELD),
                    horizontalArrangement = Arrangement.spacedBy(WarmSpacing.xs),
                ) {
                    FilterChip(
                        selected = aisle == null,
                        onClick = { aisle = null },
                        label = { Text(stringResource(R.string.shopping_list_aisle_none)) },
                        modifier = Modifier.testTag(ShoppingListTestTags.aisleOption(null)),
                    )
                    Aisle.entries.forEach { option ->
                        FilterChip(
                            selected = aisle == option,
                            onClick = { aisle = option },
                            label = { Text(stringResource(option.labelRes())) },
                            modifier = Modifier.testTag(ShoppingListTestTags.aisleOption(option.name)),
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val valid = ShoppingListRules.isManualNameValid(name) &&
                        (quantityText.isBlank() ||
                            (quantity != null && ShoppingListRules.isManualQuantityValid(quantity)))
                    if (valid) {
                        onSave(name, quantity, unit, aisle)
                    } else {
                        showError = true
                    }
                },
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag(ShoppingListTestTags.SAVE_ITEM),
            ) {
                Text(stringResource(R.string.shopping_list_save))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag(ShoppingListTestTags.CANCEL_EDIT),
            ) {
                Text(stringResource(R.string.shopping_list_cancel))
            }
        },
    )
}
