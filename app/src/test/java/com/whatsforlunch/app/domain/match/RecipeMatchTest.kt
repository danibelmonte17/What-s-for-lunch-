package com.whatsforlunch.app.domain.match

import com.whatsforlunch.app.domain.model.PantryItem
import com.whatsforlunch.app.domain.model.Recipe
import com.whatsforlunch.app.domain.model.RecipeIngredient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecipeMatchTest {

    @Test
    fun calculatesPresenceOnlyFromRequiredIngredients() {
        val recipe = recipe(
            RecipeIngredient(id = "chicken", name = "Pollo"),
            RecipeIngredient(id = "rice", name = "Arroz"),
            RecipeIngredient(id = "lemon", name = "Limón", optional = true),
        )

        val result = calculateMatch(recipe, listOf(item("Pollo"), item("Limón", quantity = 0)))

        assertEquals(50, result?.percentage)
        assertEquals(1, result?.availableRequired)
        assertEquals(2, result?.totalRequired)
    }

    @Test
    fun roundsHalfUpAndRecognizesNormalizedNames() {
        val recipe = recipe(
            RecipeIngredient(id = "1", name = " Tomáte fresco "),
            RecipeIngredient(id = "2", name = "Pimienta"),
            RecipeIngredient(id = "3", name = "Sal"),
        )

        val result = calculateMatch(recipe, listOf(item("tomate fresco"), item("Pimienta")))

        assertEquals(67, result?.percentage)
    }

    @Test
    fun emptyPantryIsZeroAndAllRequiredItemsIsFullMatch() {
        val recipe = recipe(RecipeIngredient(id = "1", name = "Tomate"))

        assertEquals(0, calculateMatch(recipe, emptyList())?.percentage)
        assertTrue(calculateMatch(recipe, listOf(item("tomate")))?.isFullMatch == true)
    }

    @Test
    fun requiredPantryItemWithZeroQuantityCountsAsAvailable() {
        val recipe = recipe(RecipeIngredient(id = "chicken", name = "Pollo"))

        val result = calculateMatch(recipe, listOf(item("Pollo", quantity = 0)))

        assertEquals(100, result?.percentage)
        assertEquals(1, result?.availableRequired)
    }

    @Test
    fun recipeWithoutRequiredIngredientsHasNoMatch() {
        val recipe = recipe(RecipeIngredient(id = "1", name = "Sal", optional = true))

        assertNull(calculateMatch(recipe, listOf(item("Sal"))))
        assertNull(calculateMatch(recipe(), emptyList()))
    }

    private fun recipe(vararg ingredients: RecipeIngredient) = Recipe(
        id = "recipe",
        title = "Recipe",
        ingredients = ingredients.toList(),
    )

    private fun item(name: String, quantity: Int? = null) = PantryItem(
        id = name,
        name = name,
        quantity = quantity,
    )
}
