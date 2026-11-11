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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.whatsforlunch.app.data.ai.AiFailureCause
import com.whatsforlunch.app.data.ai.ScanAnalysisResult
import com.whatsforlunch.app.data.ai.ScanImageAnalyzer
import com.whatsforlunch.app.data.repository.InMemoryPantryRepository
import com.whatsforlunch.app.data.scanner.ScanImageSource
import com.whatsforlunch.app.domain.model.PantryItem
import com.whatsforlunch.app.domain.scanner.AnalyzedFood
import com.whatsforlunch.app.ui.pantry.PantryScreen
import com.whatsforlunch.app.ui.pantry.PantryTestTags
import com.whatsforlunch.app.ui.pantry.PantryUiState
import com.whatsforlunch.app.ui.scanner.ScannerNotice
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
    private lateinit var analyzer: FakeScanImageAnalyzer
    private lateinit var imageSource: FakeScanImageSource
    private lateinit var showPantry: androidx.compose.runtime.MutableState<Boolean>
    private lateinit var leftScanner: androidx.compose.runtime.MutableState<Boolean>
    private lateinit var localeContext: Context

    @Before
    fun clearDatabase() {
        E2eDatabase.clear()
    }

    // RF-SDR7 / S5: el Escáner ya solo escanea; la sección de Recetas Compatibles y
    // su botón de IA viven ahora en «Recetas con mi Despensa» (0 hits aquí).
    @Test
    fun scannerNoLongerShowsCompatibleRecipesOrTheAiSuggestionButton() {
        launchScanner(
            analyzerResults = listOf(
                ScanAnalysisResult.Success(listOf(AnalyzedFood("Manzana", 3.0))),
            ),
        )
        captureAndWait()

        composeRule.onAllNodesWithTag(ScannerTestTags.FOOD_LIST).assertCountEquals(1)
        composeRule.onNodeWithText("Recetas Compatibles").assertDoesNotExist()
        composeRule.onNodeWithText("Sugerir receta con IA").assertDoesNotExist()
        composeRule.onAllNodesWithTag("scanner_compatible_section").assertCountEquals(0)
        composeRule.onAllNodesWithTag("scanner_compatible_suggest_ai").assertCountEquals(0)
    }

    @Test
    fun aiPhotoShowsChipsWithQuantitiesAndSavesThemToPantry() {
        launchScanner(
            analyzerResults = listOf(
                ScanAnalysisResult.Success(listOf(AnalyzedFood("Manzana", 3.0), AnalyzedFood("Nata", 1.0))),
            ),
        )
        captureAndWait()

        composeRule.onAllNodesWithTag(ScannerTestTags.FOOD_ITEM).assertCountEquals(2)
        assertFalse(viewModel.uiState.value.isScannerExpanded)
        composeRule.onAllNodesWithTag(ScannerTestTags.CAMERA_SECTION).assertCountEquals(0)
        composeRule.onNodeWithTag(ScannerTestTags.SCANNER_TOGGLE).assertIsDisplayed()
        composeRule.onAllNodesWithTag("scanner_rescan").assertCountEquals(0)
        val chips = viewModel.uiState.value.items
        composeRule.onNodeWithTag(ScannerTestTags.itemName(chips.first().id))
            .fetchSemanticsNode().config[SemanticsProperties.EditableText].text.let { assertEquals("Manzana", it) }
        composeRule.onNodeWithTag(ScannerTestTags.itemQuantity(chips.first().id)).assertTextEquals("3")
        composeRule.onNodeWithTag(ScannerTestTags.itemName(chips.last().id))
            .fetchSemanticsNode().config[SemanticsProperties.EditableText].text.let { assertEquals("Nata", it) }
        composeRule.onNodeWithTag(ScannerTestTags.itemQuantity(chips.last().id)).assertTextEquals("1")

        composeRule.onNodeWithTag(ScannerTestTags.SAVE).performClick()
        composeRule.waitUntil(5_000) { showPantry.value }
        composeRule.onNodeWithTag(PantryTestTags.SCREEN).assertIsDisplayed()
        composeRule.onNodeWithText("Manzana").assertIsDisplayed()
        composeRule.onNodeWithText(locale(R.string.pantry_quantity_value, 3)).assertIsDisplayed()

        val saved = runBlocking { repository.observePantryItems().first() }
        assertEquals(3, saved.first { it.name == "Manzana" }.quantity)
        assertEquals(1, saved.first { it.name == "Nata" }.quantity)
    }

    @Test
    fun blankNamesAreDiscardedAndInvalidQuantitiesAreNormalized() {
        launchScanner(
            analyzerResults = listOf(
                ScanAnalysisResult.Success(
                    listOf(
                        AnalyzedFood("   ", 4.0),
                        AnalyzedFood("Tomate", 0.5),
                        AnalyzedFood("  TOMATE ", 3.0),
                        AnalyzedFood("Leche", 150.0),
                        AnalyzedFood("Sal", null),
                    ),
                ),
            ),
        )
        captureAndWait()

        val items = viewModel.uiState.value.items
        assertEquals(listOf("Tomate", "Leche", "Sal"), items.map { it.name })
        assertEquals(listOf(3, 99, 0), items.map { it.quantity })
    }

    @Test
    fun successivePhotosAccumulateWithoutOverwritingEditedChips() {
        launchScanner(
            analyzerResults = listOf(
                ScanAnalysisResult.Success(listOf(AnalyzedFood("Manzana", 3.0))),
                ScanAnalysisResult.Success(listOf(AnalyzedFood("manzana", 5.0), AnalyzedFood("Pera", 1.0))),
            ),
        )
        captureAndWait()
        val id = viewModel.uiState.value.items.single().id
        viewModel.updateQuantity(id, 2)
        composeRule.waitForIdle()

        captureAndWait(calls = 2)

        val items = viewModel.uiState.value.items
        assertEquals(listOf("Manzana", "Pera"), items.map { it.name })
        assertEquals(2, items.first().quantity)
        assertEquals(id, items.first().id)
        assertEquals(1, items.last().quantity)
    }

    @Test
    fun manualFoodCanBeAddedWithQuantityAndSaved() {
        launchScanner()
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
            analyzerResults = listOf(
                ScanAnalysisResult.Success(
                    listOf(AnalyzedFood("Plátano", 2.0), AnalyzedFood("Manzana", 1.0)),
                ),
            ),
        )
        captureAndWait()
        val firstId = viewModel.uiState.value.items.first().id
        val secondId = viewModel.uiState.value.items.last().id

        composeRule.onNodeWithTag(ScannerTestTags.itemName(firstId)).performTextClearance()
        composeRule.onNodeWithTag(ScannerTestTags.itemName(firstId)).performTextInput("Pera")
        composeRule.onNodeWithTag(ScannerTestTags.removeItem(secondId)).performClick()
        composeRule.onNodeWithTag(ScannerTestTags.MANUAL_NAME).performTextInput("Arroz")
        composeRule.onNodeWithTag(ScannerTestTags.MANUAL_ADD).performClick()

        assertEquals(listOf("Pera", "Arroz"), viewModel.uiState.value.items.map { it.name })
        composeRule.onNodeWithTag(ScannerTestTags.SAVE).assertIsEnabled()
    }

    @Test
    fun detectedAndUserCreatedDuplicatesCollapseToOneChip() {
        launchScanner(
            analyzerResults = listOf(
                ScanAnalysisResult.Success(
                    listOf(AnalyzedFood("TOMATE FRESCO", 2.0), AnalyzedFood("  Tomate  fresco ", 3.0)),
                ),
            ),
        )
        captureAndWait()
        assertEquals(1, viewModel.uiState.value.items.size)
        assertEquals("TOMATE FRESCO", viewModel.uiState.value.items.single().name)
        assertEquals(5, viewModel.uiState.value.items.single().quantity)

        val id = viewModel.uiState.value.items.single().id
        composeRule.onNodeWithTag(ScannerTestTags.MANUAL_NAME).performTextInput("TOMATE  FRESCO")
        composeRule.onNodeWithTag(ScannerTestTags.MANUAL_ADD).performClick()
        assertEquals(1, viewModel.uiState.value.items.size)
        assertEquals(id, viewModel.uiState.value.items.single().id)
    }

    @Test
    fun quantityStepperHasBoundsAndNoEditableQuantityField() {
        // Los límites se prueban desde cerca de cada tope (98 y 1) en lugar de
        // recorrer el rango entero con ~197 toques.
        launchScanner(
            analyzerResults = listOf(
                ScanAnalysisResult.Success(
                    listOf(AnalyzedFood("Plátano", 98.0), AnalyzedFood("Pera", 1.0)),
                ),
            ),
        )
        captureAndWait()
        val upperId = viewModel.uiState.value.items.first().id
        val lowerId = viewModel.uiState.value.items.last().id

        composeRule.onNodeWithTag(ScannerTestTags.itemQuantity(upperId)).assertTextEquals("98")
        composeRule.onNodeWithTag(ScannerTestTags.quantityIncrement(upperId)).performClick()
        composeRule.onNodeWithTag(ScannerTestTags.itemQuantity(upperId)).assertTextEquals("99")
        composeRule.onNodeWithTag(ScannerTestTags.quantityIncrement(upperId)).assertIsNotEnabled()
        assertFalse(
            composeRule.onNodeWithTag(ScannerTestTags.itemQuantity(upperId))
                .fetchSemanticsNode().config.contains(SemanticsActions.SetText),
        )

        composeRule.onNodeWithTag(ScannerTestTags.itemQuantity(lowerId)).assertTextEquals("1")
        composeRule.onNodeWithTag(ScannerTestTags.quantityDecrement(lowerId)).performClick()
        composeRule.onNodeWithTag(ScannerTestTags.itemQuantity(lowerId)).assertTextEquals("0")
        composeRule.onNodeWithTag(ScannerTestTags.quantityDecrement(lowerId)).assertIsNotEnabled()
    }

    @Test
    fun deniedPermissionContinuesWithManualEntryAndCollapsedScanner() {
        launchScanner(permissionGranted = false)
        composeRule.onNodeWithText(locale(R.string.scanner_camera_denied)).assertIsDisplayed()
        composeRule.onAllNodesWithTag(ScannerTestTags.CAPTURE).assertCountEquals(0)
        composeRule.onAllNodesWithTag(ScannerTestTags.CAMERA_SECTION).assertCountEquals(0)
        composeRule.onNodeWithTag(ScannerTestTags.SCANNER_TOGGLE).assertIsDisplayed()
        composeRule.onNodeWithTag(ScannerTestTags.MANUAL_NAME).performTextInput("Harina")
        composeRule.onNodeWithTag(ScannerTestTags.MANUAL_ADD).performClick()
        composeRule.onNodeWithText("Harina").assertIsDisplayed()
        composeRule.onNodeWithTag(ScannerTestTags.SAVE).assertIsEnabled()
    }

    @Test
    fun manualEntryIsFirstContentAndCameraPanelStartsCollapsed() {
        launchScanner()

        composeRule.onNodeWithTag(ScannerTestTags.MANUAL_NAME).assertIsDisplayed()
        composeRule.onNodeWithTag(ScannerTestTags.SCANNER_TOGGLE).assertIsDisplayed()
        composeRule.onAllNodesWithTag(ScannerTestTags.CAMERA_SECTION).assertCountEquals(0)
        composeRule.onAllNodesWithTag(ScannerTestTags.CAPTURE).assertCountEquals(0)

        val manualTop = composeRule.onNodeWithTag(ScannerTestTags.MANUAL_NAME)
            .fetchSemanticsNode().boundsInRoot.top
        val hintTop = composeRule.onNodeWithText(locale(R.string.scanner_capture_hint))
            .fetchSemanticsNode().boundsInRoot.top
        assertTrue(manualTop < hintTop)
    }

    @Test
    fun cameraToggleExpandsThePanelAndCloseCollapsesIt() {
        launchScanner()

        composeRule.onNodeWithTag(ScannerTestTags.SCANNER_TOGGLE).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(ScannerTestTags.CAMERA_SECTION).assertIsDisplayed()
        composeRule.onNodeWithTag(ScannerTestTags.CAMERA_PREVIEW).assertIsDisplayed()
        composeRule.onNodeWithTag(ScannerTestTags.CAMERA_CLOSE).performClick()
        composeRule.waitForIdle()

        composeRule.onAllNodesWithTag(ScannerTestTags.CAMERA_SECTION).assertCountEquals(0)
        composeRule.onNodeWithTag(ScannerTestTags.SCANNER_TOGGLE).assertIsDisplayed()
        assertFalse(viewModel.uiState.value.isScannerExpanded)
    }

    @Test
    fun emptyAiResponseShowsCaptureHintAndManualEntryCanSave() {
        launchScanner(analyzerResults = listOf(ScanAnalysisResult.Success(emptyList())))
        captureAndWait()
        composeRule.onAllNodesWithTag(ScannerTestTags.FOOD_ITEM).assertCountEquals(0)
        assertFalse(viewModel.uiState.value.isScannerExpanded)
        composeRule.onAllNodesWithTag(ScannerTestTags.CAMERA_SECTION).assertCountEquals(0)
        composeRule.onNodeWithText(locale(R.string.scanner_no_detections)).assertIsDisplayed()
        composeRule.onNodeWithText(locale(R.string.scanner_capture_hint)).assertIsDisplayed()
        composeRule.onNodeWithTag(ScannerTestTags.MANUAL_NAME).performTextInput("Lentejas")
        composeRule.onNodeWithTag(ScannerTestTags.MANUAL_ADD).performClick()
        composeRule.onNodeWithTag(ScannerTestTags.SAVE).performClick()
        composeRule.waitUntil(5_000) { showPantry.value }
        composeRule.onNodeWithText("Lentejas").assertIsDisplayed()
    }

    @Test
    fun missingApiKeyShowsNoticeAndKeepsManualFlowAvailable() {
        launchScanner(analyzerResults = listOf(ScanAnalysisResult.MissingApiKey))
        captureAndWait()

        composeRule.onNodeWithText(locale(R.string.scanner_ai_key_missing)).assertIsDisplayed()
        assertFalse(viewModel.uiState.value.isScannerExpanded)
        composeRule.onAllNodesWithTag(ScannerTestTags.CAMERA_SECTION).assertCountEquals(0)
        composeRule.onNodeWithTag(ScannerTestTags.MANUAL_NAME).performTextInput("Garbanzos")
        composeRule.onNodeWithTag(ScannerTestTags.MANUAL_ADD).performClick()
        composeRule.onNodeWithTag(ScannerTestTags.SAVE).assertIsEnabled()
    }

    @Test
    fun providerFailureShowsNoticeAndTheNextPhotoCanRetry() {
        launchScanner(
            analyzerResults = listOf(
                ScanAnalysisResult.Failed(AiFailureCause.PROVIDER_ERROR),
                ScanAnalysisResult.Success(listOf(AnalyzedFood("Plátano", 2.0))),
            ),
        )
        captureAndWait()

        composeRule.onNodeWithText(locale(R.string.scanner_analysis_failed_provider)).assertIsDisplayed()
        composeRule.onAllNodesWithTag(ScannerTestTags.FOOD_ITEM).assertCountEquals(0)
        assertFalse(viewModel.uiState.value.isScannerExpanded)
        composeRule.onAllNodesWithTag(ScannerTestTags.CAMERA_SECTION).assertCountEquals(0)
        composeRule.onNodeWithTag(ScannerTestTags.MANUAL_NAME).performTextInput("Garbanzos")
        composeRule.onNodeWithTag(ScannerTestTags.MANUAL_ADD).performClick()
        composeRule.onNodeWithTag(ScannerTestTags.SAVE).assertIsEnabled()

        composeRule.onNodeWithTag(ScannerTestTags.MANUAL_NAME).performTextClearance()
        captureAndWait(calls = 2)
        assertEquals(2, analyzer.calls)
        composeRule.onNodeWithText("Plátano").assertIsDisplayed()
    }

    // S1 (verify de feature_list.json): fallo de red, aviso descartable y alta manual
    // que se guarda en la Despensa; un nuevo fallo vuelve a mostrar su causa.
    @Test
    fun failureNoticeAllowsManualEntryAndSaving() {
        launchScanner(
            analyzerResults = listOf(
                ScanAnalysisResult.Failed(AiFailureCause.NO_CONNECTION),
                ScanAnalysisResult.Failed(AiFailureCause.NO_CONNECTION),
            ),
        )
        captureAndWait()

        composeRule.onNodeWithText(locale(R.string.scanner_analysis_failed_no_connection)).assertIsDisplayed()
        assertTrue(viewModel.uiState.value.manualMode)
        assertEquals(ScannerNotice.ANALYSIS_NO_CONNECTION, viewModel.uiState.value.notice)

        composeRule.onNodeWithContentDescription(locale(R.string.action_dismiss)).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(locale(R.string.scanner_analysis_failed_no_connection)).assertDoesNotExist()
        assertNull(viewModel.uiState.value.notice)

        captureAndWait(calls = 2)
        composeRule.onNodeWithText(locale(R.string.scanner_analysis_failed_no_connection)).assertIsDisplayed()

        composeRule.onNodeWithTag(ScannerTestTags.MANUAL_NAME).performTextInput("Lentejas")
        composeRule.onNodeWithTag(ScannerTestTags.MANUAL_ADD).performClick()
        composeRule.onNodeWithTag(ScannerTestTags.SAVE).performClick()
        composeRule.waitUntil(5_000) { showPantry.value }
        composeRule.onNodeWithText("Lentejas").assertIsDisplayed()
    }

    @Test
    fun invalidResponseShowsNoticeAndKeepsManualEntry() {
        launchScanner(analyzerResults = listOf(ScanAnalysisResult.Failed(AiFailureCause.INVALID_RESPONSE)))
        captureAndWait()

        composeRule.onNodeWithText(locale(R.string.scanner_analysis_failed_invalid_response)).assertIsDisplayed()
        assertEquals(ScannerNotice.ANALYSIS_INVALID_RESPONSE, viewModel.uiState.value.notice)
        composeRule.onNodeWithTag(ScannerTestTags.MANUAL_NAME).performTextInput("Arroz")
        composeRule.onNodeWithTag(ScannerTestTags.MANUAL_ADD).performClick()
        composeRule.onNodeWithText("Arroz").assertIsDisplayed()
        composeRule.onNodeWithTag(ScannerTestTags.SAVE).assertIsEnabled()
    }

    @Test
    fun failureNoticeIsLocalizedInEnglish() {
        launchScanner(
            analyzerResults = listOf(ScanAnalysisResult.Failed(AiFailureCause.NO_CONNECTION)),
            locale = Locale.ENGLISH,
        )
        captureAndWait()

        composeRule.onNodeWithText(
            "No connection. Check your network and try again; you can also continue manually.",
        ).assertIsDisplayed()
        composeRule.onNodeWithTag(ScannerTestTags.MANUAL_NAME).performTextInput("Rice")
        composeRule.onNodeWithTag(ScannerTestTags.MANUAL_ADD).performClick()
        composeRule.onNodeWithText("Rice").assertIsDisplayed()
    }

    @Test
    fun englishLabelsAndPromptLanguageFollowTheActiveLocale() {
        launchScanner(
            analyzerResults = listOf(
                ScanAnalysisResult.Success(listOf(AnalyzedFood("Banana", 3.0))),
            ),
            locale = Locale.ENGLISH,
        )
        composeRule.onNodeWithText("Pantry Scanner").assertIsDisplayed()
        captureAndWait()

        composeRule.onNodeWithText("Banana").assertIsDisplayed()
        composeRule.onNodeWithText("Save to Pantry").assertIsDisplayed()
        composeRule.onNodeWithText("Scan with the camera").assertIsDisplayed()
        assertEquals("en", analyzer.lastLanguage)
        composeRule.onNodeWithContentDescription("Decrease quantity").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Increase quantity").assertIsDisplayed()
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
    fun scannerHasNoRescanActionAndPerChipDeleteRemovesOnlyThatFood() {
        launchScanner(
            analyzerResults = listOf(
                ScanAnalysisResult.Success(
                    listOf(AnalyzedFood("Plátano", 2.0), AnalyzedFood("Manzana", 1.0)),
                ),
            ),
        )
        captureAndWait()

        composeRule.onAllNodesWithTag("scanner_rescan").assertCountEquals(0)
        val removed = viewModel.uiState.value.items.first()
        composeRule.onNodeWithTag(ScannerTestTags.removeItem(removed.id)).performClick()

        assertEquals(1, viewModel.uiState.value.items.size)
        assertTrue(viewModel.uiState.value.items.none { it.id == removed.id })
        composeRule.onNodeWithText(removed.name).assertDoesNotExist()
    }

    @Test
    fun saveSumsChipQuantityIntoExistingPantryItem() {
        launchScanner(
            existing = listOf(PantryItem("tomato-id", "TOMATE fresco", 1)),
            analyzerResults = listOf(
                ScanAnalysisResult.Success(listOf(AnalyzedFood("Tomate fresco", 2.0))),
            ),
        )
        captureAndWait()
        val draft = viewModel.uiState.value.items.single()
        composeRule.onNodeWithTag(ScannerTestTags.duplicateMark(draft.id))
            .assertTextEquals(locale(R.string.scanner_already_in_pantry))
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
            analyzerResults = listOf(
                ScanAnalysisResult.Success(
                    listOf(
                        AnalyzedFood("Tomate fresco", 2.0),
                        AnalyzedFood("Leche", 0.0),
                        AnalyzedFood("Arroz", 50.0),
                        AnalyzedFood("Sal", 0.0),
                    ),
                ),
            ),
        )
        captureAndWait()
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
            analyzerResults = listOf(
                ScanAnalysisResult.Success(listOf(AnalyzedFood("Tomate fresco", 1.0))),
            ),
        )
        captureAndWait()
        val draft = viewModel.uiState.value.items.single()
        composeRule.onNodeWithTag(ScannerTestTags.duplicateMark(draft.id))
            .assertIsDisplayed()
            .assertHasNoClickAction()
            .assertTextEquals(locale(R.string.scanner_already_in_pantry))
        composeRule.onNodeWithTag(ScannerTestTags.SAVE).performClick()
        composeRule.waitUntil(5_000) { showPantry.value }

        val saved = runBlocking { repository.observePantryItems().first() }
        assertEquals(1, saved.size)
        assertEquals(4, saved.single().quantity)
    }

    @Test
    fun captureIsNeverWrittenToCacheOrFiles() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val cacheBefore = scanImages(context.cacheDir)
        val filesBefore = scanImages(context.filesDir)
        launchScanner(
            analyzerResults = listOf(
                ScanAnalysisResult.Success(listOf(AnalyzedFood("Plátano", 1.0))),
            ),
        )
        captureAndWait()
        assertEquals(1, imageSource.calls)
        composeRule.onNodeWithTag(ScannerTestTags.SAVE).performClick()
        composeRule.waitUntil(5_000) { showPantry.value }
        assertEquals(cacheBefore, scanImages(context.cacheDir))
        assertEquals(filesBefore, scanImages(context.filesDir))
    }

    @Test
    fun quantityControlsHaveAtLeast48DpTargetsAndNoEditableQuantityField() {
        launchScanner(
            analyzerResults = listOf(
                ScanAnalysisResult.Success(listOf(AnalyzedFood("Plátano", 1.0))),
            ),
        )
        captureAndWait()
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
        analyzerResults: List<ScanAnalysisResult> = listOf(ScanAnalysisResult.Success(emptyList())),
        existing: List<PantryItem> = emptyList(),
        permissionGranted: Boolean = true,
        locale: Locale = Locale.forLanguageTag("es-ES"),
    ) {
        repository = InMemoryPantryRepository()
        runBlocking { existing.forEach { repository.savePantryItem(it) } }
        imageSource = FakeScanImageSource()
        analyzer = FakeScanImageAnalyzer(analyzerResults)
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            viewModel = ScannerViewModel(repository, analyzer)
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
                            onScanFood = {},
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
                                onOpenScanner = viewModel::openScanner,
                                onCloseScanner = viewModel::closeScanner,
                                onCapture = { viewModel.captureAndAnalyze(imageSource, locale.language) },
                                onRequestExit = viewModel::requestExit,
                                onConfirmExit = {
                                    viewModel.confirmExit()
                                    leftScanner.value = true
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
    }

    private fun captureAndWait(calls: Int = 1) {
        if (composeRule.onAllNodesWithTag(ScannerTestTags.SCANNER_TOGGLE).fetchSemanticsNodes().isNotEmpty()) {
            composeRule.onNodeWithTag(ScannerTestTags.SCANNER_TOGGLE).performClick()
        }
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            viewModel.onCameraPreviewReady()
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(ScannerTestTags.CAPTURE).performClick()
        composeRule.waitUntil(10_000) {
            analyzer.calls >= calls && !viewModel.uiState.value.isAnalyzing
        }
    }

    private fun locale(resourceId: Int, vararg args: Any): String = localeContext.getString(resourceId, *args)

    private fun scanImages(directory: File): Set<String> = directory.listFiles().orEmpty()
        .filter { it.extension.lowercase() in setOf("jpg", "jpeg", "png", "webp") }
        .map { it.name }
        .toSet()

    private class FakeScanImageSource : ScanImageSource {
        @Volatile var calls: Int = 0
            private set

        override suspend fun capture(): ByteArray {
            calls++
            return FRAME
        }
    }

    private class FakeScanImageAnalyzer(private val results: List<ScanAnalysisResult>) : ScanImageAnalyzer {
        @Volatile var calls: Int = 0
            private set
        @Volatile var lastLanguage: String? = null
            private set

        override suspend fun analyze(image: ByteArray, language: String): ScanAnalysisResult {
            lastLanguage = language
            return results[minOf(calls++, results.lastIndex)]
        }
    }

    private companion object {
        const val LEFT_AFTER_EXIT = "scanner_parent_after_exit"
        val FRAME = byteArrayOf(9, 9, 9)
    }
}
