package com.whatsforlunch.app

import com.whatsforlunch.app.domain.model.IngredientUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// Vocabulario cerrado de unidades (unificar-unidades, D1/D2): parseo del enum,
// caída a «ud» para lo vacío y lo desconocido y conservación de lo legado.
class IngredientUnitTest {

    @Test
    fun exposesExactlyTheFiveClosedCodesInOrder() {
        assertEquals(listOf("ud", "g", "kg", "ml", "l"), IngredientUnit.entries.map { it.code })
    }

    @Test
    fun fromCodeParsesEveryEnumCodeIgnoringCaseAndSurroundingSpaces() {
        assertEquals(IngredientUnit.UD, IngredientUnit.fromCode("ud"))
        assertEquals(IngredientUnit.G, IngredientUnit.fromCode("g"))
        assertEquals(IngredientUnit.KG, IngredientUnit.fromCode("kg"))
        assertEquals(IngredientUnit.ML, IngredientUnit.fromCode("ml"))
        assertEquals(IngredientUnit.L, IngredientUnit.fromCode("l"))
        assertEquals(IngredientUnit.KG, IngredientUnit.fromCode("  KG "))
        assertEquals(IngredientUnit.G, IngredientUnit.fromCode("G"))
        assertEquals(IngredientUnit.ML, IngredientUnit.fromCode("Ml"))
    }

    @Test
    fun fromCodeRejectsEmptyAndLegacyUnits() {
        assertNull(IngredientUnit.fromCode(null))
        assertNull(IngredientUnit.fromCode(""))
        assertNull(IngredientUnit.fromCode("   "))
        assertNull(IngredientUnit.fromCode("cucharadas"))
        assertNull(IngredientUnit.fromCode("gramos"))
        assertNull(IngredientUnit.fromCode("bote"))
    }

    @Test
    fun normalizeFallsBackToUdForUnknownOrEmptyUnits() {
        assertEquals("ud", IngredientUnit.normalize(""))
        assertEquals("ud", IngredientUnit.normalize("   "))
        assertEquals("ud", IngredientUnit.normalize(null))
        assertEquals("ud", IngredientUnit.normalize("gramos"))
        assertEquals("ud", IngredientUnit.normalize("lata grande"))
    }

    @Test
    fun normalizeKeepsKnownCodesAndTrimsThem() {
        assertEquals("g", IngredientUnit.normalize("g"))
        assertEquals("g", IngredientUnit.normalize("  G "))
        assertEquals("kg", IngredientUnit.normalize("kg"))
        assertEquals("ml", IngredientUnit.normalize("ML"))
        assertEquals("l", IngredientUnit.normalize("l"))
        assertEquals("ud", IngredientUnit.normalize("ud"))
    }

    @Test
    fun normalizeIsIdempotent() {
        val once = IngredientUnit.normalize("gramos")

        assertEquals(once, IngredientUnit.normalize(once))
        assertEquals("g", IngredientUnit.normalize(IngredientUnit.normalize("g")))
    }
}
