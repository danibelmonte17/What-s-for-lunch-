package com.whatsforlunch.app.domain.pantry

import com.whatsforlunch.app.domain.model.PantryItem
import org.junit.Assert.assertEquals
import org.junit.Test

class PantrySearchFilterTest {
    private val items = listOf(
        PantryItem("1", "Tomate fresco"),
        PantryItem("2", "Harina"),
        PantryItem("3", "Torta de limón"),
    )

    // D18: consulta vacía o en blanco → lista completa.
    @Test
    fun emptyOrBlankQueryReturnsEverything() {
        assertEquals(items, filterPantryItems(items, ""))
        assertEquals(items, filterPantryItems(items, "   "))
    }

    // D18: coincidencia «contiene» sobre el nombre normalizado (sin distinción
    // de mayúsculas ni de acentos), mismo criterio que `filterRecipes`.
    @Test
    fun filtersByNormalizedSubstringIgnoringCaseAndAccents() {
        assertEquals(listOf("1"), filterPantryItems(items, "TOMA").map { it.id })
        assertEquals(listOf("3"), filterPantryItems(items, "limon").map { it.id })
        assertEquals(listOf("2"), filterPantryItems(items, "  har ").map { it.id })
        assertEquals(listOf("1", "2", "3"), filterPantryItems(items, "").map { it.id })
    }

    @Test
    fun queryWithNoMatchesReturnsEmptyList() {
        assertEquals(emptyList<PantryItem>(), filterPantryItems(items, "pollo"))
    }
}
