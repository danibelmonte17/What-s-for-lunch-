package com.whatsforlunch.app.ui.pantry

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Kitchen
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.whatsforlunch.app.R
import com.whatsforlunch.app.domain.model.PantryItem
import com.whatsforlunch.app.domain.pantry.ExpiryText
import com.whatsforlunch.app.domain.pantry.Freshness
import com.whatsforlunch.app.domain.pantry.FreshnessRules
import com.whatsforlunch.app.domain.pantry.PantryItemValidation
import com.whatsforlunch.app.ui.components.longPressToGallery
import com.whatsforlunch.app.ui.myrecipes.MyRecipesTestTags
import com.whatsforlunch.app.ui.theme.WarmRadius
import com.whatsforlunch.app.ui.theme.WarmSpacing
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

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
    // Nuevos del rediseño (`redisenar-vista-de-la-despensa`, D7-D18).
    const val SECTION_SUBTITLE = "pantry_section_subtitle"
    const val FRESH_BADGE = "pantry_fresh_badge"
    const val SUMMARY_CARD = "pantry_summary_card"
    const val SUMMARY_COUNT = "pantry_summary_count"
    const val SUMMARY_CAPACITY = "pantry_summary_capacity"
    const val SUMMARY_EXPIRING = "pantry_summary_expiring"
    const val TIP_BAND = "pantry_tip_band"
    const val TIP_MESSAGE = "pantry_tip_message"
    const val SEARCH_FIELD = "pantry_search_field"
    const val SEARCH_CLEAR = "pantry_search_clear"
    const val NO_RESULTS = "pantry_no_results"
    const val NO_RESULTS_TITLE = "pantry_no_results_title"
    const val NO_RESULTS_ADD = "pantry_no_results_add"
    const val EXPIRY_ROW = "pantry_expiry_row"
    const val EXPIRY_VALUE = "pantry_expiry_value"
    const val EXPIRY_CLEAR = "pantry_expiry_clear"
    const val EXPIRY_PICKER = "pantry_expiry_picker"
    const val EXPIRY_CONFIRM = "pantry_expiry_confirm"
    const val EXPIRY_CANCEL = "pantry_expiry_cancel"

    fun editItem(id: String) = "${EDIT_ITEM}_$id"
    fun deleteItem(id: String) = "${DELETE_ITEM}_$id"
    fun itemName(id: String) = "${ITEM_NAME}_$id"
    fun itemQuantity(id: String) = "${ITEM_QUANTITY}_$id"
    fun itemFreshness(id: String) = "pantry_item_freshness_$id"
    fun itemExpiry(id: String) = "pantry_item_expiry_$id"
    fun itemMenu(id: String) = "pantry_item_menu_$id"
    fun itemQuantityDecrease(id: String) = "pantry_item_qty_dec_$id"
    fun itemQuantityIncrease(id: String) = "pantry_item_qty_inc_$id"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantryScreen(
    uiState: PantryUiState,
    onScanFood: () -> Unit,
    onSaveItem: (String?, String, Int, LocalDate?) -> Unit,
    onQuantityChange: (String, Int) -> Unit,
    onSearchChange: (String) -> Unit,
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
    // El reloj lo aporta la pantalla a las reglas puras de frescura (RD28).
    val today = LocalDate.now()

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
            // D17: el título de la top bar es «Mi Despensa» (la pestaña de la barra
            // inferior conserva su clave corta `pantry_tab_label`).
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
            // Estado vacío (`code_empty.html`): la cabecera de sección se conserva
            // (D9) y no hay buscador, resumen ni consejo (D18).
            Column(
                modifier = Modifier.fillMaxSize().padding(innerPadding)
                    .padding(horizontal = WarmSpacing.md)
                    .verticalScroll(rememberScrollState()),
            ) {
                PantrySectionHeader(freshCount = 0)
                Surface(
                    shape = RoundedCornerShape(WarmRadius.lg),
                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                    modifier = Modifier.fillMaxWidth().padding(top = WarmSpacing.sm),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(WarmSpacing.lg),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Spacer(modifier = Modifier.height(32.dp))
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            modifier = Modifier
                                .size(64.dp)
                                .testTag(MyRecipesTestTags.EMPTY_ICON_CONTAINER),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.Kitchen,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .size(32.dp)
                                        .testTag(MyRecipesTestTags.EMPTY_ICON),
                                )
                            }
                        }
                        Text(
                            stringResource(R.string.pantry_empty_title),
                            style = MaterialTheme.typography.headlineSmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = WarmSpacing.md)
                                .testTag(PantryTestTags.EMPTY_TITLE),
                        )
                        Text(
                            stringResource(R.string.pantry_empty_message),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = WarmSpacing.sm)
                                .testTag(PantryTestTags.EMPTY_MESSAGE),
                        )
                        // D16: «Añadir Alimento» es la acción primaria y va primero;
                        // «Escanear la despensa» queda en tonal (enmienda de la D3 de
                        // `mover-escaneo-a-la-despensa`).
                        Button(
                            onClick = { itemToEdit = null; showEditor = true },
                            modifier = Modifier.fillMaxWidth().padding(top = WarmSpacing.lg)
                                .heightIn(min = 52.dp).testTag(PantryTestTags.EMPTY_ADD_CTA),
                        ) {
                            Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(WarmSpacing.xs))
                            Text(stringResource(R.string.pantry_empty_add_cta))
                        }
                        Button(
                            onClick = onScanFood,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                contentColor = MaterialTheme.colorScheme.onSurface,
                            ),
                            modifier = Modifier.fillMaxWidth().padding(top = WarmSpacing.sm)
                                .heightIn(min = 52.dp)
                                // Pulsación larga: en vez de abrir la cámara, elige una
                                // foto de la galería y la analiza con la IA (de develop).
                                .longPressToGallery(onGalleryRequested = onScanFoodFromGallery)
                                .testTag(PantryTestTags.SCAN_FOOD_CTA),
                        ) {
                            Icon(Icons.Outlined.DocumentScanner, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(WarmSpacing.xs))
                            Text(stringResource(R.string.pantry_empty_scan_cta))
                        }
                    }
                }
            }
        } else {
            val expiringCount = FreshnessRules.expiringCount(uiState.items, today)
            val tipNames = FreshnessRules.expiringNames(uiState.items, today)
            LazyColumn(
                state = itemListState,
                modifier = Modifier.fillMaxSize().padding(innerPadding)
                    .padding(horizontal = WarmSpacing.md)
                    .testTag(PantryTestTags.ITEM_LIST),
                contentPadding = PaddingValues(vertical = WarmSpacing.md),
                verticalArrangement = Arrangement.spacedBy(WarmSpacing.sm),
            ) {
                item(key = "header") { PantrySectionHeader(freshCount = uiState.items.size) }
                item(key = "summary") { PantrySummaryCard(uiState.items, today) }
                if (expiringCount > 0) {
                    item(key = "tip") { PantryTipBand(expiringCount, tipNames) }
                }
                item(key = "search") {
                    PantrySearchField(uiState.searchQuery, onSearchChange)
                }
                if (uiState.visibleItems.isEmpty()) {
                    item(key = "no-results") {
                        PantryNoResults(onAdd = { itemToEdit = null; showEditor = true })
                    }
                } else {
                    items(uiState.visibleItems, key = PantryItem::id) { item ->
                        PantryItemCard(
                            item = item,
                            today = today,
                            onEdit = { itemToEdit = item },
                            onDelete = { itemToDelete = item },
                            onQuantityChange = onQuantityChange,
                        )
                    }
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
            onSave = { name, quantity, expiryDate -> onSaveItem(itemToEdit?.id, name, quantity, expiryDate) },
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

// Cabecera de sección del prototipo: subtítulo con el badge «N frescos» (D9:
// el recuento es el total de Alimentos). El título vive en la top bar (D17).
@Composable
private fun PantrySectionHeader(freshCount: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().padding(top = WarmSpacing.md, bottom = WarmSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(WarmSpacing.sm),
    ) {
        Text(
            stringResource(R.string.pantry_section_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f).testTag(PantryTestTags.SECTION_SUBTITLE),
        )
        Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(WarmSpacing.xs),
            ) {
                Box(
                    modifier = Modifier.size(6.dp)
                        .background(MaterialTheme.colorScheme.secondary, CircleShape),
                )
                Text(
                    pluralStringResource(R.plurals.pantry_fresh_count, freshCount, freshCount),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.testTag(PantryTestTags.FRESH_BADGE),
                )
            }
        }
    }
}

// Tarjeta de resumen (D4 sin botones, D9/D10 contadores y capacidad).
@Composable
private fun PantrySummaryCard(items: List<PantryItem>, today: LocalDate, modifier: Modifier = Modifier) {
    val expiringCount = FreshnessRules.expiringCount(items, today)
    Surface(
        shape = RoundedCornerShape(WarmRadius.lg),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier.fillMaxWidth().testTag(PantryTestTags.SUMMARY_CARD),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(WarmSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(WarmSpacing.sm),
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    Icons.Outlined.Kitchen,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(6.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    pluralStringResource(R.plurals.pantry_food_count, items.size, items.size),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.testTag(PantryTestTags.SUMMARY_COUNT),
                )
                Text(
                    stringResource(R.string.pantry_summary_capacity, FreshnessRules.pantryCapacityPercent(items)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag(PantryTestTags.SUMMARY_CAPACITY),
                )
            }
            if (expiringCount > 0) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                ) {
                    Text(
                        pluralStringResource(R.plurals.pantry_expiring_count, expiringCount, expiringCount),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            .testTag(PantryTestTags.SUMMARY_EXPIRING),
                    )
                }
            }
        }
    }
}

// Consejo de cocina determinista (D11): solo con ≥ 1 «Consumir pronto».
@Composable
private fun PantryTipBand(count: Int, names: List<String>, modifier: Modifier = Modifier) {
    val message = when {
        count > 2 -> pluralStringResource(R.plurals.pantry_tip_many, count, count)
        names.size >= 2 -> stringResource(R.string.pantry_tip_two, names[0], names[1])
        else -> stringResource(R.string.pantry_tip_one, names[0])
    }
    Surface(
        shape = RoundedCornerShape(WarmRadius.lg),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        modifier = modifier.fillMaxWidth().testTag(PantryTestTags.TIP_BAND),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(WarmSpacing.sm),
            horizontalArrangement = Arrangement.spacedBy(WarmSpacing.sm),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(Icons.Outlined.Eco, contentDescription = null, modifier = Modifier.size(22.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.pantry_tip_label),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(message, style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.testTag(PantryTestTags.TIP_MESSAGE))
            }
        }
    }
}

// Buscador con filtrado en vivo (D18, sin debounce).
@Composable
private fun PantrySearchField(query: String, onSearchChange: (String) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = query,
        onValueChange = onSearchChange,
        modifier = modifier.fillMaxWidth().testTag(PantryTestTags.SEARCH_FIELD),
        placeholder = { Text(stringResource(R.string.pantry_search_hint)) },
        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(
                    onClick = { onSearchChange("") },
                    modifier = Modifier.testTag(PantryTestTags.SEARCH_CLEAR),
                ) {
                    Icon(
                        Icons.Outlined.Close,
                        contentDescription = stringResource(R.string.pantry_search_clear),
                    )
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(WarmRadius.md),
    )
}

// Estado «sin resultados» al filtrar sin coincidencias (D7/D18).
@Composable
private fun PantryNoResults(onAdd: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(WarmRadius.lg),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = modifier.fillMaxWidth().testTag(PantryTestTags.NO_RESULTS),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(WarmSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.size(56.dp),
            ) {
                Icon(
                    Icons.Outlined.SearchOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(14.dp),
                )
            }
            Text(
                stringResource(R.string.pantry_no_results_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = WarmSpacing.sm)
                    .testTag(PantryTestTags.NO_RESULTS_TITLE),
            )
            Text(
                stringResource(R.string.pantry_no_results_message),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = WarmSpacing.xs),
            )
            Button(
                onClick = onAdd,
                modifier = Modifier.padding(top = WarmSpacing.md).heightIn(min = 48.dp)
                    .testTag(PantryTestTags.NO_RESULTS_ADD),
            ) { Text(stringResource(R.string.pantry_no_results_add)) }
        }
    }
}

@Composable
private fun PantryItemCard(
    item: PantryItem,
    today: LocalDate,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onQuantityChange: (String, Int) -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val editLabel = stringResource(R.string.pantry_edit_accessibility)
    val deleteLabel = stringResource(R.string.pantry_delete_accessibility)
    val freshness = FreshnessRules.freshness(item.expiryDate, today)
    val quantity = item.quantity ?: PantryItemValidation.MIN_QUANTITY

    Surface(
        shape = RoundedCornerShape(WarmRadius.lg),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = Modifier.fillMaxWidth().testTag(PantryTestTags.ITEM_CARD),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(WarmSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(WarmSpacing.sm),
        ) {
            // Miniatura: icono genérico `Kitchen` en contenedor redondeado (D12).
            Surface(
                shape = RoundedCornerShape(WarmRadius.md),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.size(56.dp),
            ) {
                Icon(
                    Icons.Outlined.Kitchen,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(14.dp),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(WarmSpacing.xs),
                ) {
                    Text(
                        item.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).testTag(PantryTestTags.itemName(item.id)),
                    )
                    FreshnessPill(freshness, Modifier.testTag(PantryTestTags.itemFreshness(item.id)))
                }
                Text(
                    text = item.quantity?.let { stringResource(R.string.pantry_quantity_value, it) }
                        ?: stringResource(R.string.pantry_no_quantity),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.testTag(PantryTestTags.itemQuantity(item.id)),
                )
                Text(
                    text = expiryDisplayText(item, today),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag(PantryTestTags.itemExpiry(item.id)),
                )
            }
            // Controles de la fila: menú ⋮ arriba pegado a la derecha y stepper −/+
            // en la fila de abajo. Esta columna (48 + 48 dp) es lo más alto de la
            // tarjeta, así que el «Row» externo centrado la deja pegada a los bordes
            // superior e inferior de la tarjeta.
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box {
                    IconButton(
                        onClick = { menuOpen = true },
                        modifier = Modifier.size(48.dp)
                            .testTag(PantryTestTags.itemMenu(item.id)),
                    ) {
                        Icon(
                            Icons.Outlined.MoreVert,
                            contentDescription = stringResource(R.string.pantry_item_menu, item.name),
                        )
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.pantry_edit_accessibility)) },
                            onClick = { menuOpen = false; onEdit() },
                            leadingIcon = {
                                Icon(
                                    Icons.Outlined.Edit,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            },
                            modifier = Modifier.testTag(PantryTestTags.editItem(item.id)).semantics {
                                contentDescription = "$editLabel ${item.name}"
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.pantry_delete_accessibility)) },
                            onClick = { menuOpen = false; onDelete() },
                            leadingIcon = {
                                Icon(
                                    Icons.Outlined.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            },
                            modifier = Modifier.testTag(PantryTestTags.deleteItem(item.id)).semantics {
                                contentDescription = "$deleteLabel ${item.name}"
                            },
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onQuantityChange(item.id, quantity - 1) },
                        enabled = quantity > PantryItemValidation.MIN_QUANTITY,
                        modifier = Modifier.size(48.dp)
                            .testTag(PantryTestTags.itemQuantityDecrease(item.id)),
                    ) {
                        Icon(
                            Icons.Outlined.Remove,
                            contentDescription = stringResource(R.string.pantry_item_qty_decrease, item.name),
                        )
                    }
                    IconButton(
                        onClick = { onQuantityChange(item.id, quantity + 1) },
                        enabled = quantity < PantryItemValidation.MAX_QUANTITY,
                        modifier = Modifier.size(48.dp)
                            .testTag(PantryTestTags.itemQuantityIncrease(item.id)),
                    ) {
                        Icon(
                            Icons.Outlined.Add,
                            contentDescription = stringResource(R.string.pantry_item_qty_increase, item.name),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FreshnessPill(freshness: Freshness, modifier: Modifier = Modifier) {
    val expiring = freshness == Freshness.EXPIRING_SOON
    Surface(
        shape = RoundedCornerShape(50),
        color = if (expiring) MaterialTheme.colorScheme.errorContainer
        else MaterialTheme.colorScheme.secondaryContainer,
        contentColor = if (expiring) MaterialTheme.colorScheme.onErrorContainer
        else MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Text(
            stringResource(if (expiring) R.string.pantry_freshness_expiring else R.string.pantry_freshness_good),
            style = MaterialTheme.typography.labelSmall,
            // El testTag va en el `Text` (hoja con `Text`) para poder asertar su contenido.
            modifier = modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}

// Traduce el estado puro `ExpiryText` a los textos localizados (P19).
@Composable
private fun expiryDisplayText(item: PantryItem, today: LocalDate): String =
    when (val expiryText = FreshnessRules.expiryText(item.expiryDate, today)) {
        ExpiryText.LongShelf -> stringResource(R.string.pantry_expiry_long)
        ExpiryText.Expired -> stringResource(R.string.pantry_expiry_past)
        ExpiryText.Today -> stringResource(R.string.pantry_expiry_today)
        ExpiryText.Tomorrow -> stringResource(R.string.pantry_expiry_tomorrow)
        is ExpiryText.InDays -> pluralStringResource(
            R.plurals.pantry_expiry_days,
            expiryText.days,
            expiryText.days,
        )
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PantryItemDialog(
    item: PantryItem?,
    error: PantrySaveError?,
    onDismiss: () -> Unit,
    onSave: (String, Int, LocalDate?) -> Unit,
) {
    var name by remember(item?.id) { mutableStateOf(item?.name.orEmpty()) }
    var quantity by remember(item?.id) { mutableIntStateOf(item?.quantity ?: PantryItemValidation.MIN_QUANTITY) }
    var expiryDate by remember(item?.id) { mutableStateOf(item?.expiryDate) }
    var showDatePicker by remember { mutableStateOf(false) }
    val configuration = LocalConfiguration.current
    val dateFormatter = remember(configuration.locales) {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(configuration.locales[0])
    }

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
                // Fecha de caducidad opcional (D2): fila que abre el DatePicker de
                // Material 3 y botón «Quitar fecha» para volver a «sin fecha» (S5).
                Column {
                    Text(stringResource(R.string.pantry_expiry_label))
                    Row(
                        modifier = Modifier.fillMaxWidth().testTag(PantryTestTags.EXPIRY_ROW),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = expiryDate?.format(dateFormatter) ?: stringResource(R.string.pantry_expiry_none),
                            color = if (expiryDate == null) MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                                .clickable { showDatePicker = true }
                                .padding(vertical = WarmSpacing.xs)
                                .testTag(PantryTestTags.EXPIRY_VALUE),
                        )
                        if (expiryDate != null) {
                            TextButton(
                                onClick = { expiryDate = null },
                                modifier = Modifier.testTag(PantryTestTags.EXPIRY_CLEAR),
                            ) { Text(stringResource(R.string.pantry_expiry_clear)) }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, quantity, expiryDate) },
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

    if (showDatePicker) {
        // Selección inicial: la fecha existente o hoy; el confirmar persiste el día
        // elegido (la zona horaria del picker es UTC, igual que en la DB).
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = (expiryDate ?: LocalDate.now())
                .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            expiryDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        }
                        showDatePicker = false
                    },
                    modifier = Modifier.testTag(PantryTestTags.EXPIRY_CONFIRM),
                ) { Text(stringResource(R.string.pantry_expiry_confirm)) }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDatePicker = false },
                    modifier = Modifier.testTag(PantryTestTags.EXPIRY_CANCEL),
                ) { Text(stringResource(R.string.pantry_cancel)) }
            },
        ) {
            DatePicker(
                state = datePickerState,
                showModeToggle = false,
                modifier = Modifier.testTag(PantryTestTags.EXPIRY_PICKER),
            )
        }
    }
}
