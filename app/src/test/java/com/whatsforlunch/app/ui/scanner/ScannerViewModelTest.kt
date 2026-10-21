package com.whatsforlunch.app.ui.scanner

import com.whatsforlunch.app.data.repository.InMemoryPantryRepository
import com.whatsforlunch.app.data.scanner.FoodDetectionResult
import com.whatsforlunch.app.data.scanner.FoodDetector
import com.whatsforlunch.app.data.scanner.LiveFrameSource
import com.whatsforlunch.app.domain.model.DetectedFood
import com.whatsforlunch.app.domain.model.PantryItem
import com.whatsforlunch.app.domain.repository.PantryDuplicateResolution
import com.whatsforlunch.app.domain.repository.PantryRepository
import com.whatsforlunch.app.domain.repository.PantrySaveResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ScannerViewModelTest {
    @Before
    fun setMainDispatcher() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    @Test
    fun liveFramesAddEachFoodOnceWithBestConfidenceAndConflateIntermediateFrames() = runTest {
        val detector = FakeFoodDetector(
            listOf(
                FoodDetectionResult.Success(listOf(DetectedFood("Plátano", 0.72))),
                FoodDetectionResult.Success(listOf(DetectedFood("  PLÁTANO ", 0.83), DetectedFood("Manzana", 0.65))),
            ),
        )
        val viewModel = ScannerViewModel(InMemoryPantryRepository(), detector)
        viewModel.onCameraPermissionGranted()
        viewModel.onCameraPreviewReady()

        viewModel.startLiveDetection(FakeLiveFrameSource(listOf(byteArrayOf(1), byteArrayOf(2), byteArrayOf(3))))
        advanceUntilIdle()

        // Frame 2 is conflated away while another frame is being detected (D2): only two inferences run.
        assertEquals(2, detector.calls)
        val items = viewModel.uiState.value.items
        assertEquals(listOf("Plátano", "Manzana"), items.map { it.name })
        assertEquals(0.83, items.first().confidence!!, 0.0001)
        assertEquals(0.65, items.last().confidence!!, 0.0001)
        assertEquals(2, viewModel.uiState.value.scan?.detections?.size)
        assertFalse(viewModel.uiState.value.isDetecting)
    }

    @Test
    fun liveDetectionAccumulatesWithoutRemovingItemsAndNeverTouchesUserEdits() = runTest {
        val detector = FakeFoodDetector(
            listOf(
                FoodDetectionResult.Success(listOf(DetectedFood("Plátano", 0.70))),
                FoodDetectionResult.Success(listOf(DetectedFood("banana", 0.90), DetectedFood("Manzana", 0.61))),
            ),
        )
        val viewModel = ScannerViewModel(InMemoryPantryRepository(), detector)
        viewModel.onCameraPermissionGranted()
        viewModel.onCameraPreviewReady()
        viewModel.startLiveDetection(FakeLiveFrameSource(listOf(byteArrayOf(1))))
        advanceUntilIdle()

        val id = viewModel.uiState.value.items.single().id
        viewModel.renameItem(id, "Plátano macho")
        viewModel.updateQuantity(id, 5)
        viewModel.startLiveDetection(FakeLiveFrameSource(listOf(byteArrayOf(2))))
        advanceUntilIdle()

        // D8: the renamed item no longer shares the normalized name, so the re-detected
        // «banana» label adds a new item; the user-edited entry stays untouched.
        assertEquals(listOf("Plátano macho", "banana", "Manzana"), viewModel.uiState.value.items.map { it.name })
        assertEquals(5, viewModel.uiState.value.items.first().quantity)
        assertEquals(0.70, viewModel.uiState.value.items.first().confidence!!, 0.0001)
        assertEquals(id, viewModel.uiState.value.items.first().id)
    }

    @Test
    fun emptyFramesKeepManualEntryAndDetectorFailureStopsSamplingUntilRestarted() = runTest {
        val emptyViewModel = ScannerViewModel(
            InMemoryPantryRepository(),
            FakeFoodDetector(listOf(FoodDetectionResult.Success(emptyList()))),
        )
        emptyViewModel.onCameraPermissionGranted()
        emptyViewModel.onCameraPreviewReady()
        emptyViewModel.startLiveDetection(FakeLiveFrameSource(listOf(byteArrayOf(1))))
        advanceUntilIdle()
        assertTrue(emptyViewModel.uiState.value.items.isEmpty())
        assertNull(emptyViewModel.uiState.value.notice)
        assertFalse(emptyViewModel.uiState.value.manualMode)

        val detector = FakeFoodDetector(
            listOf(
                FoodDetectionResult.Failed,
                FoodDetectionResult.Success(listOf(DetectedFood("Plátano", 0.90))),
            ),
        )
        val failed = ScannerViewModel(InMemoryPantryRepository(), detector)
        failed.onCameraPermissionGranted()
        failed.onCameraPreviewReady()
        failed.startLiveDetection(FakeLiveFrameSource(listOf(byteArrayOf(1), byteArrayOf(2))))
        advanceUntilIdle()
        assertEquals(ScannerNotice.DETECTION_FAILED, failed.uiState.value.notice)
        assertTrue(failed.uiState.value.manualMode)
        assertTrue(failed.uiState.value.liveStopped)
        assertEquals(1, detector.calls)

        failed.startLiveDetection(FakeLiveFrameSource(listOf(byteArrayOf(3))))
        advanceUntilIdle()
        assertEquals(listOf("Plátano"), failed.uiState.value.items.map { it.name })
        assertFalse(failed.uiState.value.isDetecting)
        assertFalse(failed.uiState.value.liveStopped)
    }

    @Test
    fun permissionDenialContinuesInManualMode() = runTest {
        val viewModel = ScannerViewModel(InMemoryPantryRepository(), FakeFoodDetector(listOf(FoodDetectionResult.Failed)))
        viewModel.onCameraPermissionDenied()
        viewModel.addManualItem("Arroz")

        assertEquals(ScannerNotice.CAMERA_PERMISSION_DENIED, viewModel.uiState.value.notice)
        assertEquals("Arroz", viewModel.uiState.value.items.single().name)
        assertFalse(viewModel.uiState.value.cameraPreviewReady)
    }

    @Test
    fun manualDuplicateKeepsExistingChipAndQuantityStepperClamps() = runTest {
        val viewModel = ScannerViewModel(InMemoryPantryRepository(), FakeFoodDetector(listOf(FoodDetectionResult.Failed)))
        viewModel.addManualItem("Manzana")
        val firstId = viewModel.uiState.value.items.single().id
        viewModel.updateQuantity(firstId, 99)
        viewModel.addManualItem(" MANZANA ")

        assertEquals(1, viewModel.uiState.value.items.size)
        assertEquals(99, viewModel.uiState.value.items.single().quantity)
        viewModel.updateQuantity(firstId, 100)
        assertEquals(99, viewModel.uiState.value.items.single().quantity)
        viewModel.updateQuantity(firstId, -1)
        assertEquals(0, viewModel.uiState.value.items.single().quantity)
    }

    @Test
    fun matchedFoodIsPromptedAutomaticallyAndCanChangeDecision() = runTest {
        val repository = InMemoryPantryRepository()
        repository.savePantryItem(PantryItem("tomato", "Tomate fresco", 1))
        val viewModel = ScannerViewModel(
            repository,
            FakeFoodDetector(listOf(FoodDetectionResult.Success(listOf(DetectedFood("Tomate fresco", 0.75))))),
        )
        viewModel.onCameraPermissionGranted()
        viewModel.onCameraPreviewReady()
        viewModel.startLiveDetection(FakeLiveFrameSource(listOf(byteArrayOf(1))))
        advanceUntilIdle()

        val draftId = viewModel.uiState.value.items.single().id
        assertEquals(draftId, viewModel.uiState.value.activeDuplicateItemId)
        viewModel.resolveDuplicate(draftId, PantryDuplicateResolution.KeepExisting)
        assertEquals(PantryDuplicateResolution.KeepExisting, viewModel.uiState.value.duplicateDecisions[draftId])
        viewModel.openDuplicateResolution(draftId)
        viewModel.resolveDuplicate(draftId, PantryDuplicateResolution.UpdateQuantity)
        assertEquals(PantryDuplicateResolution.UpdateQuantity, viewModel.uiState.value.duplicateDecisions[draftId])
    }

    @Test
    fun savingWithPendingMatchesWritesNothingUntilEveryDialogIsResolved() = runTest {
        val repository = InMemoryPantryRepository()
        repository.savePantryItem(PantryItem("tomato", "Tomate fresco", 1))
        val viewModel = ScannerViewModel(
            repository,
            FakeFoodDetector(
                listOf(
                    FoodDetectionResult.Success(
                        listOf(DetectedFood("Tomate fresco", 0.75), DetectedFood("Plátano", 0.84)),
                    ),
                ),
            ),
        )
        viewModel.onCameraPermissionGranted()
        viewModel.onCameraPreviewReady()
        viewModel.startLiveDetection(FakeLiveFrameSource(listOf(byteArrayOf(1))))
        advanceUntilIdle()

        val tomato = viewModel.uiState.value.items.single { it.name == "Tomate fresco" }
        viewModel.dismissDuplicateResolution()
        viewModel.saveToPantry()
        assertTrue(viewModel.uiState.value.pendingSave)
        assertEquals(1, repository.observePantryItems().first().size)
        viewModel.dismissDuplicateResolution()
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.pendingSave)
        assertEquals(1, repository.observePantryItems().first().size)

        viewModel.saveToPantry()
        assertEquals(tomato.id, viewModel.uiState.value.activeDuplicateItemId)
        viewModel.resolveDuplicate(tomato.id, PantryDuplicateResolution.UpdateQuantity)
        advanceUntilIdle()

        val saved = repository.observePantryItems().first()
        assertEquals(2, saved.size)
        assertNull(saved.first { it.id == "tomato" }.quantity)
        assertTrue(saved.any { it.name == "Plátano" })
        assertTrue(viewModel.uiState.value.saveCompleted)
        assertNull(viewModel.uiState.value.scan)
    }

    @Test
    fun exitAndRescanAskBeforeDiscardingManualChips() = runTest {
        val viewModel = ScannerViewModel(InMemoryPantryRepository(), FakeFoodDetector(listOf(FoodDetectionResult.Failed)))
        viewModel.addManualItem("Harina")

        assertFalse(viewModel.requestExit())
        assertTrue(viewModel.uiState.value.showExitConfirmation)
        viewModel.cancelExitConfirmation()
        assertEquals("Harina", viewModel.uiState.value.items.single().name)

        assertFalse(viewModel.requestRescan())
        assertTrue(viewModel.confirmExit())
        assertTrue(viewModel.uiState.value.items.isEmpty())
        assertNull(viewModel.uiState.value.scan)
    }

    @Test
    fun duplicateRepositoryResultIsNotReportedAsSuccessfulSave() = runTest {
        val repository = DuplicatePantryRepository()
        val viewModel = ScannerViewModel(repository, FakeFoodDetector(listOf(FoodDetectionResult.Failed)))
        viewModel.addManualItem("Tomate")
        viewModel.saveToPantry()
        advanceUntilIdle()

        assertEquals(ScannerNotice.DUPLICATE_NAME, viewModel.uiState.value.notice)
        assertFalse(viewModel.uiState.value.saveCompleted)
    }

    private class FakeLiveFrameSource(private val frames: List<ByteArray>) : LiveFrameSource {
        override fun frames(): Flow<ByteArray> = flow { frames.forEach { emit(it) } }
    }

    private class FakeFoodDetector(private val results: List<FoodDetectionResult>) : FoodDetector {
        var calls: Int = 0
            private set

        override suspend fun detect(image: ByteArray): FoodDetectionResult = results[minOf(calls++, results.lastIndex)]
    }

    private class DuplicatePantryRepository : PantryRepository {
        override fun observePantryItems(): Flow<List<PantryItem>> = flowOf(emptyList())
        override suspend fun savePantryItem(item: PantryItem): PantrySaveResult = PantrySaveResult.DuplicateName
        override suspend fun deletePantryItem(id: String) = Unit
    }
}
