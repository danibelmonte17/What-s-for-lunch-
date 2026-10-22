package com.whatsforlunch.app.ui.scanner

import com.whatsforlunch.app.data.ai.ScanAnalysisResult
import com.whatsforlunch.app.data.ai.ScanImageAnalyzer
import com.whatsforlunch.app.data.repository.InMemoryPantryRepository
import com.whatsforlunch.app.data.repository.InMemoryRecipeRepository
import com.whatsforlunch.app.data.scanner.ScanImageSource
import com.whatsforlunch.app.domain.model.PantryItem
import com.whatsforlunch.app.domain.repository.PantryRepository
import com.whatsforlunch.app.domain.repository.PantrySaveResult
import com.whatsforlunch.app.domain.scanner.AnalyzedFood
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
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
    fun successfulCaptureMergesAiProposalsIntoEditableChips() = runTest {
        val viewModel = scannerViewModel(
            results = listOf(
                ScanAnalysisResult.Success(listOf(AnalyzedFood("Manzana", 3.0), AnalyzedFood("Nata", 1.0))),
            ),
        )
        grantCamera(viewModel)

        viewModel.captureAndAnalyze(FakeScanImageSource(), "es")
        advanceUntilIdle()

        assertEquals(listOf("Manzana", "Nata"), viewModel.uiState.value.items.map { it.name })
        assertEquals(listOf(3, 1), viewModel.uiState.value.items.map { it.quantity })
        assertFalse(viewModel.uiState.value.isAnalyzing)
        assertNull(viewModel.uiState.value.notice)
    }

    @Test
    fun successiveCapturesAccumulateWithoutOverwritingUserEdits() = runTest {
        val analyzer = FakeScanImageAnalyzer(
            listOf(
                ScanAnalysisResult.Success(listOf(AnalyzedFood("Manzana", 3.0))),
                ScanAnalysisResult.Success(listOf(AnalyzedFood("manzana", 5.0), AnalyzedFood("Pera", 1.0))),
            ),
        )
        val viewModel = scannerViewModel(analyzer = analyzer)
        grantCamera(viewModel)
        viewModel.captureAndAnalyze(FakeScanImageSource(), "es")
        advanceUntilIdle()

        val id = viewModel.uiState.value.items.single().id
        viewModel.updateQuantity(id, 2)
        viewModel.captureAndAnalyze(FakeScanImageSource(), "es")
        advanceUntilIdle()

        val items = viewModel.uiState.value.items
        assertEquals(listOf("Manzana", "Pera"), items.map { it.name })
        assertEquals(2, items.first().quantity)
        assertEquals(id, items.first().id)
        assertEquals(1, items.last().quantity)
    }

    @Test
    fun missingApiKeyKeepsManualEntryAndShowsSpecificNotice() = runTest {
        val viewModel = scannerViewModel(results = listOf(ScanAnalysisResult.MissingApiKey))
        grantCamera(viewModel)

        viewModel.captureAndAnalyze(FakeScanImageSource(), "es")
        advanceUntilIdle()

        assertEquals(ScannerNotice.AI_KEY_MISSING, viewModel.uiState.value.notice)
        assertTrue(viewModel.uiState.value.manualMode)
        assertFalse(viewModel.uiState.value.isAnalyzing)
        viewModel.addManualItem("Arroz")
        assertEquals("Arroz", viewModel.uiState.value.items.single().name)
    }

    @Test
    fun analysisFailureShowsNoticeAndAllowsManualRetry() = runTest {
        val analyzer = FakeScanImageAnalyzer(
            listOf(
                ScanAnalysisResult.Failed,
                ScanAnalysisResult.Success(listOf(AnalyzedFood("Plátano", 2.0))),
            ),
        )
        val viewModel = scannerViewModel(analyzer = analyzer)
        grantCamera(viewModel)

        viewModel.captureAndAnalyze(FakeScanImageSource(), "es")
        advanceUntilIdle()
        assertEquals(ScannerNotice.ANALYSIS_FAILED, viewModel.uiState.value.notice)
        assertTrue(viewModel.uiState.value.items.isEmpty())

        viewModel.captureAndAnalyze(FakeScanImageSource(), "es")
        advanceUntilIdle()
        assertEquals(listOf("Plátano"), viewModel.uiState.value.items.map { it.name })
        assertEquals(2, analyzer.calls)
        assertNull(viewModel.uiState.value.notice)
    }

    @Test
    fun failedOrEmptyCaptureIsReportedAsAnalysisFailure() = runTest {
        val viewModel = scannerViewModel()
        grantCamera(viewModel)

        viewModel.captureAndAnalyze(FakeScanImageSource(bytes = null), "es")
        advanceUntilIdle()

        assertEquals(ScannerNotice.ANALYSIS_FAILED, viewModel.uiState.value.notice)
        assertTrue(viewModel.uiState.value.manualMode)
    }

    @Test
    fun permissionDenialContinuesInManualMode() = runTest {
        val viewModel = scannerViewModel()
        viewModel.onCameraPermissionDenied()
        viewModel.addManualItem("Arroz")

        assertEquals(ScannerNotice.CAMERA_PERMISSION_DENIED, viewModel.uiState.value.notice)
        assertEquals("Arroz", viewModel.uiState.value.items.single().name)
        assertFalse(viewModel.uiState.value.cameraPreviewReady)
    }

    @Test
    fun manualDuplicateKeepsExistingChipAndQuantityStepperClamps() = runTest {
        val viewModel = scannerViewModel()
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
    fun saveSumsMatchedChipQuantityIntoExistingPantryItem() = runTest {
        val repository = InMemoryPantryRepository()
        repository.savePantryItem(PantryItem("tomato", "Tomate fresco", 1))
        val viewModel = scannerViewModel(
            repository = repository,
            results = listOf(ScanAnalysisResult.Success(listOf(AnalyzedFood("Tomate fresco", 2.0)))),
        )
        grantCamera(viewModel)
        viewModel.captureAndAnalyze(FakeScanImageSource(), "es")
        advanceUntilIdle()

        viewModel.updateQuantity(viewModel.uiState.value.items.single().id, 2)
        viewModel.saveToPantry()
        advanceUntilIdle()

        val saved = repository.observePantryItems().first()
        assertEquals(1, saved.size)
        assertEquals(3, saved.single().quantity)
        assertEquals("tomato", saved.single().id)
        assertTrue(viewModel.uiState.value.saveCompleted)
    }

    @Test
    fun saveAppliesAllSumsAndCreatesOnlyUnmatchedItems() = runTest {
        val repository = InMemoryPantryRepository()
        repository.savePantryItem(PantryItem("tomato", "Tomate", null))
        repository.savePantryItem(PantryItem("milk", "Leche", 3))
        val viewModel = scannerViewModel(
            repository = repository,
            results = listOf(
                ScanAnalysisResult.Success(
                    listOf(AnalyzedFood("Tomate", 2.0), AnalyzedFood("Leche", 0.0), AnalyzedFood("Plátano", 4.0)),
                ),
            ),
        )
        grantCamera(viewModel)
        viewModel.captureAndAnalyze(FakeScanImageSource(), "es")
        advanceUntilIdle()

        viewModel.saveToPantry()
        advanceUntilIdle()

        val saved = repository.observePantryItems().first()
        assertEquals(3, saved.size)
        assertEquals(2, saved.first { it.id == "tomato" }.quantity)
        assertEquals(3, saved.first { it.id == "milk" }.quantity)
        assertEquals(4, saved.first { it.name == "Plátano" }.quantity)
        assertTrue(viewModel.uiState.value.saveCompleted)
    }

    @Test
    fun exitAndRescanAskBeforeDiscardingManualChips() = runTest {
        val viewModel = scannerViewModel()
        viewModel.addManualItem("Harina")

        assertFalse(viewModel.requestExit())
        assertTrue(viewModel.uiState.value.showExitConfirmation)
        viewModel.cancelExitConfirmation()
        assertEquals("Harina", viewModel.uiState.value.items.single().name)

        assertFalse(viewModel.requestRescan())
        assertTrue(viewModel.confirmExit())
        assertTrue(viewModel.uiState.value.items.isEmpty())
    }

    @Test
    fun duplicateRepositoryResultIsNotReportedAsSuccessfulSave() = runTest {
        val viewModel = scannerViewModel(repository = DuplicatePantryRepository())
        viewModel.addManualItem("Tomate")
        viewModel.saveToPantry()
        advanceUntilIdle()

        assertEquals(ScannerNotice.DUPLICATE_NAME, viewModel.uiState.value.notice)
        assertFalse(viewModel.uiState.value.saveCompleted)
    }

    private fun scannerViewModel(
        repository: PantryRepository = InMemoryPantryRepository(),
        analyzer: ScanImageAnalyzer = FakeScanImageAnalyzer(
            listOf(ScanAnalysisResult.Success(listOf(AnalyzedFood("Plátano", 1.0)))),
        ),
        results: List<ScanAnalysisResult>? = null,
    ): ScannerViewModel = ScannerViewModel(
        repository,
        InMemoryRecipeRepository(),
        results?.let { FakeScanImageAnalyzer(it) } ?: analyzer,
    )

    private fun grantCamera(viewModel: ScannerViewModel) {
        viewModel.onCameraPermissionGranted()
        viewModel.onCameraPreviewReady()
    }

    private class FakeScanImageSource(private val bytes: ByteArray? = byteArrayOf(1, 2, 3)) : ScanImageSource {
        override suspend fun capture(): ByteArray? = bytes
    }

    private class FakeScanImageAnalyzer(private val results: List<ScanAnalysisResult>) : ScanImageAnalyzer {
        var calls: Int = 0
            private set

        override suspend fun analyze(image: ByteArray, language: String): ScanAnalysisResult =
            results[minOf(calls++, results.lastIndex)]
    }

    private class DuplicatePantryRepository : PantryRepository {
        override fun observePantryItems(): Flow<List<PantryItem>> = flowOf(emptyList())
        override suspend fun savePantryItem(item: PantryItem): PantrySaveResult = PantrySaveResult.DuplicateName
        override suspend fun deletePantryItem(id: String) = Unit
    }
}
