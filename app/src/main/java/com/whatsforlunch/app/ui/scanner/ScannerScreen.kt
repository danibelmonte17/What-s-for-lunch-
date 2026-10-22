package com.whatsforlunch.app.ui.scanner

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.drawscope.Stroke
import com.whatsforlunch.app.R
import com.whatsforlunch.app.domain.model.DetectedFood
import com.whatsforlunch.app.domain.model.PantryItem
import com.whatsforlunch.app.domain.model.ScanDraftItem
import com.whatsforlunch.app.domain.pantry.PantryItemValidation
import com.whatsforlunch.app.domain.repository.PantryDuplicateResolution
import com.whatsforlunch.app.ui.theme.WarmRadius
import com.whatsforlunch.app.ui.theme.WarmSpacing
import kotlin.math.roundToInt

object ScannerTestTags {
    const val SCREEN = "scanner_screen"
    const val BACK = "scanner_back"
    const val CAMERA_PREVIEW = "scanner_camera_preview"
    const val LIVE_HUD = "scanner_live_hud"
    const val HUD_LABEL_PREFIX = "scanner_hud_label"
    const val CAPTURE_PROGRESS = "scanner_capture_progress"
    const val FOOD_LIST = "scanner_food_list"
    const val FOOD_ITEM = "scanner_food_item"
    const val ITEM_NAME_PREFIX = "scanner_item_name"
    const val ITEM_CONFIDENCE_PREFIX = "scanner_item_confidence"
    const val ITEM_QUANTITY_PREFIX = "scanner_item_quantity"
    const val QUANTITY_DECREMENT_PREFIX = "scanner_quantity_decrement"
    const val QUANTITY_INCREMENT_PREFIX = "scanner_quantity_increment"
    const val REMOVE_ITEM_PREFIX = "scanner_remove_item"
    const val DUPLICATE_MARK_PREFIX = "scanner_duplicate_mark"
    const val MANUAL_NAME = "scanner_manual_name"
    const val MANUAL_ADD = "scanner_manual_add"
    const val SAVE = "scanner_save"
    const val RESCAN = "scanner_rescan"
    const val EXIT_DIALOG = "scanner_exit_dialog"
    const val EXIT_CANCEL = "scanner_exit_cancel"
    const val EXIT_CONFIRM = "scanner_exit_confirm"
    const val DUPLICATE_DIALOG = "scanner_duplicate_dialog"
    const val DUPLICATE_TITLE = "scanner_duplicate_title"
    const val DUPLICATE_MESSAGE = "scanner_duplicate_message"
    const val DUPLICATE_KEEP = "scanner_duplicate_keep"
    const val DUPLICATE_UPDATE = "scanner_duplicate_update"

    fun hudLabel(index: Int) = "${HUD_LABEL_PREFIX}_$index"
    fun itemName(id: String) = "${ITEM_NAME_PREFIX}_$id"
    fun itemConfidence(id: String) = "${ITEM_CONFIDENCE_PREFIX}_$id"
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
    onRescan: () -> Unit,
    onRequestExit: () -> Boolean,
    onConfirmExit: () -> Unit,
    onCancelExit: () -> Unit,
    onAddManualItem: (String) -> Unit,
    onRenameItem: (String, String) -> Unit,
    onQuantityChange: (String, Int) -> Unit,
    onRemoveItem: (String) -> Unit,
    onSave: () -> Unit,
    onOpenDuplicate: (String) -> Unit,
    onResolveDuplicate: (String, PantryDuplicateResolution) -> Unit,
    onDismissDuplicate: () -> Unit,
    onClearNotice: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(enabled = !uiState.showExitConfirmation && uiState.activeDuplicateItemId == null) {
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
            ScannerActions(
                uiState = uiState,
                onRescan = onRescan,
                onSave = onSave,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(WarmSpacing.sm),
        ) {
            if (uiState.cameraPreviewReady) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = WarmSpacing.md)
                        .heightIn(min = 220.dp, max = 300.dp).clip(RoundedCornerShape(WarmRadius.lg)),
                ) {
                    previewContent()
                    LiveDetectionHud(detections = uiState.liveDetections)
                }
            } else {
                CameraPlaceholder(uiState, modifier = Modifier.fillMaxWidth().padding(horizontal = WarmSpacing.md))
            }

            if (uiState.isDetecting) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = WarmSpacing.md).testTag(ScannerTestTags.CAPTURE_PROGRESS),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    Text(
                        stringResource(R.string.scanner_detection_loading),
                        modifier = Modifier.padding(start = WarmSpacing.sm),
                    )
                }
            }

            uiState.notice?.let { notice ->
                NoticeBanner(notice = notice, onDismiss = onClearNotice)
            }

            if (uiState.items.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = WarmSpacing.md)
                        .testTag(ScannerTestTags.FOOD_LIST),
                    contentPadding = PaddingValues(vertical = WarmSpacing.xs),
                    verticalArrangement = Arrangement.spacedBy(WarmSpacing.sm),
                ) {
                    items(uiState.items, key = ScanDraftItem::id) { item ->
                        ScannerFoodCard(
                            item = item,
                            match = uiState.matches[item.id],
                            onNameChange = { onRenameItem(item.id, it) },
                            onQuantityChange = { onQuantityChange(item.id, it) },
                            onRemove = { onRemoveItem(item.id) },
                            onOpenDuplicate = { onOpenDuplicate(item.id) },
                        )
                    }
                }
            } else if (uiState.cameraPreviewReady && !uiState.liveStopped) {
                Text(
                    stringResource(R.string.scanner_live_hint),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = WarmSpacing.md),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            ManualFoodEntry(
                modifier = Modifier.fillMaxWidth().padding(horizontal = WarmSpacing.md),
                onAdd = onAddManualItem,
            )
        }
    }

    val activeId = uiState.activeDuplicateItemId
    val activeItem = activeId?.let { id -> uiState.items.firstOrNull { it.id == id } }
    val pantryMatch = activeId?.let(uiState.matches::get)
    if (activeId != null && activeItem != null && pantryMatch != null) {
        DuplicateResolutionDialog(
            item = activeItem,
            existing = pantryMatch,
            onKeep = { onResolveDuplicate(activeId, PantryDuplicateResolution.KeepExisting) },
            onUpdate = { onResolveDuplicate(activeId, PantryDuplicateResolution.UpdateQuantity) },
            onDismiss = onDismissDuplicate,
        )
    }

    if (uiState.showExitConfirmation) {
        ExitConfirmationDialog(
            isRescan = uiState.rescanAfterConfirmation,
            onConfirm = onConfirmExit,
            onDismiss = onCancelExit,
        )
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

/** Live detection HUD: bounding boxes plus pill labels drawn over the camera preview. */
@Composable
private fun LiveDetectionHud(detections: List<DetectedFood>, modifier: Modifier = Modifier) {
    if (detections.isEmpty()) return
    val boxColor = MaterialTheme.colorScheme.primary
    Box(modifier = modifier.fillMaxSize().testTag(ScannerTestTags.LIVE_HUD)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            detections.forEach { detection ->
                val bounds = detection.boundingBox ?: return@forEach
                val left = (bounds.left.toFloat() * size.width).coerceIn(0f, size.width)
                val top = (bounds.top.toFloat() * size.height).coerceIn(0f, size.height)
                val right = (bounds.right.toFloat() * size.width).coerceIn(left, size.width)
                val bottom = (bounds.bottom.toFloat() * size.height).coerceIn(top, size.height)
                drawRect(
                    color = boxColor,
                    topLeft = Offset(left, top),
                    size = Size(right - left, bottom - top),
                    style = Stroke(width = 2.dp.toPx()),
                )
            }
        }
        Column(
            modifier = Modifier.align(Alignment.TopStart).padding(WarmSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            detections.forEachIndexed { index, detection ->
                val confidence = (detection.confidence * 100).roundToInt()
                val accessibilityDescription = stringResource(R.string.scanner_confidence_accessibility, confidence)
                Text(
                    stringResource(R.string.scanner_hud_detection, detection.name, confidence),
                    modifier = Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.72f))
                        .padding(horizontal = WarmSpacing.sm, vertical = 4.dp)
                        .testTag(ScannerTestTags.hudLabel(index))
                        .semantics { contentDescription = accessibilityDescription },
                    color = MaterialTheme.colorScheme.inverseOnSurface,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Composable
private fun NoticeBanner(notice: ScannerNotice, onDismiss: () -> Unit) {
    val message = when (notice) {
        ScannerNotice.CAMERA_PERMISSION_DENIED -> stringResource(R.string.scanner_camera_denied)
        ScannerNotice.CAMERA_UNAVAILABLE -> stringResource(R.string.scanner_camera_unavailable)
        ScannerNotice.DETECTION_FAILED -> stringResource(R.string.scanner_detection_failed)
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
            modifier = Modifier.fillMaxWidth().padding(start = WarmSpacing.md, end = WarmSpacing.xs, top = WarmSpacing.xs, bottom = WarmSpacing.xs),
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
private fun ManualFoodEntry(modifier: Modifier = Modifier, onAdd: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    Column(modifier = modifier.padding(bottom = WarmSpacing.sm)) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            modifier = Modifier.fillMaxWidth().testTag(ScannerTestTags.MANUAL_NAME),
            label = { Text(stringResource(R.string.scanner_manual_name)) },
            singleLine = true,
        )
        Button(
            onClick = { onAdd(name); name = "" },
            enabled = name.isNotBlank(),
            modifier = Modifier.fillMaxWidth().padding(top = WarmSpacing.xs).heightIn(min = 48.dp)
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
    onOpenDuplicate: () -> Unit,
) {
    val shape = RoundedCornerShape(WarmRadius.lg)
    Surface(
        modifier = Modifier.fillMaxWidth().clip(shape).testTag(ScannerTestTags.FOOD_ITEM),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Column(modifier = Modifier.padding(WarmSpacing.sm), verticalArrangement = Arrangement.spacedBy(WarmSpacing.xs)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = item.name,
                    onValueChange = onNameChange,
                    modifier = Modifier.weight(1f).testTag(ScannerTestTags.itemName(item.id)),
                    label = { Text(stringResource(R.string.scanner_item_name)) },
                    singleLine = true,
                )
                IconButton(onClick = onRemove, modifier = Modifier.size(48.dp).testTag(ScannerTestTags.removeItem(item.id))) {
                    Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.scanner_remove_food))
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                item.confidence?.let { score ->
                    val confidence = (score * 100).roundToInt()
                    Text(
                        stringResource(R.string.scanner_confidence, confidence),
                        modifier = Modifier.testTag(ScannerTestTags.itemConfidence(item.id)),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (match != null) {
                    Spacer(Modifier.weight(1f))
                    TextButton(
                        onClick = onOpenDuplicate,
                        modifier = Modifier.heightIn(min = 48.dp).testTag(ScannerTestTags.duplicateMark(item.id)),
                    ) { Text(stringResource(R.string.scanner_already_in_pantry), color = MaterialTheme.colorScheme.secondary) }
                }
            }
        }
    }
}

@Composable
private fun ScannerActions(
    uiState: ScannerUiState,
    onRescan: () -> Unit,
    onSave: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(horizontal = WarmSpacing.md, vertical = WarmSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(WarmSpacing.xs),
    ) {
        if (uiState.items.isNotEmpty()) {
            Button(
                onClick = onSave,
                enabled = uiState.pantryLoaded && !uiState.isDetecting && !uiState.isSaving && uiState.items.all { it.name.isNotBlank() },
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag(ScannerTestTags.SAVE),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) {
                Text(stringResource(R.string.scanner_save))
            }
        }
        // Shown with an empty list too, so a stopped loop (D5) can always be restarted.
        if (uiState.items.isNotEmpty() || (uiState.liveStopped && uiState.cameraPreviewReady)) {
            TextButton(
                onClick = onRescan,
                enabled = !uiState.isDetecting && !uiState.isSaving,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag(ScannerTestTags.RESCAN),
            ) { Text(stringResource(R.string.scanner_rescan)) }
        }
    }
}

@Composable
private fun DuplicateResolutionDialog(
    item: ScanDraftItem,
    existing: PantryItem,
    onKeep: () -> Unit,
    onUpdate: () -> Unit,
    onDismiss: () -> Unit,
) {
    val existingQuantity = existing.quantity?.let { stringResource(R.string.scanner_duplicate_existing_quantity, it) }
        ?: stringResource(R.string.scanner_no_quantity)
    val title = stringResource(R.string.scanner_duplicate_title)
    val message = stringResource(R.string.scanner_duplicate_message, item.name, existingQuantity)
    val keepLabel = stringResource(R.string.scanner_duplicate_keep)
    val updateLabel = stringResource(R.string.scanner_duplicate_update)
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag(ScannerTestTags.DUPLICATE_DIALOG),
        title = { Text(title, modifier = Modifier.testTag(ScannerTestTags.DUPLICATE_TITLE)) },
        text = {
            Text(
                message,
                modifier = Modifier.testTag(ScannerTestTags.DUPLICATE_MESSAGE),
            )
        },
        confirmButton = {
            TextButton(onClick = onKeep, modifier = Modifier.heightIn(min = 48.dp).testTag(ScannerTestTags.DUPLICATE_KEEP)) {
                Text(keepLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onUpdate, modifier = Modifier.heightIn(min = 48.dp).testTag(ScannerTestTags.DUPLICATE_UPDATE)) {
                Text(updateLabel)
            }
        },
    )
}

@Composable
private fun ExitConfirmationDialog(
    isRescan: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val title = stringResource(if (isRescan) R.string.scanner_rescan_title else R.string.scanner_exit_title)
    val message = stringResource(if (isRescan) R.string.scanner_rescan_message else R.string.scanner_exit_message)
    val confirm = stringResource(if (isRescan) R.string.scanner_rescan_confirm else R.string.scanner_exit_confirm)
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
