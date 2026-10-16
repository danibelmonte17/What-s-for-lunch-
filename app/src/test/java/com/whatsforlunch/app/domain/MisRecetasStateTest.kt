package com.whatsforlunch.app.domain

import com.whatsforlunch.app.domain.misrecetas.MisRecetasState
import com.whatsforlunch.app.domain.misrecetas.misRecetasStateOf
import com.whatsforlunch.app.domain.model.Recipe
import org.junit.Assert.assertEquals
import org.junit.Test

// Humo de domain/: la regla de negocio se verifica sin pantalla ni almacenamiento (RF-2).
class MisRecetasStateTest {

    @Test
    fun emptyCatalogProducesEmptyState() {
        assertEquals(MisRecetasState.Empty, misRecetasStateOf(emptyList()))
    }

    @Test
    fun catalogWithRecipesProducesWithRecipesState() {
        val recipes = listOf(Recipe(id = "1", title = "Tortilla de patatas"))
        assertEquals(MisRecetasState.WithRecipes, misRecetasStateOf(recipes))
    }
}
