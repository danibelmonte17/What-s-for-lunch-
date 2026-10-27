package com.whatsforlunch.app

import com.whatsforlunch.app.domain.model.Aisle
import com.whatsforlunch.app.domain.model.ShoppingItemSource
import com.whatsforlunch.app.domain.model.ShoppingListItem
import com.whatsforlunch.app.domain.shoppinglist.ShoppingListRules
import com.whatsforlunch.app.ui.shoppinglist.formatEditableQuantity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

// RD19: validación y upsert manual de un ÍtemLista (D2-D7): cantidad decimal
// opcional, Pasillo opcional, sin renombrar al editar y sin duplicar nombres.
class ShoppingListManualItemTest {

    @Test
    fun nameMustNotBeBlankAfterNormalizing() {
        assertTrue(ShoppingListRules.isManualNameValid("Pan"))
        assertTrue(ShoppingListRules.isManualNameValid("  harina  "))
        assertFalse(ShoppingListRules.isManualNameValid(""))
        assertFalse(ShoppingListRules.isManualNameValid("   "))
    }

    @Test
    fun quantityIsOptionalAndMustBeFiniteGreaterThanZero() {
        assertTrue(ShoppingListRules.isManualQuantityValid(null))
        assertTrue(ShoppingListRules.isManualQuantityValid(0.5))
        assertTrue(ShoppingListRules.isManualQuantityValid(1200.0))
        assertFalse(ShoppingListRules.isManualQuantityValid(0.0))
        assertFalse(ShoppingListRules.isManualQuantityValid(-1.0))
        assertFalse(ShoppingListRules.isManualQuantityValid(Double.NaN))
    }

    @Test
    fun quantityAcceptsCommaOrDotAndBlankMeansNoQuantity() {
        assertEquals(0.5, ShoppingListRules.parseManualQuantity("0,5")!!, 0.0001)
        assertEquals(0.5, ShoppingListRules.parseManualQuantity("0.5")!!, 0.0001)
        assertEquals(2.0, ShoppingListRules.parseManualQuantity(" 2 ")!!, 0.0001)
        assertNull(ShoppingListRules.parseManualQuantity(""))
        assertNull(ShoppingListRules.parseManualQuantity("   "))
        assertNull(ShoppingListRules.parseManualQuantity("dos"))
    }

    @Test
    fun manualSaveCreatesANewItemWithoutSourcesAndWithItsAisle() {
        val result = ShoppingListRules.saveManualItem(
            existing = emptyList(),
            id = null,
            name = "  Pan ",
            quantity = 2.0,
            unit = " uds ",
            aisle = Aisle.PANTRY_OILS,
        )

        val item = result.single()
        assertEquals(ShoppingListRules.itemIdFor("Pan"), item.id)
        assertEquals("Pan", item.name)
        assertEquals(2.0, item.quantity!!, 0.0001)
        assertEquals("uds", item.unit)
        assertEquals(Aisle.PANTRY_OILS, item.aisle)
        assertFalse(item.completed)
        assertTrue(item.sources.isEmpty())
    }

    @Test
    fun manualSaveOnAnExistingNameEditsItKeepingNameAndSources() {
        val existing = listOf(
            ShoppingListItem(
                id = ShoppingListRules.itemIdFor("Harina"),
                name = "Harina",
                quantity = 200.0,
                unit = "g",
                sources = listOf(ShoppingItemSource(sourceKey = "tortilla", sourceTitle = "Tortilla")),
            ),
        )

        val result = ShoppingListRules.saveManualItem(
            existing = existing,
            id = null,
            name = "harina",
            quantity = 500.0,
            unit = "g",
            aisle = Aisle.PANTRY_OILS,
        )

        val item = result.single()
        assertEquals(ShoppingListRules.itemIdFor("Harina"), item.id)
        assertEquals("Harina", item.name)
        assertEquals(500.0, item.quantity!!, 0.0001)
        assertEquals(Aisle.PANTRY_OILS, item.aisle)
        assertEquals(listOf("tortilla"), item.sources.map { it.sourceKey })
    }

    @Test
    fun manualSaveByIdReplacesOnlyQuantityUnitAndAisle() {
        val existing = listOf(
            ShoppingListItem(id = "item-1", name = "Azafrán", quantity = 1.0, unit = "g"),
        )

        val result = ShoppingListRules.saveManualItem(
            existing = existing,
            id = "item-1",
            name = "Azafrán",
            quantity = null,
            unit = "  ",
            aisle = Aisle.DAIRY,
        )

        val item = result.single()
        assertEquals("Azafrán", item.name)
        assertNull(item.quantity)
        assertEquals("", item.unit)
        assertEquals(Aisle.DAIRY, item.aisle)
    }

    @Test
    fun invalidDataLeavesTheListUnchanged() {
        val existing = listOf(ShoppingListItem(id = "1", name = "Pan"))

        assertEquals(
            existing,
            ShoppingListRules.saveManualItem(existing, null, "  ", 2.0, "", null),
        )
        assertEquals(
            existing,
            ShoppingListRules.saveManualItem(existing, null, "Pan", 0.0, "", null),
        )
        assertEquals(
            existing,
            ShoppingListRules.saveManualItem(existing, null, "Pan", -3.0, "", null),
        )
    }

    // Repair H1 (validación sesión 88): el prellenado del diálogo debe hacer el
    // ida-y-vuelta con parseManualQuantity sin corromper cantidades ≥ 1000 ni
    // perder precisión más allá de 3 decimales (1200 → «1.200» → 1.2 y 1234.5
    // sin parsear con el formato de lectura de las filas).
    @Test
    fun editableQuantityRoundTripsThroughParsingInEsAndEn() {
        val quantities = listOf(1200.0, 1234.5, 0.1234)
        for (locale in listOf(Locale("es", "ES"), Locale.ENGLISH)) {
            for (quantity in quantities) {
                val text = formatEditableQuantity(quantity, locale)
                assertEquals(
                    "ida-y-vuelta de $quantity con prellenado \"$text\" en $locale",
                    quantity,
                    ShoppingListRules.parseManualQuantity(text)!!,
                    0.0,
                )
            }
        }
    }

    @Test
    fun editableQuantityHasNoThousandsGroupingAndKeepsAllDecimals() {
        assertEquals("1200", formatEditableQuantity(1200.0, Locale("es", "ES")))
        assertEquals("1234,5", formatEditableQuantity(1234.5, Locale("es", "ES")))
        assertEquals("0,1234", formatEditableQuantity(0.1234, Locale("es", "ES")))
        assertEquals("1234.5", formatEditableQuantity(1234.5, Locale.ENGLISH))
        assertEquals("0.1234", formatEditableQuantity(0.1234, Locale.ENGLISH))
    }
}
