package com.whatsforlunch.app.domain.scanner

import com.whatsforlunch.app.domain.model.DetectedFood
import com.whatsforlunch.app.domain.model.PantryItem
import com.whatsforlunch.app.domain.model.ScanDraftItem
import com.whatsforlunch.app.domain.repository.PantryDuplicateResolution
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScanDetectionsTest {
    @Test
    fun filtersBelowChosenThresholdAndBlankNames() {
        val detections = listOf(
            DetectedFood("Plátano", 0.599),
            DetectedFood("Banana", 0.60),
            DetectedFood("  ", 0.99),
        )

        assertEquals(listOf("Banana"), ScanDetections.eligible(detections).map { it.name })
    }

    @Test
    fun filtersInvalidConfidenceValues() {
        assertTrue(ScanDetections.eligible(listOf(DetectedFood("Banana", -0.1))).isEmpty())
        assertTrue(ScanDetections.eligible(listOf(DetectedFood("Banana", 1.01))).isEmpty())
        assertTrue(ScanDetections.eligible(listOf(DetectedFood("Banana", Double.NaN))).isEmpty())
    }

    @Test
    fun detectionDuplicatesUseReadableNameAndHighestConfidence() {
        val items = ScanDetections.toDraftItems(
            listOf(
                DetectedFood("TOMATE FRESCO", 0.65),
                DetectedFood("  Tomate  fresco ", 0.81),
            ),
        )

        assertEquals(1, items.size)
        assertEquals("Tomate fresco", items.single().name)
        assertEquals(0.81, items.single().confidence!!, 0.0001)
        assertEquals(0, items.single().quantity)
    }

    @Test
    fun renameCollisionKeepsEditedChipNameAndQuantityButBestConfidence() {
        val kept = ScanDraftItem("Patata", 0.75, quantity = 4, id = "kept")
        val edited = ScanDraftItem("Yuca", null, quantity = 7, id = "edited")

        val merged = ScanDetections.rename(listOf(kept, edited), "edited", "  PATATA  ")

        assertEquals(1, merged.size)
        assertEquals("  PATATA  ", merged.single().name)
        assertEquals(7, merged.single().quantity)
        assertEquals(0.75, merged.single().confidence!!, 0.0001)
        assertEquals("edited", merged.single().id)
    }

    @Test
    fun manualDuplicateCanRetainExistingChipWithoutCreatingAnother() {
        val existing = ScanDraftItem("Manzana", 0.72, quantity = 3, id = "existing")
        val manual = ScanDraftItem(" MANZANA ", null, quantity = 0, id = "manual")

        val merged = ScanDetections.mergeDraftItems(listOf(existing, manual))

        assertEquals(1, merged.size)
        assertEquals(existing, merged.single())
    }

    @Test
    fun updateQuantityClampsToIntegerBounds() {
        val item = ScanDraftItem("Pera", null, id = "pear")
        assertEquals(0, ScanDetections.updateQuantity(listOf(item), "pear", -1).single().quantity)
        assertEquals(99, ScanDetections.updateQuantity(listOf(item), "pear", 100).single().quantity)
    }

    @Test
    fun conversionTreatsZeroAsNoQuantityAndKeepsMaximumQuantity() {
        assertNull(ScanDetections.toPantryItem(ScanDraftItem("Naranja", null, id = "orange")).quantity)
        assertEquals(99, ScanDetections.toPantryItem(ScanDraftItem("Naranja", 0.9, 99, "orange")).quantity)
    }

    @Test
    fun pantryMatchesUseNormalizedNames() {
        val chip = ScanDraftItem("  TOMATE  fresco ", 0.8, id = "scan")
        val pantry = PantryItem("pantry", "Tomate fresco", 2)

        assertEquals(pantry, ScanDetections.matchesWithPantry(listOf(chip), listOf(pantry))["scan"])
        assertTrue(ScanDetections.matchesWithPantry(listOf(chip), emptyList()).isEmpty())
    }

    @Test
    fun duplicateResolutionPreservesOrUpdatesExistingIdAndQuantity() {
        val existing = PantryItem("same-id", "Tomate fresco", 1)
        val update = ScanDraftItem("Tomate", null, 0, "draft")

        assertNull(ScanDetections.resolveDuplicate(existing, update, PantryDuplicateResolution.KeepExisting))
        val updated = ScanDetections.resolveDuplicate(existing, update, PantryDuplicateResolution.UpdateQuantity)!!
        assertEquals("same-id", updated.id)
        assertNull(updated.quantity)
        assertFalse(updated == existing)
    }
}
