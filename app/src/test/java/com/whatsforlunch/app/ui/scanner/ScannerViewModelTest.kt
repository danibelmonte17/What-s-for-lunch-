package com.whatsforlunch.app.ui.scanner

import com.whatsforlunch.app.data.repository.InMemoryPantryRepository
import com.whatsforlunch.app.data.scanner.FoodDetectionResult
import com.whatsforlunch.app.data.scanner.FoodDetector
import com.whatsforlunch.app.data.scanner.ScanCaptureResult
import com.whatsforlunch.app.data.scanner.ScanImageSource
import com.whatsforlunch.app.domain.model.DetectedFood
import com.whatsforlunch.app.domain.model.PantryItem
import com.whatsforlunch.app.domain.repository.PantryDuplicateResolution
import com.whatsforlunch.app.domain.repository.PantryRepository
import com.whatsforlunch.app.domain.repository.PantrySaveResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
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
    fun captureFiltersBelowThresholdAndKeepsImageBytesOnlyInState() = runTest {
        val image = byteArrayOf(4, 8, 15)
        val viewModel = ScannerViewModel(
            InMemoryPantryRepository(),
            FakeFoodDetector(
                FoodDetectionResult.Success(
                    listOf(DetectedFood("Plátano", 0.599), DetectedFood("Plátano", 0.60)),
                ),
            ),
        )
        viewModel.onCameraPermissionGranted()

        viewModel.capture(FakeScanImageSource(ScanCaptureResult.Success(image)))
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.capturedImage!!.contentEquals(image))
        assertEquals(1, viewModel.uiState.value.scan?.detections?.size)
        assertEquals(1, viewModel.uiState.value.items.size)
        assertEquals(0.60, viewModel.uiState.value.items.single().confidence!!, 0.0001)
    }

    @Test
    fun emptyAndFailedInferenceDegradeToManualEntry() = runTest {
        val empty = ScannerViewModel(InMemoryPantryRepository(), FakeFoodDetector(FoodDetectionResult.Success(emptyList())))
        empty.onCameraPermissionGranted()
        empty.capture(FakeScanImageSource(ScanCaptureResult.Success(byteArrayOf(1))))
        advanceUntilIdle()
        assertEquals(ScannerNotice.NO_FOODS_DETECTED, empty.uiState.value.notice)
        assertTrue(empty.uiState.value.manualMode)

        val failed = ScannerViewModel(InMemoryPantryRepository(), FakeFoodDetector(FoodDetectionResult.Failed))
        failed.onCameraPermissionGranted()
        failed.capture(FakeScanImageSource(ScanCaptureResult.Success(byteArrayOf(1))))
        advanceUntilIdle()
        assertEquals(ScannerNotice.DETECTION_FAILED, failed.uiState.value.notice)
        assertTrue(failed.uiState.value.manualMode)
    }

    @Test
    fun permissionDenialContinuesInManualMode() = runTest {
        val viewModel = ScannerViewModel(InMemoryPantryRepository(), FakeFoodDetector(FoodDetectionResult.Failed))
        viewModel.onCameraPermissionDenied()
        viewModel.addManualItem("Arroz")

        assertEquals(ScannerNotice.CAMERA_PERMISSION_DENIED, viewModel.uiState.value.notice)
        assertEquals("Arroz", viewModel.uiState.value.items.single().name)
        assertFalse(viewModel.uiState.value.cameraPreviewReady)
    }

    @Test
    fun manualDuplicateKeepsExistingChipAndQuantityStepperClamps() = runTest {
        val viewModel = ScannerViewModel(InMemoryPantryRepository(), FakeFoodDetector(FoodDetectionResult.Failed))
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
        val viewModel = ScannerViewModel(repository, FakeFoodDetector(FoodDetectionResult.Success(listOf(DetectedFood("Tomate fresco", 0.75)))))
        viewModel.onCameraPermissionGranted()
        viewModel.capture(FakeScanImageSource(ScanCaptureResult.Success(byteArrayOf(1))))
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
                FoodDetectionResult.Success(
                    listOf(DetectedFood("Tomate fresco", 0.75), DetectedFood("Plátano", 0.84)),
                ),
            ),
        )
        viewModel.onCameraPermissionGranted()
        viewModel.capture(FakeScanImageSource(ScanCaptureResult.Success(byteArrayOf(1))))
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
        assertNull(viewModel.uiState.value.capturedImage)
    }

    @Test
    fun exitAndRescanAskBeforeDiscardingManualChips() = runTest {
        val viewModel = ScannerViewModel(InMemoryPantryRepository(), FakeFoodDetector(FoodDetectionResult.Failed))
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
        val viewModel = ScannerViewModel(repository, FakeFoodDetector(FoodDetectionResult.Failed))
        viewModel.addManualItem("Tomate")
        viewModel.saveToPantry()
        advanceUntilIdle()

        assertEquals(ScannerNotice.DUPLICATE_NAME, viewModel.uiState.value.notice)
        assertFalse(viewModel.uiState.value.saveCompleted)
    }

    private class FakeScanImageSource(private val result: ScanCaptureResult) : ScanImageSource {
        override suspend fun capture(): ScanCaptureResult = result
    }

    private class FakeFoodDetector(private val result: FoodDetectionResult) : FoodDetector {
        override suspend fun detect(image: ByteArray): FoodDetectionResult = result
    }

    private class DuplicatePantryRepository : PantryRepository {
        override fun observePantryItems(): Flow<List<PantryItem>> = flowOf(emptyList())
        override suspend fun savePantryItem(item: PantryItem): PantrySaveResult = PantrySaveResult.DuplicateName
        override suspend fun deletePantryItem(id: String) = Unit
    }
}
