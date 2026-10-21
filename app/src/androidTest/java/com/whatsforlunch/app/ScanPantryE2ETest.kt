package com.whatsforlunch.app

import android.content.Context
import android.content.res.Configuration
import android.view.KeyEvent
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
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.whatsforlunch.app.data.repository.InMemoryPantryRepository
import com.whatsforlunch.app.data.scanner.FoodDetectionResult
import com.whatsforlunch.app.data.scanner.FoodDetector
import com.whatsforlunch.app.data.scanner.ScanCaptureResult
import com.whatsforlunch.app.data.scanner.ScanImageSource
import com.whatsforlunch.app.domain.model.DetectedFood
import com.whatsforlunch.app.domain.model.PantryItem
import com.whatsforlunch.app.domain.repository.PantryDuplicateResolution
import com.whatsforlunch.app.ui.pantry.PantryScreen
import com.whatsforlunch.app.ui.pantry.PantryTestTags
import com.whatsforlunch.app.ui.pantry.PantryUiState
import com.whatsforlunch.app.ui.scanner.ScannerScreen
import com.whatsforlunch.app.ui.scanner.ScannerTestTags
import com.whatsforlunch.app.ui.scanner.ScannerViewModel
import com.whatsforlunch.app.ui.theme.WhatsForLunchTheme
import java.io.File
import java.util.Locale
import kotlinx.coroutines.flow.first
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
    private lateinit var imageSource: FakeScanImageSource
    private lateinit var showPantry: androidx.compose.runtime.MutableState<Boolean>
    private lateinit var leftScanner: androidx.compose.runtime.MutableState<Boolean>
    private lateinit var localeContext: Context

    @Before
    fun clearDatabase() {
        E2eDatabase.clear()
    }

    @Test
    fun injectedCaptureShowsFoodChipConfidenceAndHudLabel() {
        launchScanner(detections = listOf(DetectedFood("Plátano", 0.84)))
        assertTrue("Fake scanner should expose the shutter", viewModel.uiState.value.cameraPreviewReady)
        composeRule.waitUntil(5_000) { viewModel.uiState.value.pantryLoaded }
        composeRule.onNodeWithTag(ScannerTestTags.CAPTURE).performClick()
        waitForDetection()

        composeRule.onNodeWithTag(ScannerTestTags.CAPTURED_IMAGE).assertIsDisplayed()
        composeRule.onNodeWithText("Plátano · 84%").assertIsDisplayed()
        val id = viewModel.uiState.value.items.single().id
        composeRule.onNodeWithTag(ScannerTestTags.itemName(id)).assertIsDisplayed()
        composeRule.onNodeWithTag(ScannerTestTags.itemConfidence(id)).assertTextEquals("84%")
    }

    @Test
    fun savePersistsChosenQuantityAndShowsPantryScreen() {
        launchScanner(detections = listOf(DetectedFood("Plátano", 0.84)))
        composeRule.onNodeWithTag(ScannerTestTags.CAPTURE).performClick()
        waitForDetection()
        val id = viewModel.uiState.value.items.single().id
        repeat(3) { composeRule.onNodeWithTag(ScannerTestTags.quantityIncrement(id)).performClick() }
        composeRule.onNodeWithTag(ScannerTestTags.SAVE).performClick()

        composeRule.waitUntil(5_000) { showPantry.value }
        composeRule.onNodeWithTag(PantryTestTags.SCREEN).assertIsDisplayed()
        composeRule.onNodeWithText("Plátano").assertIsDisplayed()
        composeRule.onNodeWithText(locale(R.string.pantry_quantity_value, 3)).assertIsDisplayed()
        assertEquals(3, runBlocking { repository.observePantryItems().first().single().quantity })
        assertTrue(viewModel.uiState.value.capturedImage == null)
    }

    @Test
    fun chipsCanBeRemovedRenamedAndAddedManually() {
        launchScanner(detections = listOf(DetectedFood("Plátano", 0.84), DetectedFood("Manzana", 0.73)))
        composeRule.onNodeWithTag(ScannerTestTags.CAPTURE).performClick()
        waitForDetection()
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
            detections = listOf(
                DetectedFood("TOMATE FRESCO", 0.65),
                DetectedFood("  Tomate  fresco ", 0.81),
            ),
        )
        composeRule.onNodeWithTag(ScannerTestTags.CAPTURE).performClick()
        waitForDetection()
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
        launchScanner(detections = listOf(DetectedFood("Plátano", 0.84)))
        composeRule.onNodeWithTag(ScannerTestTags.CAPTURE).performClick()
        waitForDetection()
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
        composeRule.onAllNodesWithTag(ScannerTestTags.CAPTURE).assertCountEquals(0)
        composeRule.onNodeWithTag(ScannerTestTags.MANUAL_NAME).performTextInput("Harina")
        composeRule.onNodeWithTag(ScannerTestTags.MANUAL_ADD).performClick()
        composeRule.onNodeWithText("Harina").assertIsDisplayed()
        composeRule.onNodeWithTag(ScannerTestTags.SAVE).assertIsEnabled()
    }

    @Test
    fun emptyDetectionShowsManualEntryAndCanSave() {
        launchScanner(detections = emptyList())
        composeRule.onNodeWithTag(ScannerTestTags.CAPTURE).performClick()
        waitForDetection()
        composeRule.onNodeWithText(locale(R.string.scanner_no_detections)).assertIsDisplayed()
        composeRule.onNodeWithTag(ScannerTestTags.MANUAL_NAME).performTextInput("Lentejas")
        composeRule.onNodeWithTag(ScannerTestTags.MANUAL_ADD).performClick()
        composeRule.onNodeWithTag(ScannerTestTags.SAVE).performClick()
        composeRule.waitUntil(5_000) { showPantry.value }
        composeRule.onNodeWithText("Lentejas").assertIsDisplayed()
    }

    @Test
    fun detectorFailureShowsNoticeAndKeepsManualFlowAvailable() {
        launchScanner(detectionResult = FoodDetectionResult.Failed)
        composeRule.onNodeWithTag(ScannerTestTags.CAPTURE).performClick()
        waitForDetection()
        composeRule.onNodeWithText(locale(R.string.scanner_detection_failed)).assertIsDisplayed()
        composeRule.onNodeWithTag(ScannerTestTags.MANUAL_NAME).performTextInput("Garbanzos")
        composeRule.onNodeWithTag(ScannerTestTags.MANUAL_ADD).performClick()
        composeRule.onNodeWithTag(ScannerTestTags.SAVE).assertIsEnabled()
    }

    @Test
    fun englishLabelsAndScannerActionsAreLocalized() {
        launchScanner(detections = listOf(DetectedFood("Banana", 0.84)), locale = Locale.ENGLISH)
        composeRule.onNodeWithText("Pantry Scanner").assertIsDisplayed()
        composeRule.onNodeWithTag(ScannerTestTags.CAPTURE).performClick()
        waitForDetection()
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
    fun rescanningWithUnsavedChipsRequiresConfirmation() {
        launchScanner(detections = listOf(DetectedFood("Plátano", 0.84)))
        composeRule.onNodeWithTag(ScannerTestTags.CAPTURE).performClick()
        waitForDetection()
        composeRule.onNodeWithTag(ScannerTestTags.RESCAN).performClick()
        composeRule.onNodeWithTag(ScannerTestTags.EXIT_DIALOG).assertIsDisplayed()
        composeRule.onNodeWithTag(ScannerTestTags.EXIT_CANCEL).performClick()
        composeRule.onNodeWithText("Plátano").assertIsDisplayed()

        composeRule.onNodeWithTag(ScannerTestTags.RESCAN).performClick()
        composeRule.onNodeWithTag(ScannerTestTags.EXIT_CONFIRM).performClick()
        waitForDetection()
        assertTrue(viewModel.uiState.value.items.isNotEmpty())
        assertFalse(viewModel.uiState.value.showExitConfirmation)
    }

    @Test
    fun pantryMatchOpensDecisionDialogAndKeepOrUpdateReusesExistingRow() {
        launchScanner(
            existing = listOf(PantryItem("tomato-id", "TOMATE fresco", 1)),
            detections = listOf(DetectedFood("Tomate fresco", 0.75)),
        )
        composeRule.waitForIdle()
        assertTrue("Preview state should enable the shutter: ${viewModel.uiState.value}", viewModel.uiState.value.cameraPreviewReady)
        composeRule.onNodeWithTag(ScannerTestTags.CAPTURE).assertIsDisplayed()
        composeRule.onNodeWithTag(ScannerTestTags.CAPTURE).performClick()
        waitForDetection()
        val draft = viewModel.uiState.value.items.single()
        composeRule.onNodeWithTag(ScannerTestTags.DUPLICATE_DIALOG).assertIsDisplayed()
        composeRule.onNodeWithTag(ScannerTestTags.DUPLICATE_TITLE)
            .assertTextEquals(locale(R.string.scanner_duplicate_title))
        composeRule.onNodeWithTag(ScannerTestTags.DUPLICATE_MESSAGE)
            .assertTextEquals(locale(R.string.scanner_duplicate_message, "Tomate fresco", locale(R.string.scanner_duplicate_existing_quantity, 1)))
        composeRule.onAllNodesWithText("Cantidad: 1", substring = true).assertCountEquals(1)
        composeRule.onNodeWithTag(ScannerTestTags.DUPLICATE_KEEP).performClick()
        assertEquals(PantryDuplicateResolution.KeepExisting, viewModel.uiState.value.duplicateDecisions[draft.id])
        composeRule.onNodeWithTag(ScannerTestTags.duplicateMark(draft.id)).performClick()
        composeRule.onNodeWithTag(ScannerTestTags.DUPLICATE_UPDATE).performClick()
        composeRule.onNodeWithTag(ScannerTestTags.SAVE).performClick()
        composeRule.waitUntil(5_000) { showPantry.value }

        val items = runBlocking { repository.observePantryItems().first() }
        assertEquals(1, items.size)
        assertEquals("tomato-id", items.single().id)
        assertEquals(null, items.single().quantity)
    }

    @Test
    fun multiplePendingMatchesAskSequentiallyAndDismissAbortsWholeSave() {
        launchScanner(
            existing = listOf(PantryItem("tomato-id", "Tomate fresco", 1), PantryItem("milk-id", "Leche", 2)),
            detections = listOf(DetectedFood("Tomate fresco", 0.75), DetectedFood("Leche", 0.88)),
        )
        composeRule.onNodeWithTag(ScannerTestTags.CAPTURE).performClick()
        waitForDetection()
        val items = viewModel.uiState.value.items
        val tomato = items.first { it.name == "Tomate fresco" }
        val milk = items.first { it.name == "Leche" }

        composeRule.onNodeWithTag(ScannerTestTags.DUPLICATE_DIALOG).assertIsDisplayed()
        composeRule.onNodeWithTag(ScannerTestTags.DUPLICATE_UPDATE).performClick()
        composeRule.onNodeWithTag(ScannerTestTags.DUPLICATE_DIALOG).assertIsDisplayed()
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(ScannerTestTags.SAVE).performClick()
        composeRule.onNodeWithTag(ScannerTestTags.DUPLICATE_DIALOG).assertIsDisplayed()
        assertEquals(2, runBlocking { repository.observePantryItems().first().size })
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        composeRule.waitForIdle()
        assertFalse(viewModel.uiState.value.pendingSave)
        assertEquals(2, runBlocking { repository.observePantryItems().first().size })
        assertEquals(PantryDuplicateResolution.UpdateQuantity, viewModel.uiState.value.duplicateDecisions[tomato.id])
        assertEquals(null, viewModel.uiState.value.duplicateDecisions[milk.id])

        composeRule.onNodeWithTag(ScannerTestTags.SAVE).performClick()
        composeRule.onNodeWithTag(ScannerTestTags.DUPLICATE_KEEP).performClick()
        composeRule.waitUntil(5_000) { showPantry.value }
        val saved = runBlocking { repository.observePantryItems().first() }
        assertEquals(2, saved.size)
        assertNull(saved.first { it.id == "tomato-id" }.quantity)
        assertEquals(2, saved.first { it.id == "milk-id" }.quantity)
    }

    @Test
    fun capturedImageIsNeverWrittenToCacheOrFiles() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val cacheBefore = scanImages(context.cacheDir)
        val filesBefore = scanImages(context.filesDir)
        launchScanner(detections = listOf(DetectedFood("Plátano", 0.84)))
        composeRule.onNodeWithTag(ScannerTestTags.CAPTURE).performClick()
        waitForDetection()
        val id = viewModel.uiState.value.items.single().id
        composeRule.onNodeWithTag(ScannerTestTags.SAVE).performClick()
        composeRule.waitUntil(5_000) { showPantry.value }
        assertEquals(cacheBefore, scanImages(context.cacheDir))
        assertEquals(filesBefore, scanImages(context.filesDir))
        assertTrue(id.isNotBlank())
    }

    @Test
    fun quantityControlsHaveAtLeast48DpTargetsAndNoEditableQuantityField() {
        launchScanner(detections = listOf(DetectedFood("Plátano", 0.84)))
        composeRule.onNodeWithTag(ScannerTestTags.CAPTURE).performClick()
        waitForDetection()
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
        detections: List<DetectedFood> = listOf(DetectedFood("Plátano", 0.84)),
        detectionResult: FoodDetectionResult = FoodDetectionResult.Success(detections),
        existing: List<PantryItem> = emptyList(),
        permissionGranted: Boolean = true,
        locale: Locale = Locale("es", "ES"),
    ) {
        repository = InMemoryPantryRepository()
        runBlocking { existing.forEach { repository.savePantryItem(it) } }
        val fakeSource = FakeScanImageSource(
            InstrumentationRegistry.getInstrumentation().context.assets.open("scanner/banana.jpg").use { it.readBytes() },
        )
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            viewModel = ScannerViewModel(repository, FakeFoodDetector(detectionResult))
            imageSource = fakeSource
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
                                onCapture = { viewModel.capture(imageSource) },
                                onRescan = { if (viewModel.requestRescan()) viewModel.capture(imageSource) },
                                onRequestExit = viewModel::requestExit,
                                onConfirmExit = {
                                    if (viewModel.confirmExit()) viewModel.capture(imageSource) else leftScanner.value = true
                                },
                                onCancelExit = viewModel::cancelExitConfirmation,
                                onAddManualItem = viewModel::addManualItem,
                                onRenameItem = viewModel::renameItem,
                                onQuantityChange = viewModel::updateQuantity,
                                onRemoveItem = viewModel::removeItem,
                                onSave = viewModel::saveToPantry,
                                onOpenDuplicate = viewModel::openDuplicateResolution,
                                onResolveDuplicate = viewModel::resolveDuplicate,
                                onDismissDuplicate = viewModel::dismissDuplicateResolution,
                                onClearNotice = viewModel::clearNotice,
                            )
                        }
                    }
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun waitForDetection() {
        composeRule.waitUntil(10_000) { !viewModel.uiState.value.isCapturing && !viewModel.uiState.value.isDetecting }
    }

    private fun locale(resourceId: Int, vararg args: Any): String = localeContext.getString(resourceId, *args)

    private fun scanImages(directory: File): Set<String> = directory.listFiles().orEmpty()
        .filter { it.extension.lowercase() in setOf("jpg", "jpeg", "png", "webp") }
        .map { it.name }
        .toSet()

    private class FakeScanImageSource(private val imageBytes: ByteArray) : ScanImageSource {
        override suspend fun capture(): ScanCaptureResult = ScanCaptureResult.Success(imageBytes)
    }

    private class FakeFoodDetector(private val result: FoodDetectionResult) : FoodDetector {
        override suspend fun detect(image: ByteArray): FoodDetectionResult = result
    }

    private companion object {
        const val LEFT_AFTER_EXIT = "scanner_parent_after_exit"
    }
}
