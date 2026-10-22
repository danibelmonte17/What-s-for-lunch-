package com.whatsforlunch.app.ui.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whatsforlunch.app.data.scanner.FoodDetectionResult
import com.whatsforlunch.app.data.scanner.FoodDetector
import com.whatsforlunch.app.data.scanner.LiveFrameSource
import com.whatsforlunch.app.domain.model.DetectedFood
import com.whatsforlunch.app.domain.model.PantryItem
import com.whatsforlunch.app.domain.model.Scan
import com.whatsforlunch.app.domain.model.ScanDraftItem
import com.whatsforlunch.app.domain.repository.PantryDuplicateResolution
import com.whatsforlunch.app.domain.repository.PantryRepository
import com.whatsforlunch.app.domain.repository.PantrySaveResult
import com.whatsforlunch.app.domain.scanner.ScanDetections
import com.whatsforlunch.app.domain.text.normalizeName
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.launch

enum class ScannerNotice {
    CAMERA_PERMISSION_DENIED,
    CAMERA_UNAVAILABLE,
    DETECTION_FAILED,
    DUPLICATE_NAME,
    INVALID_NAME,
    SAVE_FAILED,
}

data class ScannerUiState(
    val cameraPermissionGranted: Boolean = false,
    val cameraPreviewReady: Boolean = false,
    val pantryLoaded: Boolean = false,
    val manualMode: Boolean = false,
    val isDetecting: Boolean = false,
    /** True while the live loop is stopped after a detector failure (D5) and a restart is offered. */
    val liveStopped: Boolean = false,
    val liveDetections: List<DetectedFood> = emptyList(),
    val scan: Scan? = null,
    val items: List<ScanDraftItem> = emptyList(),
    val pantryItems: List<PantryItem> = emptyList(),
    val matches: Map<String, PantryItem> = emptyMap(),
    val duplicateDecisions: Map<String, PantryDuplicateResolution> = emptyMap(),
    val activeDuplicateItemId: String? = null,
    val duplicatePromptQueue: List<String> = emptyList(),
    val suppressedAutoPrompts: Set<String> = emptySet(),
    val pendingSave: Boolean = false,
    val isSaving: Boolean = false,
    val showExitConfirmation: Boolean = false,
    val rescanAfterConfirmation: Boolean = false,
    val saveCompleted: Boolean = false,
    val notice: ScannerNotice? = null,
)

class ScannerViewModel(
    private val pantryRepository: PantryRepository,
    private val foodDetector: FoodDetector,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ScannerUiState())
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    private var detectionJob: Job? = null
    private var loopActive = false

    init {
        viewModelScope.launch {
            pantryRepository.observePantryItems().collect(::updatePantryItems)
        }
    }

    fun onCameraPermissionGranted() {
        _uiState.value = _uiState.value.copy(
            cameraPermissionGranted = true,
            manualMode = false,
            notice = null,
        )
    }

    fun onCameraPermissionDenied() {
        stopLiveDetection()
        _uiState.value = _uiState.value.copy(
            cameraPermissionGranted = false,
            cameraPreviewReady = false,
            manualMode = true,
            isDetecting = false,
            liveDetections = emptyList(),
            notice = ScannerNotice.CAMERA_PERMISSION_DENIED,
        )
    }

    fun onCameraPreviewReady() {
        _uiState.value = _uiState.value.copy(cameraPreviewReady = true, manualMode = false)
    }

    fun onCameraPreviewFailed() {
        stopLiveDetection()
        _uiState.value = _uiState.value.copy(
            cameraPreviewReady = false,
            manualMode = true,
            isDetecting = false,
            liveDetections = emptyList(),
            notice = ScannerNotice.CAMERA_UNAVAILABLE,
        )
    }

    /**
     * Starts the live detection loop: sampled frames are detected one after another and
     * accumulated into the tray (conflated: only the latest frame is pending at any time).
     * Idempotent while the loop is active; after a detector failure or «Re-escanear» it
     * starts a fresh loop.
     */
    fun startLiveDetection(frameSource: LiveFrameSource) {
        val state = _uiState.value
        if (!state.cameraPermissionGranted || !state.cameraPreviewReady) return
        if (loopActive) return
        loopActive = true
        _uiState.value = state.copy(liveStopped = false)
        detectionJob = viewModelScope.launch {
            try {
                frameSource.frames()
                    .conflate()
                    .collect { frame ->
                        detectFrame(frame)
                        delay(ScanDetections.LIVE_SAMPLE_INTERVAL_MS)
                    }
            } finally {
                loopActive = false
            }
        }
    }

    fun stopLiveDetection() {
        detectionJob?.cancel()
        detectionJob = null
        if (_uiState.value.isDetecting) {
            _uiState.value = _uiState.value.copy(isDetecting = false)
        }
    }

    override fun onCleared() {
        detectionJob?.cancel()
        detectionJob = null
        super.onCleared()
    }

    private suspend fun detectFrame(frame: ByteArray) {
        _uiState.value = _uiState.value.copy(isDetecting = true)
        val result = try {
            foodDetector.detect(frame)
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            FoodDetectionResult.Failed
        } catch (_: LinkageError) {
            FoodDetectionResult.Failed
        }
        when (result) {
            is FoodDetectionResult.Success -> {
                val eligible = ScanDetections.eligible(result.detections)
                val merged = ScanDetections.mergeLiveDetections(_uiState.value.items, eligible)
                val base = recalculateItems(_uiState.value, merged).copy(
                    isDetecting = false,
                    liveDetections = eligible,
                    scan = Scan(detections = eligible),
                )
                _uiState.value = base
            }

            FoodDetectionResult.Failed -> {
                // Cancels this very coroutine: with an eager dispatcher the loop job may not
                // be assigned yet, so cancelling only `detectionJob` would keep sampling.
                currentCoroutineContext().cancel()
                _uiState.value = _uiState.value.copy(
                    isDetecting = false,
                    manualMode = true,
                    liveStopped = true,
                    liveDetections = emptyList(),
                    notice = ScannerNotice.DETECTION_FAILED,
                )
            }
        }
    }

    fun addManualItem(name: String) {
        val cleanName = name.trim()
        if (cleanName.isEmpty()) return
        val state = _uiState.value
        val items = ScanDetections.mergeDraftItems(
            state.items + ScanDraftItem(name = cleanName, confidence = null),
        )
        _uiState.value = recalculateItems(state, items)
    }

    fun renameItem(id: String, name: String) {
        val state = _uiState.value
        val items = ScanDetections.rename(state.items, id, name)
        _uiState.value = recalculateItems(state, items, preferredId = id)
    }

    fun updateQuantity(id: String, quantity: Int) {
        val state = _uiState.value
        _uiState.value = state.copy(items = ScanDetections.updateQuantity(state.items, id, quantity))
    }

    fun removeItem(id: String) {
        val state = _uiState.value
        _uiState.value = recalculateItems(state, state.items.filterNot { it.id == id })
    }

    fun openDuplicateResolution(itemId: String) {
        if (_uiState.value.matches.containsKey(itemId)) {
            _uiState.value = _uiState.value.copy(activeDuplicateItemId = itemId)
        }
    }

    fun resolveDuplicate(itemId: String, resolution: PantryDuplicateResolution) {
        val state = _uiState.value
        if (itemId !in state.matches) return
        val decisions = state.duplicateDecisions + (itemId to resolution)
        val remainingQueue = state.duplicatePromptQueue.filterNot { it == itemId }
        val next = if (state.pendingSave) {
            state.items.firstOrNull { it.id in state.matches && it.id !in decisions }?.id
        } else {
            remainingQueue.firstOrNull { it in state.matches && it !in decisions }
        }
        _uiState.value = state.copy(
            duplicateDecisions = decisions,
            activeDuplicateItemId = next,
            duplicatePromptQueue = remainingQueue,
        )
        if (state.pendingSave && next == null) persistItems()
    }

    fun dismissDuplicateResolution() {
        val state = _uiState.value
        val itemId = state.activeDuplicateItemId ?: return
        if (state.pendingSave) {
            _uiState.value = state.copy(
                pendingSave = false,
                activeDuplicateItemId = null,
                duplicatePromptQueue = emptyList(),
            )
            return
        }
        val item = state.items.firstOrNull { it.id == itemId }
        val match = state.matches[itemId]
        val suppressed = if (item != null && match != null) {
            state.suppressedAutoPrompts + promptKey(item, match)
        } else {
            state.suppressedAutoPrompts
        }
        val queue = state.duplicatePromptQueue.filterNot { it == itemId }
        _uiState.value = state.copy(
            activeDuplicateItemId = queue.firstOrNull { it in state.matches && it !in state.duplicateDecisions },
            duplicatePromptQueue = queue,
            suppressedAutoPrompts = suppressed,
        )
    }

    fun saveToPantry() {
        val state = _uiState.value
        if (state.isSaving || state.items.isEmpty() || !state.pantryLoaded) return
        if (state.items.any { it.name.isBlank() }) {
            _uiState.value = state.copy(notice = ScannerNotice.INVALID_NAME)
            return
        }
        val pending = state.items.filter { it.id in state.matches && it.id !in state.duplicateDecisions }
        if (pending.isNotEmpty()) {
            val queue = pending.map(ScanDraftItem::id)
            _uiState.value = state.copy(
                pendingSave = true,
                duplicatePromptQueue = queue,
                activeDuplicateItemId = queue.first(),
            )
        } else {
            persistItems()
        }
    }

    private fun persistItems() {
        val snapshot = _uiState.value
        if (snapshot.isSaving || snapshot.pendingSave && snapshot.activeDuplicateItemId != null) return
        _uiState.value = snapshot.copy(isSaving = true, pendingSave = false, activeDuplicateItemId = null)
        viewModelScope.launch {
            try {
                snapshot.items.forEach { draft ->
                    val existing = snapshot.matches[draft.id]
                    val result = when {
                        existing == null -> pantryRepository.savePantryItem(ScanDetections.toPantryItem(draft))
                        snapshot.duplicateDecisions[draft.id] == PantryDuplicateResolution.KeepExisting -> PantrySaveResult.Saved
                        snapshot.duplicateDecisions[draft.id] == PantryDuplicateResolution.UpdateQuantity -> {
                            val item = ScanDetections.resolveDuplicate(
                                existing,
                                draft,
                                PantryDuplicateResolution.UpdateQuantity,
                            ) ?: existing
                            pantryRepository.savePantryItem(item)
                        }
                        else -> PantrySaveResult.DuplicateName
                    }
                    if (result == PantrySaveResult.DuplicateName) {
                        _uiState.value = _uiState.value.copy(
                            isSaving = false,
                            notice = ScannerNotice.DUPLICATE_NAME,
                        )
                        return@launch
                    }
                }
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    saveCompleted = true,
                    scan = null,
                    liveDetections = emptyList(),
                    items = emptyList(),
                    matches = emptyMap(),
                    duplicateDecisions = emptyMap(),
                    duplicatePromptQueue = emptyList(),
                    suppressedAutoPrompts = emptySet(),
                    notice = null,
                )
            } catch (exception: Exception) {
                if (exception is CancellationException) throw exception
                _uiState.value = _uiState.value.copy(isSaving = false, notice = ScannerNotice.SAVE_FAILED)
            }
        }
    }

    fun consumeSaveCompleted() {
        _uiState.value = _uiState.value.copy(saveCompleted = false)
    }

    /** Returns true when the route can leave immediately; false means show confirmation. */
    fun requestExit(): Boolean {
        val state = _uiState.value
        if (state.items.isEmpty()) {
            clearTemporaryScan()
            return true
        }
        _uiState.value = state.copy(showExitConfirmation = true, rescanAfterConfirmation = false)
        return false
    }

    /** Returns true when confirmation should restart the live scan instead of leaving. */
    fun confirmExit(): Boolean {
        val shouldRescan = _uiState.value.rescanAfterConfirmation
        clearTemporaryScan()
        return shouldRescan
    }

    fun requestRescan(): Boolean {
        val state = _uiState.value
        if (state.items.isNotEmpty()) {
            _uiState.value = state.copy(showExitConfirmation = true, rescanAfterConfirmation = true)
            return false
        }
        clearTemporaryScan()
        return true
    }

    fun cancelExitConfirmation() {
        _uiState.value = _uiState.value.copy(showExitConfirmation = false, rescanAfterConfirmation = false)
    }

    fun clearNotice() {
        _uiState.value = _uiState.value.copy(notice = null)
    }

    private fun clearTemporaryScan() {
        val state = _uiState.value
        _uiState.value = state.copy(
            showExitConfirmation = false,
            rescanAfterConfirmation = false,
            scan = null,
            items = emptyList(),
            matches = emptyMap(),
            duplicateDecisions = emptyMap(),
            activeDuplicateItemId = null,
            duplicatePromptQueue = emptyList(),
            suppressedAutoPrompts = emptySet(),
            pendingSave = false,
            notice = null,
        )
    }

    private fun updatePantryItems(pantryItems: List<PantryItem>) {
        val state = _uiState.value
        val matches = ScanDetections.matchesWithPantry(state.items, pantryItems)
        val base = state.copy(pantryItems = pantryItems, matches = matches, pantryLoaded = true)
        _uiState.value = queueNewAutomaticMatches(base, state.matches)
    }

    private fun recalculateItems(
        state: ScannerUiState,
        items: List<ScanDraftItem>,
        preferredId: String? = null,
    ): ScannerUiState {
        val matches = ScanDetections.matchesWithPantry(items, state.pantryItems)
        val decisions = state.duplicateDecisions.filter { (id, _) ->
            val previous = state.items.firstOrNull { it.id == id }
            val current = items.firstOrNull { it.id == id }
            previous != null && current != null &&
                normalizeName(previous.name) == normalizeName(current.name) &&
                state.matches[id]?.id == matches[id]?.id
        }
        val base = state.copy(items = items, matches = matches, duplicateDecisions = decisions)
        return queueNewAutomaticMatches(base, state.matches, preferredId)
    }

    private fun queueNewAutomaticMatches(
        state: ScannerUiState,
        previousMatches: Map<String, PantryItem>,
        preferredId: String? = null,
    ): ScannerUiState {
        val newMatches = state.items.filter { item ->
            val match = state.matches[item.id]
            match != null && previousMatches[item.id]?.id != match.id && item.id !in state.duplicateDecisions &&
                promptKey(item, match) !in state.suppressedAutoPrompts
        }.map(ScanDraftItem::id)
        val queue = (state.duplicatePromptQueue + newMatches).distinct()
            .filter { it in state.matches && it !in state.duplicateDecisions }
        val active = state.activeDuplicateItemId?.takeIf { it in state.matches }
            ?: preferredId?.takeIf { it in queue }
            ?: queue.firstOrNull()
        return state.copy(duplicatePromptQueue = queue, activeDuplicateItemId = active)
    }

    private fun promptKey(item: ScanDraftItem, pantryItem: PantryItem): String =
        "${item.id}:${normalizeName(item.name)}:${pantryItem.id}"
}
