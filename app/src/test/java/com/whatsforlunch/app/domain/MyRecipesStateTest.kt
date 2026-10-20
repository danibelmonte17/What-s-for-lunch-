package com.whatsforlunch.app.domain

import com.whatsforlunch.app.domain.myrecipes.MyRecipesState
import com.whatsforlunch.app.domain.myrecipes.myRecipesStateOf
import com.whatsforlunch.app.domain.model.Recipe
import org.junit.Assert.assertEquals
import org.junit.Test

// Humo de domain/: la regla de negocio se verifica sin pantalla ni almacenamiento (RF-2).
class MyRecipesStateTest {

    @Test
    fun emptyCatalogProducesEmptyState() {
        assertEquals(MyRecipesState.Empty, myRecipesStateOf(emptyList()))
    }

    @Test
    fun catalogWithRecipesProducesWithRecipesState() {
        val recipes = listOf(Recipe(id = "1", title = "Tortilla de patatas"))
        assertEquals(MyRecipesState.WithRecipes, myRecipesStateOf(recipes))
    }

    @Test
    fun nonEmptyCatalogWithoutVisibleRecipesProducesNoResultsState() {
        val recipes = listOf(Recipe(id = "1", title = "Tortilla de patatas"))
        assertEquals(MyRecipesState.NoResults, myRecipesStateOf(recipes, emptyList()))
    }
}
