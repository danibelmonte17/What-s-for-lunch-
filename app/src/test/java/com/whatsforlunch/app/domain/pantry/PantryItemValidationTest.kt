package com.whatsforlunch.app.domain.pantry

import com.whatsforlunch.app.domain.text.normalizeName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PantryItemValidationTest {
    @Test
    fun nameMustContainNonWhitespaceCharacters() {
        assertTrue(PantryItemValidation.isNameValid("Tomate"))
        assertFalse(PantryItemValidation.isNameValid("  \t "))
    }

    @Test
    fun quantityAcceptsIntegerBoundsAndZeroRepresentsNoQuantity() {
        assertEquals(0, PantryItemValidation.MIN_QUANTITY)
        assertEquals(999, PantryItemValidation.MAX_QUANTITY)
        assertTrue(PantryItemValidation.isQuantityValid(PantryItemValidation.MIN_QUANTITY))
        assertTrue(PantryItemValidation.isQuantityValid(PantryItemValidation.MAX_QUANTITY))
        assertTrue(PantryItemValidation.isQuantityValid(3))
    }

    @Test
    fun quantityOutsideIntegerBoundsIsRejected() {
        assertFalse(PantryItemValidation.isQuantityValid(-1))
        assertFalse(PantryItemValidation.isQuantityValid(1000))
    }

    @Test
    fun nameNormalizationTrimsCaseAndDiacritics() {
        assertEquals("tomate fresco", normalizeName("  TOMÁTE  fresco "))
    }
}
