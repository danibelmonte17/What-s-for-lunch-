package com.whatsforlunch.app.ui.pantry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Kitchen
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.whatsforlunch.app.R
import com.whatsforlunch.app.domain.model.PantryItem
import com.whatsforlunch.app.domain.pantry.PantryItemValidation
import com.whatsforlunch.app.ui.components.longPressToGallery
import com.whatsforlunch.app.ui.theme.WarmRadius
import com.whatsforlunch.app.ui.theme.WarmSpacing

object PantryTestTags {
    const val SCREEN = "pantry_screen"
    const val TOP_BAR_TITLE = "pantry_top_bar_title"
    const val EMPTY_TITLE = "pantry_empty_title"
    const val EMPTY_MESSAGE = "pantry_empty_message"
    const val ADD_ITEM = "pantry_add_item"
    const val SCAN_FOOD = "pantry_scan_food"
    const val SCAN_FOOD_CTA = "pantry_scan_food_cta"
    const val EMPTY_ADD_CTA = "pantry_empty_add_item"
    const val ITEM_LIST = "pantry_item_list"
    const val ITEM_CARD = "pantry_item_card"
    const val ITEM_NAME = "pantry_item_name"
    const val ITEM_QUANTITY = "pantry_item_quantity"
    const val EDIT_ITEM = "pantry_edit_item"
    const val DELETE_ITEM = "pantry_delete_item"
    const val EDIT_DIALOG = "pantry_edit_dialog"
    const val NAME_FIELD = "pantry_name_field"
    const val QUANTITY_STEPPER = "pantry_quantity_stepper"
    const val QUANTITY_VALUE = "pantry_quantity_value"
    const val QUANTITY_INCREMENT = "pantry_quantity_increment"
    const val QUANTITY_DECREMENT = "pantry_quantity_decrement"
    const val NAME_ERROR = "pantry_name_error"
    const val SAVE_ITEM = "pantry_save_item"
    const val CANCEL_EDIT = "pantry_cancel_edit"
    const val DELETE_DIALOG = "pantry_delete_dialog"
    const val DELETE_CONFIRM = "pantry_delete_confirm"
    const val DELETE_CANCEL = "pantry_delete_cancel"

    fun editItem(id: String) = "${EDIT_ITEM}_$id"
    fun deleteItem(id: String) = "${DELETE_ITEM}_$id"
    fun itemName(id: String) = "${ITEM_NAME}_$id"
    fun itemQuantity(id: String) = "${ITEM_QUANTITY}_$id"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantryScreen(
    uiState: PantryUiState,
    onScanFood: () -> Unit,
    onSaveItem: (String?, String, Int) -> Unit,
    onDeleteItem: (String) -> Unit,
    onErrorConsumed: () -> Unit,
    onSaveCompletedConsumed: () -> Unit,
    modifier: Modifier = Modifier,
    onScanFoodFromGallery: () -> Unit = {},
    scrollToTopTick: Int = 0,
) {
    var itemToEdit by remember { mutableStateOf<PantryItem?>(null) }
    var itemToDelete by remember { mutableStateOf<PantryItem?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val deleteErrorMessage = stringResource(R.string.pantry_delete_error)
    val saveErrorMessage = stringResource(R.string.pantry_save_error)
    // RF-MOB6: tocar la pestaña activa devuelve el contenido a su primera posición.
    val itemListState = rememberLazyListState()

    LaunchedEffect(uiState.saveError) {
        if (uiState.saveError == PantrySaveError.DeleteFailed || uiState.saveError == PantrySaveError.SaveFailed) {
            snackbarHostState.showSnackbar(
                if (uiState.saveError == PantrySaveError.DeleteFailed) {
                    deleteErrorMessage
                } else {
                    saveErrorMessage
                },
            )
            onErrorConsumed()
        }
    }
    LaunchedEffect(uiState.saveCompleted) {
        if (uiState.saveCompleted) {
            showEditor = false
            itemToEdit = null
            onSaveCompletedConsumed()
        }
    }
    // RF-MOB6: el trigger lo eleva la barra (extraída a WhatsForLunchApp); el
    // valor inicial no desplaza nada.
    LaunchedEffect(scrollToTopTick) {
        if (scrollToTopTick > 0) itemListState.animateScrollToItem(0)
    }

    Scaffold(
        modifier = modifier.testTag(PantryTestTags.SCREEN),
        containerColor = MaterialTheme.colorScheme.surface,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            // RF-MOB5: la Despensa es destino de primer nivel y ya no muestra «Volver».
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.pantry_title),
                        modifier = Modifier.testTag(PantryTestTags.TOP_BAR_TITLE),
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
        floatingActionButton = {
            // D1: dos FABs apilados junto a la esquina — «Escanear alimentos» encima
            // y el existente «Añadir alimento» abajo.
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(WarmSpacing.sm),
            ) {
                // FABs redondos (CircleShape) de solo icono: el texto queda como
                // contentDescription para TalkBack y las pruebas siguen por testTag.
                FloatingActionButton(
                    onClick = onScanFood,
                    modifier = Modifier.testTag(PantryTestTags.SCAN_FOOD)
                        // Pulsación larga: en vez de abrir la cámara, elige una
                        // foto de la galería y la analiza con la IA.
                        .longPressToGallery(onGalleryRequested = onScanFoodFromGallery),
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    Icon(
                        Icons.Outlined.PhotoCamera,
                        contentDescription = stringResource(R.string.pantry_scan_food),
                    )
                }
                FloatingActionButton(
                    onClick = { itemToEdit = null; showEditor = true },
                    modifier = Modifier.testTag(PantryTestTags.ADD_ITEM),
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    Icon(
                        Icons.Outlined.Add,
                        contentDescription = stringResource(R.string.pantry_add_food),
                    )
                }
            }
        },
    ) { innerPadding ->
        if (uiState.items.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = WarmSpacing.lg),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Surface(
                    modifier = Modifier.size(56.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                ) {
                    Icon(
                        Icons.Outlined.Kitchen,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(14.dp),
                    )
                }
                Text(
                    stringResource(R.string.pantry_empty_title),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(top = WarmSpacing.md).testTag(PantryTestTags.EMPTY_TITLE),
                )
                Text(
                    stringResource(R.string.pantry_empty_message),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = WarmSpacing.sm).testTag(PantryTestTags.EMPTY_MESSAGE),
                )
                // D3: el estado vacío replica los dos FABs — «Escanear alimentos»
                // primario y «Añadir alimento» debajo.
                Button(
                    onClick = onScanFood,
                    modifier = Modifier.fillMaxWidth().padding(top = WarmSpacing.lg).heightIn(min = 52.dp)
                        // Pulsación larga: en vez de abrir la cámara, elige una
                        // foto de la galería y la analiza con la IA.
                        .longPressToGallery(onGalleryRequested = onScanFoodFromGallery)
                        .testTag(PantryTestTags.SCAN_FOOD_CTA),
                ) { Text(stringResource(R.string.pantry_scan_food)) }
                OutlinedButton(
                    onClick = { itemToEdit = null; showEditor = true },
                    modifier = Modifier.fillMaxWidth().padding(top = WarmSpacing.sm).heightIn(min = 52.dp)
                        .testTag(PantryTestTags.EMPTY_ADD_CTA),
                ) { Text(stringResource(R.string.pantry_add_food)) }
            }
        } else {
            LazyColumn(
                state = itemListState,
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = WarmSpacing.md)
                    .testTag(PantryTestTags.ITEM_LIST),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = WarmSpacing.md),
                verticalArrangement = Arrangement.spacedBy(WarmSpacing.md),
            ) {
                items(uiState.items, key = PantryItem::id) { item ->
                    PantryItemCard(
                        item = item,
                        onEdit = { itemToEdit = item },
                        onDelete = { itemToDelete = item },
                    )
                }
            }
        }
    }

    LaunchedEffect(itemToEdit) { if (itemToEdit != null) showEditor = true }
    if (showEditor) {
        PantryItemDialog(
            item = itemToEdit,
            error = uiState.saveError,
            onDismiss = { showEditor = false; itemToEdit = null; onErrorConsumed() },
            onSave = { name, quantity -> onSaveItem(itemToEdit?.id, name, quantity) },
        )
    }
    itemToDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            modifier = Modifier.testTag(PantryTestTags.DELETE_DIALOG),
            title = { Text(stringResource(R.string.pantry_delete_title)) },
            text = { Text(stringResource(R.string.pantry_delete_message, item.name)) },
            confirmButton = {
                TextButton(
                    onClick = { itemToDelete = null; onDeleteItem(item.id) },
                    modifier = Modifier.testTag(PantryTestTags.DELETE_CONFIRM),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text(stringResource(R.string.pantry_delete_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }, modifier = Modifier.testTag(PantryTestTags.DELETE_CANCEL)) {
                    Text(stringResource(R.string.pantry_cancel))
                }
            },
        )
    }

}

@Composable
private fun PantryItemCard(item: PantryItem, onEdit: () -> Unit, onDelete: () -> Unit) {
    val shape = RoundedCornerShape(WarmRadius.lg)
    val editLabel = stringResource(R.string.pantry_edit_accessibility)
    val deleteLabel = stringResource(R.string.pantry_delete_accessibility)
    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = Modifier.fillMaxWidth().padding(horizontal = WarmSpacing.md, vertical = WarmSpacing.sm).testTag(PantryTestTags.ITEM_CARD),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = WarmSpacing.md, end = WarmSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.testTag(PantryTestTags.itemName(item.id)))
                item.quantity?.let { quantity ->
                    Text(
                        stringResource(R.string.pantry_quantity_value, quantity),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp).testTag(PantryTestTags.itemQuantity(item.id)),
                    )
                }
            }
            IconButton(
                onClick = onEdit,
                modifier = Modifier.size(48.dp).semantics { contentDescription = "$editLabel ${item.name}" }
                    .testTag(PantryTestTags.editItem(item.id)),
            ) { Icon(Icons.Outlined.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(48.dp).semantics { contentDescription = "$deleteLabel ${item.name}" }
                    .testTag(PantryTestTags.deleteItem(item.id)),
            ) { Icon(Icons.Outlined.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable
private fun PantryItemDialog(
    item: PantryItem?,
    error: PantrySaveError?,
    onDismiss: () -> Unit,
    onSave: (String, Int) -> Unit,
) {
    var name by remember(item?.id) { mutableStateOf(item?.name.orEmpty()) }
    var quantity by remember(item?.id) { mutableIntStateOf(item?.quantity ?: PantryItemValidation.MIN_QUANTITY) }
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag(PantryTestTags.EDIT_DIALOG),
        title = { Text(stringResource(if (item == null) R.string.pantry_add_title else R.string.pantry_edit_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(WarmSpacing.sm)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.pantry_name_label)) },
                    singleLine = true,
                    isError = error == PantrySaveError.InvalidName || error == PantrySaveError.DuplicateName,
                    modifier = Modifier.fillMaxWidth().testTag(PantryTestTags.NAME_FIELD),
                )
                if (error == PantrySaveError.DuplicateName) {
                    Text(stringResource(R.string.pantry_duplicate_error), color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag(PantryTestTags.NAME_ERROR))
                }
                Row(
                    modifier = Modifier.fillMaxWidth().testTag(PantryTestTags.QUANTITY_STEPPER),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(R.string.pantry_quantity_label), modifier = Modifier.weight(1f))
                    IconButton(
                        onClick = { quantity = (quantity - 1).coerceAtLeast(PantryItemValidation.MIN_QUANTITY) },
                        enabled = quantity > PantryItemValidation.MIN_QUANTITY,
                        modifier = Modifier.size(48.dp).testTag(PantryTestTags.QUANTITY_DECREMENT),
                    ) {
                        Icon(Icons.Outlined.Remove, contentDescription = stringResource(R.string.pantry_quantity_decrease))
                    }
                    Text(
                        quantity.toString(),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(40.dp).testTag(PantryTestTags.QUANTITY_VALUE),
                    )
                    IconButton(
                        onClick = { quantity = (quantity + 1).coerceAtMost(PantryItemValidation.MAX_QUANTITY) },
                        enabled = quantity < PantryItemValidation.MAX_QUANTITY,
                        modifier = Modifier.size(48.dp).testTag(PantryTestTags.QUANTITY_INCREMENT),
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.pantry_quantity_increase))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, quantity) },
                enabled = name.isNotBlank(),
                modifier = Modifier.heightIn(min = 48.dp).testTag(PantryTestTags.SAVE_ITEM),
            ) { Text(stringResource(if (item == null) R.string.pantry_add_food else R.string.pantry_save_changes)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp).testTag(PantryTestTags.CANCEL_EDIT)) {
                Text(stringResource(R.string.pantry_cancel))
            }
        },
    )
}
