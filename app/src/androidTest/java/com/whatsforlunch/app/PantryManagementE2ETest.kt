package com.whatsforlunch.app

import android.content.res.Configuration
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextClearance
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.whatsforlunch.app.R
import com.whatsforlunch.app.data.settings.AppLocaleController
import com.whatsforlunch.app.domain.model.PantryItem
import com.whatsforlunch.app.domain.repository.PantryRepository
import com.whatsforlunch.app.ui.myrecipes.MyRecipesTestTags
import com.whatsforlunch.app.ui.nav.AppBottomBarTestTags
import com.whatsforlunch.app.ui.pantry.PantryTestTags
import com.whatsforlunch.app.ui.scanner.ScannerTestTags
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext

@RunWith(AndroidJUnit4::class)
class PantryManagementE2ETest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun clearDatabase() {
        E2eDatabase.clear()
    }

    @Test
    fun opensPantryFromMyRecipesAndShowsEmptyState() {
        composeRule.onNodeWithTag(AppBottomBarTestTags.TAB_PANTRY).assertIsDisplayed().performClick()
        composeRule.onNodeWithTag(PantryTestTags.SCREEN).assertIsDisplayed()
        composeRule.onNodeWithTag(PantryTestTags.EMPTY_TITLE).assertIsDisplayed()
        composeRule.onNodeWithTag(PantryTestTags.EMPTY_MESSAGE).assertIsDisplayed()
        composeRule.onNodeWithTag(PantryTestTags.ADD_ITEM).assertIsDisplayed()
        composeRule.onNodeWithTag(PantryTestTags.SECTION_SUBTITLE).assertIsDisplayed()
    }

    @Test
    fun addEditAndPersistPantryItemAcrossActivityRecreation() {
        openPantry()
        addItem("Tomate fresco")
        composeRule.onNodeWithText("Tomate fresco").assertIsDisplayed()

        // Edición desde el menú ⋮ de la tarjeta (D4).
        openItemMenu("Tomate fresco")
        composeRule.onNodeWithContentDescription("${localized(R.string.pantry_edit_accessibility)} Tomate fresco")
            .performClick()
        composeRule.onNodeWithTag(PantryTestTags.NAME_FIELD).performTextClearance()
        composeRule.onNodeWithTag(PantryTestTags.NAME_FIELD).performTextInput("Tomate maduro")
        repeat(3) { composeRule.onNodeWithTag(PantryTestTags.QUANTITY_INCREMENT).performClick() }
        composeRule.onNodeWithTag(PantryTestTags.SAVE_ITEM).performClick()
        composeRule.onNodeWithText("Tomate maduro").assertIsDisplayed()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(localized(R.string.pantry_quantity_value, 3))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(localized(R.string.pantry_quantity_value, 3)).assertIsDisplayed()
        composeRule.onNodeWithText("Tomate fresco").assertDoesNotExist()

        composeRule.activityRule.scenario.recreate()
        composeRule.onNodeWithText("Tomate maduro").assertIsDisplayed()
        composeRule.onNodeWithText(localized(R.string.pantry_quantity_value, 3)).assertIsDisplayed()
    }

    @Test
    fun normalizedDuplicateShowsInlineWarningWithoutAddingRow() {
        openPantry()
        addItem("Tomate fresco")
        composeRule.onNodeWithTag(PantryTestTags.ADD_ITEM).performClick()
        composeRule.onNodeWithTag(PantryTestTags.NAME_FIELD).performTextInput("  TOMATE  fresco ")
        composeRule.onNodeWithTag(PantryTestTags.SAVE_ITEM).performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(localized(R.string.pantry_duplicate_error))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(localized(R.string.pantry_duplicate_error)).assertIsDisplayed()
        composeRule.onAllNodesWithText("Tomate fresco").assertCountEquals(1)
        composeRule.onNodeWithTag(PantryTestTags.EDIT_DIALOG).assertIsDisplayed()
    }

    @Test
    fun stepperEnforcesIntegerBoundsAndZeroSavesWithoutQuantity() {
        openPantry()
        // Nuevo ítem: arranque en 0 con «−» deshabilitado y sin campo de texto de
        // cantidad (solo el nombre tiene SetText; el buscador de la lista no
        // existe todavía porque la Despensa está vacía).
        composeRule.onNodeWithTag(PantryTestTags.ADD_ITEM).performClick()
        composeRule.onNodeWithTag(PantryTestTags.SAVE_ITEM).assertIsNotEnabled()
        composeRule.onNodeWithTag(PantryTestTags.QUANTITY_STEPPER).assertIsDisplayed()
        composeRule.onNodeWithTag(PantryTestTags.QUANTITY_VALUE).assertTextEquals("0")
        composeRule.onNodeWithTag(PantryTestTags.QUANTITY_DECREMENT).assertIsNotEnabled()
        composeRule.onAllNodes(hasSetTextAction()).assertCountEquals(1)
        composeRule.onNodeWithTag(PantryTestTags.CANCEL_EDIT).performClick()

        // Tope superior sin recorrer todo el rango: se parte de 98 (sembrado en
        // Room) y se sube un solo paso hasta el máximo, vía el menú ⋮ (D4).
        runBlocking {
            GlobalContext.get().get<PantryRepository>()
                .savePantryItem(PantryItem(id = "harina-98", name = "Harina", quantity = 98))
        }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(PantryTestTags.ITEM_CARD).fetchSemanticsNodes().size == 1
        }
        composeRule.onNodeWithTag(PantryTestTags.itemMenu("harina-98")).performClick()
        composeRule.onNodeWithTag(PantryTestTags.editItem("harina-98")).performClick()
        composeRule.onNodeWithTag(PantryTestTags.QUANTITY_VALUE).assertTextEquals("98")
        composeRule.onNodeWithTag(PantryTestTags.QUANTITY_INCREMENT).performClick()
        composeRule.onNodeWithTag(PantryTestTags.QUANTITY_VALUE).assertTextEquals("99")
        composeRule.onNodeWithTag(PantryTestTags.QUANTITY_INCREMENT).assertIsNotEnabled()
        composeRule.onNodeWithTag(PantryTestTags.CANCEL_EDIT).performClick()

        // Guardar 0 no deja línea de cantidad en la fila (la sembrada sí la tiene).
        composeRule.onNodeWithTag(PantryTestTags.ADD_ITEM).performClick()
        composeRule.onNodeWithTag(PantryTestTags.NAME_FIELD).performTextInput("Azúcar")
        composeRule.onNodeWithTag(PantryTestTags.SAVE_ITEM).performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Azúcar").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onAllNodesWithText(localized(R.string.pantry_quantity_value, 0)).assertCountEquals(0)
        composeRule.onNodeWithText(localized(R.string.pantry_no_quantity)).assertIsDisplayed()
        composeRule.onAllNodesWithTag(PantryTestTags.ITEM_CARD).assertCountEquals(2)
    }

    @Test
    fun deleteCanBeCancelledOrConfirmed() {
        openPantry()
        addItem("Tomate fresco")
        openItemMenu("Tomate fresco")
        composeRule.onNodeWithContentDescription("${localized(R.string.pantry_delete_accessibility)} Tomate fresco")
            .performClick()
        composeRule.onNodeWithTag(PantryTestTags.DELETE_DIALOG).assertIsDisplayed()
        composeRule.onNodeWithText(localized(R.string.pantry_delete_message, "Tomate fresco")).assertIsDisplayed()
        composeRule.onNodeWithTag(PantryTestTags.DELETE_CANCEL).performClick()
        composeRule.onNodeWithText("Tomate fresco").assertIsDisplayed()

        openItemMenu("Tomate fresco")
        composeRule.onNodeWithContentDescription("${localized(R.string.pantry_delete_accessibility)} Tomate fresco")
            .performClick()
        composeRule.onNodeWithTag(PantryTestTags.DELETE_CONFIRM).performClick()
        composeRule.onNodeWithTag(PantryTestTags.EMPTY_TITLE).assertIsDisplayed()
        composeRule.onNodeWithText("Tomate fresco").assertDoesNotExist()
    }

    // S13 (verify 4, D16; enmienda de la D3 de `mover-escaneo-a-la-despensa`): el
    // estado vacío muestra «Añadir Alimento» (primario) primero y «Escanear la
    // despensa» (tonal) debajo, junto a los dos FABs.
    @Test
    fun emptyStateShowsAddAndScanCtasInOrder() {
        openPantry()
        composeRule.onNodeWithTag(PantryTestTags.EMPTY_ADD_CTA).assertIsDisplayed()
        composeRule.onNodeWithTag(PantryTestTags.SCAN_FOOD_CTA).assertIsDisplayed()
        composeRule.onNodeWithTag(PantryTestTags.SCAN_FOOD).assertIsDisplayed()
        composeRule.onNodeWithTag(PantryTestTags.ADD_ITEM).assertIsDisplayed()
        composeRule.onNodeWithText(localized(R.string.pantry_empty_add_cta)).assertIsDisplayed()
        composeRule.onNodeWithText(localized(R.string.pantry_empty_scan_cta)).assertIsDisplayed()

        val addCtaTop = composeRule.onNodeWithTag(PantryTestTags.EMPTY_ADD_CTA).getUnclippedBoundsInRoot().top
        val scanCtaTop = composeRule.onNodeWithTag(PantryTestTags.SCAN_FOOD_CTA).getUnclippedBoundsInRoot().top
        assertTrue(addCtaTop < scanCtaTop)
    }

    // D1 (verify 2): los dos FABs se apilan con «Escanear alimentos» encima y
    // «Añadir alimento» abajo, junto a la esquina.
    @Test
    fun scanFoodFabIsStackedAboveTheAddFoodFab() {
        openPantry()
        val scanTop = composeRule.onNodeWithTag(PantryTestTags.SCAN_FOOD).getUnclippedBoundsInRoot().top
        val addTop = composeRule.onNodeWithTag(PantryTestTags.ADD_ITEM).getUnclippedBoundsInRoot().top
        assertTrue(scanTop < addTop)
    }

    // S2 (verify 2): el FAB «Escanear alimentos» de la Despensa abre el Escáner.
    @Test
    fun scanFoodFabOpensTheScannerFromThePantry() {
        openPantry()
        composeRule.onNodeWithTag(PantryTestTags.SCAN_FOOD).performClick()
        composeRule.onNodeWithTag(ScannerTestTags.SCREEN).assertIsDisplayed()
    }

    // S2 (verify 2): guardar en el Escáner vuelve a la Despensa con los Alimentos
    // y no deja la Despensa duplicada en el back stack (D5).
    @Test
    fun scannerSaveReturnsToThePantryWithTheSavedFoods() {
        openPantry()
        composeRule.onNodeWithTag(PantryTestTags.SCAN_FOOD).performClick()
        composeRule.onNodeWithTag(ScannerTestTags.SCREEN).assertIsDisplayed()

        composeRule.onNodeWithTag(ScannerTestTags.MANUAL_NAME).performTextInput("Tomate fresco")
        composeRule.onNodeWithTag(ScannerTestTags.MANUAL_ADD).performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(ScannerTestTags.FOOD_ITEM).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(hasTestTag(ScannerTestTags.SAVE).and(isEnabled()))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(ScannerTestTags.SAVE).performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(PantryTestTags.ITEM_CARD).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(PantryTestTags.SCREEN).assertIsDisplayed()
        composeRule.onNodeWithText("Tomate fresco").assertIsDisplayed()

        // RF-MOB4/D1: la Despensa ya no muestra «Volver» (es pestaña, `e8c5730`);
        // el back del sistema vuelve a Mi recetario sin dejar la Despensa
        // duplicada en el back stack. Paso reparado en `crear-mi-perfil` (2026-11-09)
        // por ser un resto del merge 9dacc87: la afirmación esperaba un botón
        // retirado y fallaba en cualquier entorno.
        Espresso.pressBack()
        composeRule.onNodeWithTag(MyRecipesTestTags.SCREEN).assertIsDisplayed()
        openPantry()
        assertEquals(1, composeRule.onAllNodesWithTag(PantryTestTags.SCREEN).fetchSemanticsNodes().size)
        composeRule.onNodeWithText("Tomate fresco").assertIsDisplayed()
    }

    // S10 (verify 1): cabecera, tarjeta de resumen, consejo y contadores.
    @Test
    fun headerSummaryAndTipShowCountsAndCoachingAdvice() {
        openPantry()
        seedPantry(
            PantryItem("aguacate", "Aguacate", quantity = 2, expiryDate = LocalDate.now().plusDays(1)),
            PantryItem("harina", "Harina", quantity = 5, expiryDate = LocalDate.now().plusDays(10)),
            PantryItem("arroz", "Arroz", quantity = null, expiryDate = null),
        )

        // Cabecera: subtítulo y badge «N frescos» = total de Alimentos (D9).
        composeRule.onNodeWithTag(PantryTestTags.SECTION_SUBTITLE).assertIsDisplayed()
        composeRule.onNodeWithTag(PantryTestTags.FRESH_BADGE)
            .assertTextEquals(plural(R.plurals.pantry_fresh_count, 3))

        // Resumen: recuento, capacidad (3/40 → 8 %, D10) y «N por caducar» (D9).
        composeRule.onNodeWithTag(PantryTestTags.SUMMARY_COUNT)
            .assertTextEquals(plural(R.plurals.pantry_food_count, 3))
        composeRule.onNodeWithTag(PantryTestTags.SUMMARY_CAPACITY)
            .assertTextEquals(localized(R.string.pantry_summary_capacity, 8))
        composeRule.onNodeWithTag(PantryTestTags.SUMMARY_EXPIRING)
            .assertTextEquals(plural(R.plurals.pantry_expiring_count, 1))

        // Consejo determinista con el nombre del Alimento «Consumir pronto» (D11).
        composeRule.onNodeWithTag(PantryTestTags.TIP_BAND).assertIsDisplayed()
        composeRule.onNodeWithTag(PantryTestTags.TIP_MESSAGE)
            .assertTextEquals(localized(R.string.pantry_tip_one, "Aguacate"))

        // Píldoras y textos de caducidad por tarjeta (D8/D13).
        composeRule.onNodeWithTag(PantryTestTags.itemFreshness("aguacate"))
            .assertTextEquals(localized(R.string.pantry_freshness_expiring))
        composeRule.onNodeWithTag(PantryTestTags.itemFreshness("harina"))
            .assertTextEquals(localized(R.string.pantry_freshness_good))
        composeRule.onNodeWithTag(PantryTestTags.itemFreshness("arroz"))
            .assertTextEquals(localized(R.string.pantry_freshness_good))
        composeRule.onNodeWithTag(PantryTestTags.itemExpiry("aguacate"))
            .assertTextEquals(localized(R.string.pantry_expiry_tomorrow))
        composeRule.onNodeWithTag(PantryTestTags.itemExpiry("harina"))
            .assertTextEquals(plural(R.plurals.pantry_expiry_days, 10, 10))
        composeRule.onNodeWithTag(PantryTestTags.itemExpiry("arroz"))
            .assertTextEquals(localized(R.string.pantry_expiry_long))
        composeRule.onNodeWithTag(PantryTestTags.itemQuantity("arroz"))
            .assertTextEquals(localized(R.string.pantry_no_quantity))
    }

    // S11 (verify 2): el stepper inline de la tarjeta guarda al instante y
    // respeta los límites 0..99 (D15), persistiendo tras recrear la actividad.
    @Test
    fun inlineStepperSavesImmediatelyAndEnforcesBounds() {
        openPantry()
        seedPantry(
            PantryItem("pimiento", "Pimiento", quantity = 5),
            PantryItem("harina", "Harina", quantity = 98),
            PantryItem("azucar", "Azúcar", quantity = null),
        )

        // «+» dos veces → 7, guardado al momento.
        composeRule.onNodeWithTag(PantryTestTags.itemQuantityIncrease("pimiento")).performClick()
        composeRule.onNodeWithTag(PantryTestTags.itemQuantityIncrease("pimiento")).performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(localized(R.string.pantry_quantity_value, 7))
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Límite superior: 98 → 99 con «+» deshabilitado.
        composeRule.onNodeWithTag(PantryTestTags.itemQuantityIncrease("harina")).performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(localized(R.string.pantry_quantity_value, 99))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(PantryTestTags.itemQuantityIncrease("harina")).assertIsNotEnabled()

        // Límite inferior: sin cantidad (0) → «−» deshabilitado.
        composeRule.onNodeWithTag(PantryTestTags.itemQuantityDecrease("azucar")).assertIsNotEnabled()
        composeRule.onNodeWithTag(PantryTestTags.itemQuantityDecrease("harina")).performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(localized(R.string.pantry_quantity_value, 98))
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Las cantidades persisten tras recrear la actividad (D15).
        composeRule.activityRule.scenario.recreate()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(localized(R.string.pantry_quantity_value, 7))
                .fetchSemanticsNodes().isNotEmpty() &&
                composeRule.onAllNodesWithText(localized(R.string.pantry_quantity_value, 98))
                    .fetchSemanticsNodes().isNotEmpty()
        }
    }

    // S12 (verify 3): filtrado en vivo por nombre y estado «sin resultados» con
    // su acción de alta; al limpiar la búsqueda vuelve la lista completa (D18).
    @Test
    fun searchFiltersLiveAndOffersRegistrationWhenNothingMatches() {
        openPantry()
        seedPantry(
            PantryItem("tomate", "Tomate fresco", quantity = 2),
            PantryItem("harina", "Harina", quantity = 1),
        )

        composeRule.onNodeWithTag(PantryTestTags.SEARCH_FIELD).performTextInput("toma")
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(PantryTestTags.ITEM_CARD).fetchSemanticsNodes().size == 1
        }
        composeRule.onNodeWithText("Tomate fresco").assertIsDisplayed()
        composeRule.onNodeWithText("Harina").assertDoesNotExist()

        composeRule.onNodeWithTag(PantryTestTags.SEARCH_FIELD).performTextClearance()
        composeRule.onNodeWithTag(PantryTestTags.SEARCH_FIELD).performTextInput("zzz")
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(PantryTestTags.NO_RESULTS).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(PantryTestTags.NO_RESULTS_TITLE).assertIsDisplayed()
        composeRule.onNodeWithTag(PantryTestTags.NO_RESULTS_ADD).performClick()
        composeRule.onNodeWithTag(PantryTestTags.EDIT_DIALOG).assertIsDisplayed()
        composeRule.onNodeWithTag(PantryTestTags.CANCEL_EDIT).performClick()

        composeRule.onNodeWithTag(PantryTestTags.SEARCH_CLEAR).performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(PantryTestTags.ITEM_CARD).fetchSemanticsNodes().size == 2
        }
        composeRule.onNodeWithText("Harina").assertIsDisplayed()
    }

    // S13 (verify 5): alta con fecha de caducidad, persistencia tras recrear la
    // app y edición que quita la fecha (S5: vuelve a «En buen estado»/«Larga
    // duración»).
    @Test
    fun expiryDateIsSavedEditedAndPersistsAcrossRecreation() {
        openPantry()
        composeRule.onNodeWithTag(PantryTestTags.ADD_ITEM).performClick()
        composeRule.onNodeWithTag(PantryTestTags.NAME_FIELD).performTextInput("Aguacates")
        // El DatePicker nace con hoy seleccionado; confirmar fija la fecha (D2).
        composeRule.onNodeWithTag(PantryTestTags.EXPIRY_VALUE).performClick()
        composeRule.onNodeWithTag(PantryTestTags.EXPIRY_CONFIRM).performClick()
        composeRule.onNodeWithTag(PantryTestTags.SAVE_ITEM).performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Aguacates").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onAllNodesWithText(localized(R.string.pantry_freshness_expiring)).assertCountEquals(1)
        composeRule.onNodeWithText(localized(R.string.pantry_expiry_today)).assertIsDisplayed()

        // La fecha sobrevive al reiniciar la actividad (verify 5).
        composeRule.activityRule.scenario.recreate()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(localized(R.string.pantry_expiry_today))
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Editar y quitar la fecha → «En buen estado» y «Larga duración» (S5).
        openItemMenu("Aguacates")
        composeRule.onNodeWithContentDescription("${localized(R.string.pantry_edit_accessibility)} Aguacates")
            .performClick()
        composeRule.onNodeWithTag(PantryTestTags.EXPIRY_CLEAR).performClick()
        composeRule.onNodeWithTag(PantryTestTags.SAVE_ITEM).performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(localized(R.string.pantry_expiry_long))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onAllNodesWithText(localized(R.string.pantry_freshness_expiring)).assertCountEquals(0)
        composeRule.onAllNodesWithText(localized(R.string.pantry_freshness_good)).assertCountEquals(1)
    }

    // S14 (verify 6): los dos FABs se conservan tras el rediseño y siguen
    // operativos (abren el diálogo de alta y el Escáner).
    @Test
    fun bothFabsRemainStackedAndOperationalAfterRedesign() {
        openPantry()
        composeRule.onNodeWithTag(PantryTestTags.SCAN_FOOD).assertIsDisplayed()
        composeRule.onNodeWithTag(PantryTestTags.ADD_ITEM).assertIsDisplayed()
        val scanTop = composeRule.onNodeWithTag(PantryTestTags.SCAN_FOOD).getUnclippedBoundsInRoot().top
        val addTop = composeRule.onNodeWithTag(PantryTestTags.ADD_ITEM).getUnclippedBoundsInRoot().top
        assertTrue(scanTop < addTop)

        composeRule.onNodeWithTag(PantryTestTags.ADD_ITEM).performClick()
        composeRule.onNodeWithTag(PantryTestTags.EDIT_DIALOG).assertIsDisplayed()
        composeRule.onNodeWithTag(PantryTestTags.CANCEL_EDIT).performClick()

        composeRule.onNodeWithTag(PantryTestTags.SCAN_FOOD).performClick()
        composeRule.onNodeWithTag(ScannerTestTags.SCREEN).assertIsDisplayed()
    }

    private fun openPantry() {
        composeRule.onNodeWithTag(AppBottomBarTestTags.TAB_PANTRY).performClick()
        composeRule.onNodeWithTag(PantryTestTags.SCREEN).assertIsDisplayed()
    }

    // El idioma efectivo de la app es el de AppLocaleController (Ajustes), no el
    // del dispositivo: los textos esperados se resuelven en ese idioma para no
    // depender del locale del AVD.
    private fun localized(resourceId: Int, vararg arguments: Any): String {
        val baseContext = InstrumentationRegistry.getInstrumentation().targetContext
        val language = GlobalContext.get().get<AppLocaleController>().language.value
        val context = baseContext.createConfigurationContext(
            Configuration(baseContext.resources.configuration).apply { setLocale(language.locale) },
        )
        return context.getString(resourceId, *arguments)
    }

    // Igual que `localized` pero para `<plurals>` (contadores de cabecera/resumen).
    private fun plural(resourceId: Int, quantity: Int, vararg arguments: Any): String {
        val baseContext = InstrumentationRegistry.getInstrumentation().targetContext
        val language = GlobalContext.get().get<AppLocaleController>().language.value
        val context = baseContext.createConfigurationContext(
            Configuration(baseContext.resources.configuration).apply { setLocale(language.locale) },
        )
        return if (arguments.isEmpty()) {
            context.resources.getQuantityString(resourceId, quantity, quantity)
        } else {
            context.resources.getQuantityString(resourceId, quantity, *arguments)
        }
    }

    private fun addItem(name: String) {
        composeRule.onNodeWithTag(PantryTestTags.ADD_ITEM).performClick()
        composeRule.onNodeWithTag(PantryTestTags.NAME_FIELD).performTextInput(name)
        composeRule.onNodeWithTag(PantryTestTags.SAVE_ITEM).performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(PantryTestTags.ITEM_CARD).fetchSemanticsNodes().isNotEmpty()
        }
    }

    // Abre el menú ⋮ de la tarjeta cuyo nombre visible es `itemName`.
    private fun openItemMenu(itemName: String) {
        composeRule.onNodeWithContentDescription(localized(R.string.pantry_item_menu, itemName)).performClick()
    }

    // Siembra Alimentos directamente en Room y espera a que aparezcan sus tarjetas.
    private fun seedPantry(vararg items: PantryItem) {
        runBlocking {
            val repository = GlobalContext.get().get<PantryRepository>()
            items.forEach { repository.savePantryItem(it) }
        }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(PantryTestTags.ITEM_CARD).fetchSemanticsNodes().size == items.size
        }
    }
}
