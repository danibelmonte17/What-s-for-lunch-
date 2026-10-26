package com.whatsforlunch.app.ui.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whatsforlunch.app.data.ai.AiFailure
import com.whatsforlunch.app.data.ai.AiFailureCause
import com.whatsforlunch.app.data.ai.ScanAnalysisResult
import com.whatsforlunch.app.data.ai.ScanImageAnalyzer
import com.whatsforlunch.app.data.scanner.ScanDraftHolder
import com.whatsforlunch.app.data.scanner.ScanImageSource
import com.whatsforlunch.app.domain.model.PantryItem
import com.whatsforlunch.app.domain.model.ScanDraftItem
import com.whatsforlunch.app.domain.repository.PantryRepository
import com.whatsforlunch.app.domain.repository.PantrySaveResult
import com.whatsforlunch.app.domain.scanner.ScanAnalysis
import com.whatsforlunch.app.domain.scanner.ScanDetections
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

enum class ScannerNotice {
    CAMERA_PERMISSION_DENIED,
    CAMERA_UNAVAILABLE,
    ANALYSIS_FAILED,
    ANALYSIS_NO_CONNECTION,
    ANALYSIS_PROVIDER_ERROR,
    ANALYSIS_INVALID_RESPONSE,
    AI_KEY_MISSING,
    NO_DETECTIONS,
    DUPLICATE_NAME,
    INVALID_NAME,
    SAVE_FAILED,
}

data class ScannerUiState(
    val cameraPermissionGranted: Boolean = false,
    val cameraPreviewReady: Boolean = false,
    val isScannerExpanded: Boolean = false,
    val pantryLoaded: Boolean = false,
    val manualMode: Boolean = false,
    val isAnalyzing: Boolean = false,
    val items: List<ScanDraftItem> = emptyList(),
    val pantryItems: List<PantryItem> = emptyList(),
    val matches: Map<String, PantryItem> = emptyMap(),
    val isSaving: Boolean = false,
    val showExitConfirmation: Boolean = false,
    val saveCompleted: Boolean = false,
    val notice: ScannerNotice? = null,
)

class ScannerViewModel(
    private val pantryRepository: PantryRepository,
    private val scanImageAnalyzer: ScanImageAnalyzer,
    private val scanDraftHolder: ScanDraftHolder = ScanDraftHolder(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(ScannerUiState())
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            pantryRepository.observePantryItems().collect(::updatePantryItems)
        }
        // Los chips viven en el holder compartido (D3): así Sugerencias los lee sin
        // guardarlos y el Escáner recupera su estado al volver.
        viewModelScope.launch {
            scanDraftHolder.items.collect { heldItems ->
                if (heldItems != _uiState.value.items) {
                    _uiState.value = recalculateItems(_uiState.value, heldItems)
                }
            }
        }
    }

    fun openScanner() {
        _uiState.value = _uiState.value.copy(isScannerExpanded = true)
    }

    fun closeScanner() {
        _uiState.value = _uiState.value.copy(
            isScannerExpanded = false,
            cameraPreviewReady = false,
        )
    }

    fun onCameraPermissionGranted() {
        _uiState.value = _uiState.value.copy(
            cameraPermissionGranted = true,
            manualMode = false,
            notice = null,
        )
    }

    fun onCameraPermissionDenied() {
        _uiState.value = _uiState.value.copy(
            cameraPermissionGranted = false,
            cameraPreviewReady = false,
            isScannerExpanded = false,
            manualMode = true,
            isAnalyzing = false,
            notice = ScannerNotice.CAMERA_PERMISSION_DENIED,
        )
    }

    fun onCameraPreviewReady() {
        _uiState.value = _uiState.value.copy(cameraPreviewReady = true, manualMode = false)
    }

    fun onCameraPreviewFailed() {
        _uiState.value = _uiState.value.copy(
            cameraPreviewReady = false,
            isScannerExpanded = false,
            manualMode = true,
            isAnalyzing = false,
            notice = ScannerNotice.CAMERA_UNAVAILABLE,
        )
    }

    /**
     * One photo, one AI call (D4): captures an in-memory JPEG, analyzes it and merges the
     * proposals into the current tray (D1). There are no automatic retries (D5); the user
     * can press the shutter again. Missing key and failures keep the manual entry available.
     */
    fun captureAndAnalyze(imageSource: ScanImageSource, language: String) {
        val state = _uiState.value
        if (state.isAnalyzing || state.isSaving) return
        if (!state.cameraPermissionGranted || !state.cameraPreviewReady) return
        _uiState.value = state.copy(isAnalyzing = true, notice = null)
        viewModelScope.launch {
            val image = try {
                imageSource.capture()
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                null
            } catch (_: LinkageError) {
                null
            }
            if (image == null) {
                _uiState.value = _uiState.value.copy(
                    isAnalyzing = false,
                    isScannerExpanded = false,
                    cameraPreviewReady = false,
                    manualMode = true,
                    notice = ScannerNotice.ANALYSIS_FAILED,
                )
                return@launch
            }
            val result = try {
                scanImageAnalyzer.analyze(image, language)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                ScanAnalysisResult.Failed(AiFailure.fromException(exception))
            } catch (exception: LinkageError) {
                ScanAnalysisResult.Failed(AiFailure.fromException(exception))
            }
            when (result) {
                is ScanAnalysisResult.Success -> {
                    val hasFoods = result.foods.any { it.name.isNotBlank() }
                    val merged = ScanAnalysis.mergeProposals(_uiState.value.items, result.foods)
                    _uiState.value = recalculateItems(_uiState.value, merged).copy(
                        isAnalyzing = false,
                        isScannerExpanded = false,
                        cameraPreviewReady = false,
                        manualMode = false,
                        notice = if (hasFoods) null else ScannerNotice.NO_DETECTIONS,
                    )
                }

                ScanAnalysisResult.MissingApiKey -> {
                    _uiState.value = _uiState.value.copy(
                        isAnalyzing = false,
                        isScannerExpanded = false,
                        cameraPreviewReady = false,
                        manualMode = true,
                        notice = ScannerNotice.AI_KEY_MISSING,
                    )
                }

                is ScanAnalysisResult.Failed -> {
                    _uiState.value = _uiState.value.copy(
                        isAnalyzing = false,
                        isScannerExpanded = false,
                        cameraPreviewReady = false,
                        manualMode = true,
                        notice = result.cause.toNotice(),
                    )
                }
            }
        }
    }

    private fun AiFailureCause.toNotice(): ScannerNotice = when (this) {
        AiFailureCause.NO_CONNECTION -> ScannerNotice.ANALYSIS_NO_CONNECTION
        AiFailureCause.PROVIDER_ERROR -> ScannerNotice.ANALYSIS_PROVIDER_ERROR
        AiFailureCause.INVALID_RESPONSE -> ScannerNotice.ANALYSIS_INVALID_RESPONSE
    }

    fun addManualItem(name: String) {
        val cleanName = name.trim()
        if (cleanName.isEmpty()) return
        val state = _uiState.value
        val items = ScanDetections.mergeDraftItems(state.items + ScanDraftItem(name = cleanName))
        _uiState.value = recalculateItems(state, items)
    }

    fun renameItem(id: String, name: String) {
        val state = _uiState.value
        val items = ScanDetections.rename(state.items, id, name)
        _uiState.value = recalculateItems(state, items)
    }

    fun updateQuantity(id: String, quantity: Int) {
        val state = _uiState.value
        _uiState.value = applyItems(state, ScanDetections.updateQuantity(state.items, id, quantity))
    }

    fun removeItem(id: String) {
        val state = _uiState.value
        _uiState.value = recalculateItems(state, state.items.filterNot { it.id == id })
    }

    fun saveToPantry() {
        val state = _uiState.value
        if (state.isSaving || state.items.isEmpty() || !state.pantryLoaded) return
        if (state.items.any { it.name.isBlank() }) {
            _uiState.value = state.copy(notice = ScannerNotice.INVALID_NAME)
            return
        }
        persistItems()
    }

    private fun persistItems() {
        val snapshot = _uiState.value
        if (snapshot.isSaving) return
        _uiState.value = snapshot.copy(isSaving = true)
        viewModelScope.launch {
            try {
                snapshot.items.forEach { draft ->
                    val existing = snapshot.matches[draft.id]
                    val item = existing?.let { ScanDetections.sumInto(it, draft) }
                        ?: ScanDetections.toPantryItem(draft)
                    val result = pantryRepository.savePantryItem(item)
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
                    items = emptyList(),
                    matches = emptyMap(),
                    notice = null,
                )
                scanDraftHolder.clear()
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
        _uiState.value = state.copy(showExitConfirmation = true)
        return false
    }

    fun confirmExit() {
        clearTemporaryScan()
    }

    fun cancelExitConfirmation() {
        _uiState.value = _uiState.value.copy(showExitConfirmation = false)
    }

    fun clearNotice() {
        _uiState.value = _uiState.value.copy(notice = null)
    }

    private fun clearTemporaryScan() {
        val state = _uiState.value
        _uiState.value = state.copy(
            showExitConfirmation = false,
            isScannerExpanded = false,
            cameraPreviewReady = false,
            items = emptyList(),
            matches = emptyMap(),
            notice = null,
        )
        scanDraftHolder.clear()
    }

    private fun updatePantryItems(pantryItems: List<PantryItem>) {
        val state = _uiState.value
        val matches = ScanDetections.matchesWithPantry(state.items, pantryItems)
        _uiState.value = state.copy(
            pantryItems = pantryItems,
            matches = matches,
            pantryLoaded = true,
        )
    }

    private fun recalculateItems(state: ScannerUiState, items: List<ScanDraftItem>): ScannerUiState {
        val matches = ScanDetections.matchesWithPantry(items, state.pantryItems)
        return applyItems(state.copy(matches = matches), items)
    }

    private fun applyItems(state: ScannerUiState, items: List<ScanDraftItem>): ScannerUiState {
        scanDraftHolder.update(items)
        return state.copy(items = items)
    }
}
