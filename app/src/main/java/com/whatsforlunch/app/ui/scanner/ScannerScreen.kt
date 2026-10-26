package com.whatsforlunch.app.ui.scanner

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.whatsforlunch.app.R
import com.whatsforlunch.app.domain.model.PantryItem
import com.whatsforlunch.app.domain.model.ScanDraftItem
import com.whatsforlunch.app.domain.pantry.PantryItemValidation
import com.whatsforlunch.app.ui.theme.WarmRadius
import com.whatsforlunch.app.ui.theme.WarmSpacing

object ScannerTestTags {
    const val SCREEN = "scanner_screen"
    const val BACK = "scanner_back"
    const val SCANNER_TOGGLE = "scanner_toggle"
    const val CAMERA_SECTION = "scanner_camera_section"
    const val CAMERA_CLOSE = "scanner_camera_close"
    const val CAMERA_PREVIEW = "scanner_camera_preview"
    const val CAPTURE = "scanner_capture"
    const val CAPTURE_PROGRESS = "scanner_capture_progress"
    const val FOOD_LIST = "scanner_food_list"
    const val FOOD_ITEM = "scanner_food_item"
    const val ITEM_NAME_PREFIX = "scanner_item_name"
    const val ITEM_QUANTITY_PREFIX = "scanner_item_quantity"
    const val QUANTITY_DECREMENT_PREFIX = "scanner_quantity_decrement"
    const val QUANTITY_INCREMENT_PREFIX = "scanner_quantity_increment"
    const val REMOVE_ITEM_PREFIX = "scanner_remove_item"
    const val DUPLICATE_MARK_PREFIX = "scanner_duplicate_mark"
    const val MANUAL_NAME = "scanner_manual_name"
    const val MANUAL_ADD = "scanner_manual_add"
    const val SAVE = "scanner_save"
    const val EXIT_DIALOG = "scanner_exit_dialog"
    const val EXIT_CANCEL = "scanner_exit_cancel"
    const val EXIT_CONFIRM = "scanner_exit_confirm"

    fun itemName(id: String) = "${ITEM_NAME_PREFIX}_$id"
    fun itemQuantity(id: String) = "${ITEM_QUANTITY_PREFIX}_$id"
    fun quantityDecrement(id: String) = "${QUANTITY_DECREMENT_PREFIX}_$id"
    fun quantityIncrement(id: String) = "${QUANTITY_INCREMENT_PREFIX}_$id"
    fun removeItem(id: String) = "${REMOVE_ITEM_PREFIX}_$id"
    fun duplicateMark(id: String) = "${DUPLICATE_MARK_PREFIX}_$id"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(
    uiState: ScannerUiState,
    previewContent: @Composable () -> Unit,
    onBack: () -> Unit,
    onOpenScanner: () -> Unit,
    onCloseScanner: () -> Unit,
    onCapture: () -> Unit,
    onRequestExit: () -> Boolean,
    onConfirmExit: () -> Unit,
    onCancelExit: () -> Unit,
    onAddManualItem: (String) -> Unit,
    onRenameItem: (String, String) -> Unit,
    onQuantityChange: (String, Int) -> Unit,
    onRemoveItem: (String) -> Unit,
    onSave: () -> Unit,
    onClearNotice: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var manualName by rememberSaveable { mutableStateOf("") }

    BackHandler(enabled = !uiState.showExitConfirmation) {
        if (onRequestExit()) onBack()
    }

    val requestExit = {
        if (onRequestExit()) onBack()
    }

    Scaffold(
        modifier = modifier.testTag(ScannerTestTags.SCREEN),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.scanner_title)) },
                navigationIcon = {
                    TextButton(onClick = requestExit, modifier = Modifier.heightIn(min = 48.dp).testTag(ScannerTestTags.BACK)) {
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
            // Solo ocupa altura cuando hay Alimentos que guardar; el fondo del
            // Escáner sigue llegando hasta el borde inferior.
            if (uiState.items.isNotEmpty()) {
                ScannerActions(uiState = uiState, onSave = onSave)
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(WarmSpacing.sm),
        ) {
            uiState.notice?.let { notice ->
                NoticeBanner(notice = notice, onDismiss = onClearNotice)
            }

            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = WarmSpacing.md)
                    .testTag(ScannerTestTags.FOOD_LIST),
                contentPadding = PaddingValues(vertical = WarmSpacing.md),
                verticalArrangement = Arrangement.spacedBy(WarmSpacing.md),
            ) {
                item(key = "manual_entry") {
                    ManualFoodEntry(
                        name = manualName,
                        onNameChange = { manualName = it },
                        onAdd = {
                            if (manualName.isNotBlank()) {
                                onAddManualItem(manualName)
                                manualName = ""
                            }
                        },
                    )
                    if (!uiState.isScannerExpanded) {
                        OutlinedButton(
                            onClick = onOpenScanner,
                            enabled = !uiState.isAnalyzing && !uiState.isSaving,
                            modifier = Modifier.fillMaxWidth().padding(top = WarmSpacing.sm).heightIn(min = 52.dp)
                                .testTag(ScannerTestTags.SCANNER_TOGGLE),
                        ) {
                            Icon(Icons.Outlined.CameraAlt, contentDescription = null)
                            Text(
                                stringResource(R.string.scanner_open_camera),
                                modifier = Modifier.padding(start = WarmSpacing.xs),
                            )
                        }
                    }
                    AnimatedVisibility(
                        visible = uiState.isScannerExpanded,
                        enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
                        exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top),
                    ) {
                        ScannerCameraPanel(
                            uiState = uiState,
                            previewContent = previewContent,
                            onCapture = onCapture,
                            onClose = onCloseScanner,
                        )
                    }
                }

                if (uiState.items.isNotEmpty()) {
                    items(uiState.items, key = ScanDraftItem::id) { item ->
                        ScannerFoodCard(
                            item = item,
                            match = uiState.matches[item.id],
                            onNameChange = { onRenameItem(item.id, it) },
                            onQuantityChange = { onQuantityChange(item.id, it) },
                            onRemove = { onRemoveItem(item.id) },
                            modifier = Modifier.animateItem(),
                        )
                    }
                } else {
                    item(key = "empty_hint") {
                        Text(
                            stringResource(R.string.scanner_capture_hint),
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }

    if (uiState.showExitConfirmation) {
        ExitConfirmationDialog(
            onConfirm = onConfirmExit,
            onDismiss = onCancelExit,
        )
    }
}

@Composable
private fun ScannerCameraPanel(
    uiState: ScannerUiState,
    previewContent: @Composable () -> Unit,
    onCapture: () -> Unit,
    onClose: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(top = WarmSpacing.md).testTag(ScannerTestTags.CAMERA_SECTION),
        shape = RoundedCornerShape(WarmRadius.lg),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Column(
            modifier = Modifier.padding(WarmSpacing.md),
            verticalArrangement = Arrangement.spacedBy(WarmSpacing.md),
        ) {
            if (uiState.cameraPreviewReady) {
                Box(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 220.dp, max = 300.dp)
                        .clip(RoundedCornerShape(WarmRadius.lg)),
                ) {
                    previewContent()
                }
            } else {
                CameraPlaceholder(uiState, modifier = Modifier.fillMaxWidth())
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(WarmSpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = onCapture,
                    enabled = uiState.cameraPreviewReady && !uiState.isAnalyzing && !uiState.isSaving,
                    modifier = Modifier.weight(1f).heightIn(min = 52.dp).testTag(ScannerTestTags.CAPTURE),
                ) {
                    Icon(Icons.Outlined.CameraAlt, contentDescription = null)
                    Text(stringResource(R.string.scanner_capture), modifier = Modifier.padding(start = WarmSpacing.xs))
                }
                IconButton(
                    onClick = onClose,
                    enabled = !uiState.isAnalyzing && !uiState.isSaving,
                    modifier = Modifier.size(48.dp).testTag(ScannerTestTags.CAMERA_CLOSE),
                ) {
                    Icon(
                        Icons.Outlined.Close,
                        contentDescription = stringResource(R.string.scanner_close_camera),
                    )
                }
            }

            if (uiState.isAnalyzing) {
                Row(
                    modifier = Modifier.fillMaxWidth().testTag(ScannerTestTags.CAPTURE_PROGRESS),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    Text(
                        stringResource(R.string.scanner_analyzing),
                        modifier = Modifier.padding(start = WarmSpacing.sm),
                    )
                }
            }
        }
    }
}

@Composable
private fun CameraPlaceholder(uiState: ScannerUiState, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.heightIn(min = 180.dp).clip(RoundedCornerShape(WarmRadius.lg))
            .background(MaterialTheme.colorScheme.surfaceContainer).padding(WarmSpacing.lg),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Outlined.CameraAlt, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
        Text(
            stringResource(if (uiState.manualMode) R.string.scanner_camera_unavailable else R.string.scanner_camera_loading),
            modifier = Modifier.padding(top = WarmSpacing.sm),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun NoticeBanner(notice: ScannerNotice, onDismiss: () -> Unit) {
    val message = when (notice) {
        ScannerNotice.CAMERA_PERMISSION_DENIED -> stringResource(R.string.scanner_camera_denied)
        ScannerNotice.CAMERA_UNAVAILABLE -> stringResource(R.string.scanner_camera_unavailable)
        ScannerNotice.ANALYSIS_FAILED -> stringResource(R.string.scanner_analysis_failed)
        ScannerNotice.ANALYSIS_NO_CONNECTION -> stringResource(R.string.scanner_analysis_failed_no_connection)
        ScannerNotice.ANALYSIS_PROVIDER_ERROR -> stringResource(R.string.scanner_analysis_failed_provider)
        ScannerNotice.ANALYSIS_INVALID_RESPONSE -> stringResource(R.string.scanner_analysis_failed_invalid_response)
        ScannerNotice.AI_KEY_MISSING -> stringResource(R.string.scanner_ai_key_missing)
        ScannerNotice.NO_DETECTIONS -> stringResource(R.string.scanner_no_detections)
        ScannerNotice.DUPLICATE_NAME -> stringResource(R.string.scanner_duplicate_error)
        ScannerNotice.INVALID_NAME -> stringResource(R.string.scanner_name_invalid)
        ScannerNotice.SAVE_FAILED -> stringResource(R.string.scanner_save_failed)
    }
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = WarmSpacing.md),
        color = if (notice == ScannerNotice.SAVE_FAILED) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.tertiaryContainer,
        shape = RoundedCornerShape(WarmRadius.md),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = WarmSpacing.md, end = WarmSpacing.xs, top = WarmSpacing.sm, bottom = WarmSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(message, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface)
            IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.action_dismiss))
            }
        }
    }
}

@Composable
private fun ManualFoodEntry(
    name: String,
    onNameChange: (String) -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(WarmSpacing.sm)) {
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            modifier = Modifier.fillMaxWidth().testTag(ScannerTestTags.MANUAL_NAME),
            label = { Text(stringResource(R.string.scanner_manual_name)) },
            singleLine = true,
        )
        Button(
            onClick = onAdd,
            enabled = name.isNotBlank(),
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                .testTag(ScannerTestTags.MANUAL_ADD),
        ) {
            Icon(Icons.Outlined.Add, contentDescription = null)
            Text(stringResource(R.string.scanner_add_food), modifier = Modifier.padding(start = WarmSpacing.xs))
        }
    }
}

@Composable
private fun ScannerFoodCard(
    item: ScanDraftItem,
    match: PantryItem?,
    onNameChange: (String) -> Unit,
    onQuantityChange: (Int) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(WarmRadius.lg)
    Surface(
        modifier = modifier.fillMaxWidth().clip(shape).testTag(ScannerTestTags.FOOD_ITEM),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Column(modifier = Modifier.padding(WarmSpacing.md), verticalArrangement = Arrangement.spacedBy(WarmSpacing.sm)) {
            Row(horizontalArrangement = Arrangement.spacedBy(WarmSpacing.xs), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = item.name,
                    onValueChange = onNameChange,
                    modifier = Modifier.weight(1f).testTag(ScannerTestTags.itemName(item.id)),
                    label = { Text(stringResource(R.string.scanner_item_name)) },
                    singleLine = true,
                )
                IconButton(onClick = onRemove, modifier = Modifier.size(48.dp).testTag(ScannerTestTags.removeItem(item.id))) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = stringResource(R.string.scanner_remove_food),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.scanner_quantity), modifier = Modifier.weight(1f))
                IconButton(
                    onClick = { onQuantityChange(item.quantity - 1) },
                    enabled = item.quantity > PantryItemValidation.MIN_QUANTITY,
                    modifier = Modifier.size(48.dp).testTag(ScannerTestTags.quantityDecrement(item.id)),
                ) { Icon(Icons.Outlined.Remove, contentDescription = stringResource(R.string.scanner_quantity_decrease)) }
                Text(
                    item.quantity.toString(),
                    modifier = Modifier.width(40.dp).testTag(ScannerTestTags.itemQuantity(item.id)),
                    textAlign = TextAlign.Center,
                )
                IconButton(
                    onClick = { onQuantityChange(item.quantity + 1) },
                    enabled = item.quantity < PantryItemValidation.MAX_QUANTITY,
                    modifier = Modifier.size(48.dp).testTag(ScannerTestTags.quantityIncrement(item.id)),
                ) { Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.scanner_quantity_increase)) }
            }
            if (match != null) {
                Text(
                    stringResource(R.string.scanner_already_in_pantry),
                    modifier = Modifier.testTag(ScannerTestTags.duplicateMark(item.id)),
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }
    }
}

@Composable
private fun ScannerActions(
    uiState: ScannerUiState,
    onSave: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            // El fondo llega hasta el borde; las acciones quedan por encima de
            // la barra de navegación del sistema.
            .navigationBarsPadding()
            .padding(horizontal = WarmSpacing.md, vertical = WarmSpacing.md),
        verticalArrangement = Arrangement.spacedBy(WarmSpacing.xs),
    ) {
        Button(
            onClick = onSave,
            enabled = uiState.pantryLoaded && !uiState.isAnalyzing && !uiState.isSaving && uiState.items.all { it.name.isNotBlank() },
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag(ScannerTestTags.SAVE),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        ) {
            Text(stringResource(R.string.scanner_save))
        }
    }
}

@Composable
private fun ExitConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val title = stringResource(R.string.scanner_exit_title)
    val message = stringResource(R.string.scanner_exit_message)
    val confirm = stringResource(R.string.scanner_exit_confirm)
    val cancel = stringResource(R.string.scanner_cancel)
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag(ScannerTestTags.EXIT_DIALOG),
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                modifier = Modifier.heightIn(min = 48.dp).testTag(ScannerTestTags.EXIT_CONFIRM),
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) { Text(confirm) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp).testTag(ScannerTestTags.EXIT_CANCEL)) {
                Text(cancel)
            }
        },
    )
}
