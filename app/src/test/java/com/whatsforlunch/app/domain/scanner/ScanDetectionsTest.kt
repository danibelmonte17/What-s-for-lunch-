package com.whatsforlunch.app.domain.scanner

import com.whatsforlunch.app.domain.model.PantryItem
import com.whatsforlunch.app.domain.model.ScanDraftItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScanDetectionsTest {
    @Test
    fun renameCollisionKeepsEditedChipNameAndQuantity() {
        val kept = ScanDraftItem("Patata", quantity = 4, id = "kept")
        val edited = ScanDraftItem("Yuca", quantity = 7, id = "edited")

        val merged = ScanDetections.rename(listOf(kept, edited), "edited", "  PATATA  ")

        assertEquals(1, merged.size)
        assertEquals("  PATATA  ", merged.single().name)
        assertEquals(7, merged.single().quantity)
        assertEquals("edited", merged.single().id)
    }

    @Test
    fun manualDuplicateCanRetainExistingChipWithoutCreatingAnother() {
        val existing = ScanDraftItem("Manzana", quantity = 3, id = "existing")
        val manual = ScanDraftItem(" MANZANA ", quantity = 0, id = "manual")

        val merged = ScanDetections.mergeDraftItems(listOf(existing, manual))

        assertEquals(1, merged.size)
        assertEquals(existing, merged.single())
    }

    @Test
    fun updateQuantityClampsToIntegerBounds() {
        val item = ScanDraftItem("Pera", id = "pear")
        assertEquals(0, ScanDetections.updateQuantity(listOf(item), "pear", -1).single().quantity)
        assertEquals(99, ScanDetections.updateQuantity(listOf(item), "pear", 100).single().quantity)
    }

    @Test
    fun conversionTreatsZeroAsNoQuantityAndKeepsMaximumQuantity() {
        assertNull(ScanDetections.toPantryItem(ScanDraftItem("Naranja", id = "orange")).quantity)
        assertEquals(99, ScanDetections.toPantryItem(ScanDraftItem("Naranja", 99, "orange")).quantity)
    }

    @Test
    fun pantryMatchesUseNormalizedNames() {
        val chip = ScanDraftItem("  TOMATE  fresco ", id = "scan")
        val pantry = PantryItem("pantry", "Tomate fresco", 2)

        assertEquals(pantry, ScanDetections.matchesWithPantry(listOf(chip), listOf(pantry))["scan"])
        assertTrue(ScanDetections.matchesWithPantry(listOf(chip), emptyList()).isEmpty())
    }

    @Test
    fun sumIntoAddsChipQuantityToExistingQuantity() {
        val existing = PantryItem("tomato", "Tomate", 1)
        val summed = ScanDetections.sumInto(existing, ScanDraftItem("Tomate", 2, "draft"))

        assertEquals(3, summed.quantity)
        assertEquals("tomato", summed.id)
        assertEquals("Tomate", summed.name)
    }

    @Test
    fun sumIntoTreatsNoQuantityAsAdditiveNeutral() {
        val withoutQuantity = PantryItem("tomato", "Tomate", null)

        assertEquals(
            2,
            ScanDetections.sumInto(withoutQuantity, ScanDraftItem("Tomate", 2, "draft")).quantity,
        )
        assertEquals(
            3,
            ScanDetections.sumInto(PantryItem("tomato", "Tomate", 3), ScanDraftItem("Tomate", 0, "draft")).quantity,
        )
        assertNull(
            ScanDetections.sumInto(withoutQuantity, ScanDraftItem("Tomate", 0, "draft")).quantity,
        )
    }

    @Test
    fun sumIntoCapsTotalAtMaximumWithoutWarning() {
        assertEquals(
            99,
            ScanDetections.sumInto(PantryItem("tomato", "Tomate", 60), ScanDraftItem("Tomate", 50, "draft")).quantity,
        )
        assertEquals(
            99,
            ScanDetections.sumInto(PantryItem("tomato", "Tomate", 99), ScanDraftItem("Tomate", 1, "draft")).quantity,
        )
    }
}
