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
    fun quantityIsOptionalAndAcceptsNonNegativeDecimals() {
        assertTrue(PantryItemValidation.isQuantityValid(""))
        assertEquals(0.0, PantryItemValidation.parseQuantity("0"))
        assertEquals(2.5, PantryItemValidation.parseQuantity("2.5"))
        assertEquals(2.5, PantryItemValidation.parseQuantity("2,5"))
        assertTrue(PantryItemValidation.isQuantityValid("0"))
    }

    @Test
    fun negativeNonNumericAndNonFiniteQuantitiesAreRejected() {
        listOf("-1", "abc", "NaN", "Infinity").forEach { value ->
            assertFalse("$value should be invalid", PantryItemValidation.isQuantityValid(value))
        }
    }

    @Test
    fun nameNormalizationTrimsCaseAndDiacritics() {
        assertEquals("tomate fresco", normalizeName("  TOMÁTE  fresco "))
    }
}
