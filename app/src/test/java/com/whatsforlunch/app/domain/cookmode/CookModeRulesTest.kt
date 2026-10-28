package com.whatsforlunch.app.domain.cookmode

import com.whatsforlunch.app.domain.model.Recipe
import com.whatsforlunch.app.domain.model.RecipeIngredient
import com.whatsforlunch.app.domain.model.RecipeStep
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// RD22 (D1/D4/D9): las filas del drawer del Modo Cocina son los Ingredientes
// vinculados al Paso activo (RD21) en el orden de la Receta y con la cantidad
// escalada por RD03.
class CookModeRulesTest {

    @Test
    fun rowsFollowRecipeIngredientOrderAndScaleQuantities() {
        val recipe = recipe(
            ingredients = listOf(
                RecipeIngredient(id = "egg", name = "Huevos", quantity = 4.0, unit = "uds"),
                RecipeIngredient(id = "potato", name = "Patatas", quantity = 700.0, unit = "g"),
                RecipeIngredient(id = "oil", name = "Aceite", quantity = 0.25, unit = "l", optional = true),
            ),
            steps = listOf(
                // El vínculo declara Patatas antes que Huevos; la fila sale en el
                // orden de ingredientes de la Receta (D7 de RD21).
                RecipeStep(id = "s1", text = "Corta las patatas", ingredientIds = listOf("potato", "egg")),
                RecipeStep(id = "s2", text = "Fríe"),
            ),
        )

        val rows = CookModeRules.stepIngredientRows(recipe, stepIndex = 0, servings = 2)

        assertEquals(listOf("egg", "potato"), rows.map { it.id })
        assertEquals(2.0, rows.first { it.id == "egg" }.quantity, 0.0001)
        assertEquals(350.0, rows.first { it.id == "potato" }.quantity, 0.0001)
        // La identidad del Ingrediente se conserva (nombre, unidad, opcionalidad).
        assertEquals("Patatas", rows.first { it.id == "potato" }.name)
        assertEquals("g", rows.first { it.id == "potato" }.unit)
    }

    @Test
    fun stepWithoutLinksShowsNoRowsInsteadOfAllIngredients() {
        val recipe = recipe(
            ingredients = listOf(
                RecipeIngredient(id = "egg", name = "Huevos", quantity = 4.0, unit = "uds"),
                RecipeIngredient(id = "potato", name = "Patatas", quantity = 700.0, unit = "g"),
            ),
            steps = listOf(RecipeStep(id = "s1", text = "Mezcla")),
        )

        // D1/D4: sin vínculos no hay fallback a todos los Ingredientes.
        assertTrue(CookModeRules.stepIngredientRows(recipe, stepIndex = 0, servings = 4).isEmpty())
    }

    @Test
    fun baseServingsKeepTheStoredPrecision() {
        val recipe = recipe(
            ingredients = listOf(RecipeIngredient(id = "oil", name = "Aceite", quantity = 0.25, unit = "l")),
            steps = listOf(RecipeStep(id = "s1", text = "Calienta", ingredientIds = listOf("oil"))),
        )

        val rows = CookModeRules.stepIngredientRows(recipe, stepIndex = 0, servings = recipe.baseServings)

        assertEquals(0.25, rows.single().quantity, 0.0001)
    }

    @Test
    fun outOfRangeServingsAreClampedBeforeScaling() {
        val recipe = recipe(
            ingredients = listOf(RecipeIngredient(id = "potato", name = "Patatas", quantity = 700.0, unit = "g")),
            steps = listOf(RecipeStep(id = "s1", text = "Corta", ingredientIds = listOf("potato"))),
        )

        // 13 se recorta a 12: 700 × (12 / 4) = 2100.
        val rows = CookModeRules.stepIngredientRows(recipe, stepIndex = 0, servings = 13)

        assertEquals(2100.0, rows.single().quantity, 0.0001)
    }

    @Test
    fun orphanAndBlankIdsAreDiscarded() {
        val recipe = recipe(
            ingredients = listOf(
                RecipeIngredient(id = "egg", name = "Huevos", quantity = 2.0, unit = "uds"),
                RecipeIngredient(id = "", name = "Sal"),
            ),
            steps = listOf(
                RecipeStep(id = "s1", text = "Mezcla", ingredientIds = listOf("egg", "ghost", "")),
            ),
        )

        val rows = CookModeRules.stepIngredientRows(recipe, stepIndex = 0, servings = 4)

        // «ghost» no existe y el Ingrediente sin id estable no es vinculable (RD21).
        assertEquals(listOf("egg"), rows.map { it.id })
    }

    @Test
    fun unknownStepIndexShowsNoRows() {
        val recipe = recipe(
            ingredients = listOf(RecipeIngredient(id = "egg", name = "Huevos")),
            steps = listOf(RecipeStep(id = "s1", text = "Mezcla", ingredientIds = listOf("egg"))),
        )

        assertTrue(CookModeRules.stepIngredientRows(recipe, stepIndex = 5, servings = 4).isEmpty())
    }

    private fun recipe(
        ingredients: List<RecipeIngredient>,
        steps: List<RecipeStep>,
    ) = Recipe(
        id = "recipe",
        title = "Receta",
        baseServings = 4,
        ingredients = ingredients,
        steps = steps,
    )
}
