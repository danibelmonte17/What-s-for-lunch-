package com.whatsforlunch.app

import android.content.Context
import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.whatsforlunch.app.data.repository.InMemoryPantryRepository
import com.whatsforlunch.app.data.scanner.FoodDetectionResult
import com.whatsforlunch.app.data.scanner.FoodDetector
import com.whatsforlunch.app.data.scanner.LiveFrameSource
import com.whatsforlunch.app.domain.model.DetectedFood
import com.whatsforlunch.app.domain.model.PantryItem
import com.whatsforlunch.app.ui.pantry.PantryScreen
import com.whatsforlunch.app.ui.pantry.PantryTestTags
import com.whatsforlunch.app.ui.pantry.PantryUiState
import com.whatsforlunch.app.ui.scanner.ScannerScreen
import com.whatsforlunch.app.ui.scanner.ScannerTestTags
import com.whatsforlunch.app.ui.scanner.ScannerViewModel
import com.whatsforlunch.app.ui.theme.WhatsForLunchTheme
import java.io.File
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScanPantryE2ETest {
    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var viewModel: ScannerViewModel
    private lateinit var repository: InMemoryPantryRepository
    private lateinit var detector: FakeFoodDetector
    private lateinit var showPantry: androidx.compose.runtime.MutableState<Boolean>
    private lateinit var leftScanner: androidx.compose.runtime.MutableState<Boolean>
    private lateinit var localeContext: Context

    @Before
    fun clearDatabase() {
        E2eDatabase.clear()
    }

    @Test
    fun liveFramesShowFoodOnceWithBestConfidenceAndHudLabel() {
        launchScanner(
            frames = listOf(FRAME, FRAME),
            detectionResults = listOf(
                FoodDetectionResult.Success(listOf(DetectedFood("Plátano", 0.72))),
                FoodDetectionResult.Success(listOf(DetectedFood("Plátano", 0.84))),
            ),
        )
        waitForFrames(2)

        composeRule.onAllNodesWithTag(ScannerTestTags.FOOD_ITEM).assertCountEquals(1)
        val id = viewModel.uiState.value.items.single().id
        composeRule.onNodeWithTag(ScannerTestTags.itemName(id)).assertIsDisplayed()
        composeRule.onNodeWithTag(ScannerTestTags.itemConfidence(id)).assertTextEquals("84%")
        composeRule.onNodeWithTag(ScannerTestTags.hudLabel(0)).assertIsDisplayed()
        composeRule.onNodeWithText("Plátano · 84%").assertIsDisplayed()
    }

    @Test
    fun savePersistsChosenQuantityAndShowsPantryScreen() {
        launchScanner(detectionResults = listOf(FoodDetectionResult.Success(listOf(DetectedFood("Plátano", 0.84)))))
        waitForItems(1)
        val id = viewModel.uiState.value.items.single().id
        repeat(3) { composeRule.onNodeWithTag(ScannerTestTags.quantityIncrement(id)).performClick() }
        composeRule.onNodeWithTag(ScannerTestTags.SAVE).performClick()

        composeRule.waitUntil(5_000) { showPantry.value }
        composeRule.onNodeWithTag(PantryTestTags.SCREEN).assertIsDisplayed()
        composeRule.onNodeWithText("Plátano").assertIsDisplayed()
        composeRule.onNodeWithText(locale(R.string.pantry_quantity_value, 3)).assertIsDisplayed()
        assertEquals(3, runBlocking { repository.observePantryItems().first().single().quantity })
    }

    @Test
    fun manualFoodCanBeAddedWithQuantityAndSaved() {
        launchScanner(detectionResults = listOf(FoodDetectionResult.Success(emptyList())))
        composeRule.onNodeWithTag(ScannerTestTags.MANUAL_NAME).performTextInput("Tomate")
        composeRule.onNodeWithTag(ScannerTestTags.MANUAL_ADD).performClick()

        val id = viewModel.uiState.value.items.single().id
        repeat(2) { composeRule.onNodeWithTag(ScannerTestTags.quantityIncrement(id)).performClick() }
        assertEquals(2, viewModel.uiState.value.items.single().quantity)
        composeRule.onNodeWithTag(ScannerTestTags.SAVE).performClick()

        composeRule.waitUntil(5_000) { showPantry.value }
        assertEquals(2, runBlocking { repository.observePantryItems().first().single().quantity })
        composeRule.onNodeWithText(locale(R.string.pantry_quantity_value, 2)).assertIsDisplayed()
    }

    @Test
    fun chipsCanBeRemovedRenamedAndAddedManually() {
        launchScanner(
            detectionResults = listOf(
                FoodDetectionResult.Success(listOf(DetectedFood("Plátano", 0.84), DetectedFood("Manzana", 0.73))),
            ),
        )
        waitForItems(2)
        val firstId = viewModel.uiState.value.items.first().id
        val secondId = viewModel.uiState.value.items.last().id

        composeRule.onNodeWithTag(ScannerTestTags.itemName(firstId)).performTextClearance()
        composeRule.onNodeWithTag(ScannerTestTags.itemName(firstId)).performTextInput("Pera")
        composeRule.onNodeWithTag(ScannerTestTags.removeItem(secondId)).performClick()
        composeRule.onNodeWithTag(ScannerTestTags.MANUAL_NAME).performTextInput("Arroz")
        composeRule.onNodeWithTag(ScannerTestTags.MANUAL_ADD).performClick()

        assertEquals(listOf("Pera", "Arroz"), viewModel.uiState.value.items.map { it.name })
        assertEquals(null, viewModel.uiState.value.items.last().confidence)
        composeRule.onNodeWithTag(ScannerTestTags.SAVE).assertIsEnabled()
    }

    @Test
    fun detectedAndUserCreatedDuplicatesCollapseToOneChip() {
        launchScanner(
            detectionResults = listOf(
                FoodDetectionResult.Success(
                    listOf(DetectedFood("TOMATE FRESCO", 0.65), DetectedFood("  Tomate  fresco ", 0.81)),
                ),
            ),
        )
        waitForItems(1)
        assertEquals(1, viewModel.uiState.value.items.size)
        assertEquals("Tomate fresco", viewModel.uiState.value.items.single().name)

        val id = viewModel.uiState.value.items.single().id
        composeRule.onNodeWithTag(ScannerTestTags.MANUAL_NAME).performTextInput("TOMATE  FRESCO")
        composeRule.onNodeWithTag(ScannerTestTags.MANUAL_ADD).performClick()
        assertEquals(1, viewModel.uiState.value.items.size)
        assertEquals(id, viewModel.uiState.value.items.single().id)
    }

    @Test
    fun quantityStepperHasBoundsAndNoEditableQuantityField() {
        launchScanner(detectionResults = listOf(FoodDetectionResult.Success(listOf(DetectedFood("Plátano", 0.84)))))
        waitForItems(1)
        val id = viewModel.uiState.value.items.single().id

        composeRule.onNodeWithTag(ScannerTestTags.itemQuantity(id)).assertTextEquals("0")
        composeRule.onNodeWithTag(ScannerTestTags.quantityDecrement(id)).assertIsNotEnabled()
        assertFalse(
            composeRule.onNodeWithTag(ScannerTestTags.itemQuantity(id))
                .fetchSemanticsNode().config.contains(SemanticsActions.SetText),
        )
        repeat(99) { composeRule.onNodeWithTag(ScannerTestTags.quantityIncrement(id)).performClick() }
        composeRule.onNodeWithTag(ScannerTestTags.itemQuantity(id)).assertTextEquals("99")
        composeRule.onNodeWithTag(ScannerTestTags.quantityIncrement(id)).assertIsNotEnabled()
    }

    @Test
    fun deniedPermissionContinuesWithManualEntry() {
        launchScanner(permissionGranted = false)
        composeRule.onNodeWithText(locale(R.string.scanner_camera_denied)).assertIsDisplayed()
        composeRule.onAllNodesWithTag(ScannerTestTags.LIVE_HUD).assertCountEquals(0)
        composeRule.onNodeWithTag(ScannerTestTags.MANUAL_NAME).performTextInput("Harina")
        composeRule.onNodeWithTag(ScannerTestTags.MANUAL_ADD).performClick()
        composeRule.onNodeWithText("Harina").assertIsDisplayed()
        composeRule.onNodeWithTag(ScannerTestTags.SAVE).assertIsEnabled()
    }

    @Test
    fun emptyFramesShowLiveHintAndManualEntryCanSave() {
        launchScanner(detectionResults = listOf(FoodDetectionResult.Success(emptyList())))
        waitForFrames(1)
        composeRule.onAllNodesWithTag(ScannerTestTags.FOOD_ITEM).assertCountEquals(0)
        composeRule.onNodeWithText(locale(R.string.scanner_live_hint)).assertIsDisplayed()
        composeRule.onNodeWithTag(ScannerTestTags.MANUAL_NAME).performTextInput("Lentejas")
        composeRule.onNodeWithTag(ScannerTestTags.MANUAL_ADD).performClick()
        composeRule.onNodeWithTag(ScannerTestTags.SAVE).performClick()
        composeRule.waitUntil(5_000) { showPantry.value }
        composeRule.onNodeWithText("Lentejas").assertIsDisplayed()
    }

    @Test
    fun detectorFailureShowsNoticeAndKeepsManualFlowAvailable() {
        launchScanner(detectionResults = listOf(FoodDetectionResult.Failed))
        waitForFrames(1)
        composeRule.onNodeWithText(locale(R.string.scanner_detection_failed)).assertIsDisplayed()
        composeRule.onNodeWithTag(ScannerTestTags.MANUAL_NAME).performTextInput("Garbanzos")
        composeRule.onNodeWithTag(ScannerTestTags.MANUAL_ADD).performClick()
        composeRule.onNodeWithTag(ScannerTestTags.SAVE).assertIsEnabled()
    }

    @Test
    fun detectorFailureWithEmptyListCanBeRestartedWithRescan() {
        launchScanner(
            detectionResults = listOf(
                FoodDetectionResult.Failed,
                FoodDetectionResult.Success(listOf(DetectedFood("Plátano", 0.90))),
            ),
        )
        waitForFrames(1)

        composeRule.onNodeWithText(locale(R.string.scanner_detection_failed)).assertIsDisplayed()
        composeRule.onAllNodesWithTag(ScannerTestTags.FOOD_ITEM).assertCountEquals(0)
        composeRule.onNodeWithText(locale(R.string.scanner_live_hint)).assertDoesNotExist()
        composeRule.onNodeWithTag(ScannerTestTags.RESCAN).assertIsDisplayed()

        composeRule.onNodeWithTag(ScannerTestTags.RESCAN).performClick()
        composeRule.onNodeWithTag(ScannerTestTags.EXIT_DIALOG).assertDoesNotExist()
        composeRule.waitUntil(10_000) {
            detector.calls >= 2 && viewModel.uiState.value.items.isNotEmpty()
        }
        assertFalse(viewModel.uiState.value.liveStopped)
        composeRule.onNodeWithText("Plátano").assertIsDisplayed()
        composeRule.onNodeWithTag(ScannerTestTags.itemConfidence(viewModel.uiState.value.items.single().id))
            .assertTextEquals("90%")
    }

    @Test
    fun englishLabelsAndScannerActionsAreLocalized() {
        launchScanner(
            detectionResults = listOf(FoodDetectionResult.Success(listOf(DetectedFood("Banana", 0.84)))),
            locale = Locale.ENGLISH,
        )
        composeRule.onNodeWithText("Pantry Scanner").assertIsDisplayed()
        waitForItems(1)
        composeRule.onNodeWithText("Banana · 84%").assertIsDisplayed()
        composeRule.onNodeWithText("Save to Pantry").assertIsDisplayed()
        val id = viewModel.uiState.value.items.single().id
        composeRule.onNodeWithContentDescription("Decrease quantity").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Increase quantity").assertIsDisplayed()
        assertTrue(id.isNotBlank())
    }

    @Test
    fun exitConfirmationCancelKeepsChipsAndConfirmLeaves() {
        launchScanner(permissionGranted = false)
        composeRule.onNodeWithTag(ScannerTestTags.MANUAL_NAME).performTextInput("Harina")
        composeRule.onNodeWithTag(ScannerTestTags.MANUAL_ADD).performClick()
        composeRule.onNodeWithTag(ScannerTestTags.BACK).performClick()
        composeRule.onNodeWithTag(ScannerTestTags.EXIT_DIALOG).assertIsDisplayed()
        composeRule.onNodeWithTag(ScannerTestTags.EXIT_CANCEL).performClick()
        composeRule.onNodeWithText("Harina").assertIsDisplayed()

        composeRule.onNodeWithTag(ScannerTestTags.BACK).performClick()
        composeRule.onNodeWithTag(ScannerTestTags.EXIT_CONFIRM).performClick()
        composeRule.onNodeWithTag(LEFT_AFTER_EXIT).assertIsDisplayed()
        assertTrue(viewModel.uiState.value.items.isEmpty())
    }

    @Test
    fun rescanningWithUnsavedChipsRequiresConfirmationAndRestartsEmpty() {
        launchScanner(detectionResults = listOf(FoodDetectionResult.Success(listOf(DetectedFood("Plátano", 0.84)))))
        waitForItems(1)
        composeRule.onNodeWithTag(ScannerTestTags.RESCAN).performClick()
        composeRule.onNodeWithTag(ScannerTestTags.EXIT_DIALOG).assertIsDisplayed()
        composeRule.onNodeWithTag(ScannerTestTags.EXIT_CANCEL).performClick()
        composeRule.onNodeWithText("Plátano").assertIsDisplayed()

        composeRule.onNodeWithTag(ScannerTestTags.RESCAN).performClick()
        composeRule.onNodeWithTag(ScannerTestTags.EXIT_CONFIRM).performClick()
        composeRule.waitUntil(10_000) {
            !viewModel.uiState.value.showExitConfirmation && viewModel.uiState.value.items.isNotEmpty()
        }
        assertFalse(viewModel.uiState.value.showExitConfirmation)
    }

    @Test
    fun saveSumsChipQuantityIntoExistingPantryItem() {
        launchScanner(
            existing = listOf(PantryItem("tomato-id", "TOMATE fresco", 1)),
            detectionResults = listOf(FoodDetectionResult.Success(listOf(DetectedFood("Tomate fresco", 0.75)))),
        )
        waitForItems(1)
        val draft = viewModel.uiState.value.items.single()
        composeRule.onNodeWithTag(ScannerTestTags.duplicateMark(draft.id))
            .assertTextEquals(locale(R.string.scanner_already_in_pantry))
        repeat(2) { composeRule.onNodeWithTag(ScannerTestTags.quantityIncrement(draft.id)).performClick() }
        composeRule.onNodeWithTag(ScannerTestTags.SAVE).performClick()
        composeRule.waitUntil(5_000) { showPantry.value }

        val items = runBlocking { repository.observePantryItems().first() }
        assertEquals(1, items.size)
        assertEquals("tomato-id", items.single().id)
        assertEquals("TOMATE fresco", items.single().name)
        assertEquals(3, items.single().quantity)
    }

    @Test
    fun saveAppliesAgreedQuantityEdgeCasesWithoutNewRows() {
        launchScanner(
            existing = listOf(
                PantryItem("tomato-id", "Tomate fresco", null),
                PantryItem("milk-id", "Leche", 3),
                PantryItem("rice-id", "Arroz", 60),
                PantryItem("salt-id", "Sal", null),
            ),
            detectionResults = listOf(
                FoodDetectionResult.Success(
                    listOf(
                        DetectedFood("Tomate fresco", 0.75),
                        DetectedFood("Leche", 0.88),
                        DetectedFood("Arroz", 0.72),
                        DetectedFood("Sal", 0.66),
                    ),
                ),
            ),
        )
        waitForItems(4)
        val chips = viewModel.uiState.value.items
        viewModel.updateQuantity(chips.first { it.name == "Tomate fresco" }.id, 2)
        viewModel.updateQuantity(chips.first { it.name == "Leche" }.id, 0)
        viewModel.updateQuantity(chips.first { it.name == "Arroz" }.id, 50)
        viewModel.updateQuantity(chips.first { it.name == "Sal" }.id, 0)
        composeRule.onNodeWithTag(ScannerTestTags.SAVE).performClick()
        composeRule.waitUntil(5_000) { showPantry.value }

        val saved = runBlocking { repository.observePantryItems().first() }
        assertEquals(4, saved.size)
        assertEquals(2, saved.first { it.id == "tomato-id" }.quantity)
        assertEquals(3, saved.first { it.id == "milk-id" }.quantity)
        assertEquals(99, saved.first { it.id == "rice-id" }.quantity)
        assertNull(saved.first { it.id == "salt-id" }.quantity)
    }

    @Test
    fun saveWithPantryMatchesNeverShowsDecisionDialog() {
        launchScanner(
            existing = listOf(PantryItem("tomato-id", "Tomate fresco", 3)),
            detectionResults = listOf(FoodDetectionResult.Success(listOf(DetectedFood("Tomate fresco", 0.75)))),
        )
        waitForItems(1)
        val draft = viewModel.uiState.value.items.single()
        composeRule.onNodeWithTag(ScannerTestTags.duplicateMark(draft.id))
            .assertIsDisplayed()
            .assertHasNoClickAction()
            .assertTextEquals(locale(R.string.scanner_already_in_pantry))
        composeRule.onNodeWithTag(ScannerTestTags.SAVE).performClick()
        composeRule.waitUntil(5_000) { showPantry.value }

        val saved = runBlocking { repository.observePantryItems().first() }
        assertEquals(1, saved.size)
        assertEquals(3, saved.single().quantity)
    }

    @Test
    fun liveFramesAreNeverWrittenToCacheOrFiles() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val cacheBefore = scanImages(context.cacheDir)
        val filesBefore = scanImages(context.filesDir)
        launchScanner(detectionResults = listOf(FoodDetectionResult.Success(listOf(DetectedFood("Plátano", 0.84)))))
        waitForItems(1)
        val id = viewModel.uiState.value.items.single().id
        composeRule.onNodeWithTag(ScannerTestTags.SAVE).performClick()
        composeRule.waitUntil(5_000) { showPantry.value }
        assertEquals(cacheBefore, scanImages(context.cacheDir))
        assertEquals(filesBefore, scanImages(context.filesDir))
        assertTrue(id.isNotBlank())
    }

    @Test
    fun quantityControlsHaveAtLeast48DpTargetsAndNoEditableQuantityField() {
        launchScanner(detectionResults = listOf(FoodDetectionResult.Success(listOf(DetectedFood("Plátano", 0.84)))))
        waitForItems(1)
        val id = viewModel.uiState.value.items.single().id
        val density = InstrumentationRegistry.getInstrumentation().targetContext.resources.displayMetrics.density
        val decrement = composeRule.onNodeWithTag(ScannerTestTags.quantityDecrement(id)).fetchSemanticsNode().boundsInRoot.height
        val increment = composeRule.onNodeWithTag(ScannerTestTags.quantityIncrement(id)).fetchSemanticsNode().boundsInRoot.height
        assertTrue(decrement / density >= 48f)
        assertTrue(increment / density >= 48f)
        composeRule.onNodeWithTag(ScannerTestTags.itemQuantity(id)).assertIsDisplayed()
        composeRule.onAllNodesWithTag(ScannerTestTags.itemQuantity(id).plus("_edit")).assertCountEquals(0)
    }

    private fun launchScanner(
        frames: List<ByteArray> = listOf(FRAME),
        detectionResults: List<FoodDetectionResult> = listOf(FoodDetectionResult.Success(listOf(DetectedFood("Plátano", 0.84)))),
        existing: List<PantryItem> = emptyList(),
        permissionGranted: Boolean = true,
        locale: Locale = Locale("es", "ES"),
    ) {
        repository = InMemoryPantryRepository()
        runBlocking { existing.forEach { repository.savePantryItem(it) } }
        val fakeSource = FakeLiveFrameSource(frames)
        detector = FakeFoodDetector(detectionResults)
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            viewModel = ScannerViewModel(repository, detector)
            if (permissionGranted) {
                viewModel.onCameraPermissionGranted()
                viewModel.onCameraPreviewReady()
                assertTrue("Fake camera setup: ${viewModel.uiState.value}", viewModel.uiState.value.cameraPreviewReady)
            } else {
                viewModel.onCameraPermissionDenied()
            }
        }
        showPantry = mutableStateOf(false)
        leftScanner = mutableStateOf(false)

        val baseContext = InstrumentationRegistry.getInstrumentation().targetContext
        val configuration = Configuration(baseContext.resources.configuration).apply { setLocale(locale) }
        localeContext = baseContext.createConfigurationContext(configuration)
        composeRule.setContent {
            val state by viewModel.uiState.collectAsState()
            val pantryItems by repository.observePantryItems().collectAsState(initial = emptyList())
            CompositionLocalProvider(
                LocalContext provides localeContext,
                LocalConfiguration provides localeContext.resources.configuration,
            ) {
                WhatsForLunchTheme {
                    when {
                        leftScanner.value -> androidx.compose.material3.Text("Scanner parent", Modifier.testTag(LEFT_AFTER_EXIT))
                        showPantry.value -> PantryScreen(
                            uiState = PantryUiState(items = pantryItems),
                            onBack = { showPantry.value = false },
                            onSaveItem = { _, _, _ -> },
                            onDeleteItem = {},
                            onErrorConsumed = {},
                            onSaveCompletedConsumed = {},
                        )
                        else -> {
                            LaunchedEffect(state.saveCompleted) {
                                if (state.saveCompleted) {
                                    showPantry.value = true
                                    viewModel.consumeSaveCompleted()
                                }
                            }
                            ScannerScreen(
                                uiState = state,
                                previewContent = {
                                    Box(Modifier.fillMaxWidth().height(240.dp).testTag(ScannerTestTags.CAMERA_PREVIEW))
                                },
                                onBack = { leftScanner.value = true },
                                onRescan = { if (viewModel.requestRescan()) viewModel.startLiveDetection(fakeSource) },
                                onRequestExit = viewModel::requestExit,
                                onConfirmExit = {
                                    if (viewModel.confirmExit()) viewModel.startLiveDetection(fakeSource) else leftScanner.value = true
                                },
                                onCancelExit = viewModel::cancelExitConfirmation,
                                onAddManualItem = viewModel::addManualItem,
                                onRenameItem = viewModel::renameItem,
                                onQuantityChange = viewModel::updateQuantity,
                                onRemoveItem = viewModel::removeItem,
                                onSave = viewModel::saveToPantry,
                                onClearNotice = viewModel::clearNotice,
                            )
                        }
                    }
                }
            }
        }
        composeRule.waitForIdle()
        if (permissionGranted) {
            viewModel.startLiveDetection(fakeSource)
        }
    }

    private fun waitForItems(minCount: Int) {
        composeRule.waitUntil(10_000) {
            viewModel.uiState.value.items.size >= minCount && !viewModel.uiState.value.isDetecting
        }
    }

    private fun waitForFrames(processedFrames: Int) {
        composeRule.waitUntil(10_000) {
            detector.calls >= processedFrames && !viewModel.uiState.value.isDetecting
        }
    }

    private fun locale(resourceId: Int, vararg args: Any): String = localeContext.getString(resourceId, *args)

    private fun scanImages(directory: File): Set<String> = directory.listFiles().orEmpty()
        .filter { it.extension.lowercase() in setOf("jpg", "jpeg", "png", "webp") }
        .map { it.name }
        .toSet()

    private class FakeLiveFrameSource(
        private val frames: List<ByteArray>,
        private val intervalMillis: Long = FRAME_INTERVAL_MS,
    ) : LiveFrameSource {
        override fun frames(): Flow<ByteArray> = flow {
            frames.forEachIndexed { index, frame ->
                if (index > 0) delay(intervalMillis)
                emit(frame)
            }
        }
    }

    private class FakeFoodDetector(private val results: List<FoodDetectionResult>) : FoodDetector {
        @Volatile var calls: Int = 0
            private set

        override suspend fun detect(image: ByteArray): FoodDetectionResult = results[minOf(calls++, results.lastIndex)]
    }

    private companion object {
        const val LEFT_AFTER_EXIT = "scanner_parent_after_exit"
        const val FRAME_INTERVAL_MS = 400L
        val FRAME = byteArrayOf(9, 9, 9)
    }
}
