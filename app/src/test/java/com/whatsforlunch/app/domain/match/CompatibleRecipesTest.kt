package com.whatsforlunch.app.domain.match

import com.whatsforlunch.app.domain.model.PantryItem
import com.whatsforlunch.app.domain.model.Recipe
import com.whatsforlunch.app.domain.model.RecipeIngredient
import com.whatsforlunch.app.domain.model.RecipeOrigin
import org.junit.Assert.assertEquals
import org.junit.Test

class CompatibleRecipesTest {

    @Test
    fun keepsRecipesAtOrAboveRoundedThresholdOrderedByMatchThenTitleThenId() {
        val a100 = recipe(id = "a100", title = "Tortilla de patatas", required = 4)
        val a100b = recipe(id = "a100b", title = "Arroz blanco", required = 2)
        val b85 = recipe(id = "b85", title = "Pechuga al horno", required = 20)
        val c100 = recipe(id = "c100", title = "Arroz blanco", required = 2)
        val d84 = recipe(id = "d84", title = "Pasta", required = 19)
        val e50 = recipe(id = "e50", title = "Sopa", required = 2)
        val recipes = listOf(b85, a100, c100, a100b, d84, e50)
        val pantry = pantryFor(
            a100 to 4,
            a100b to 2,
            b85 to 17,
            c100 to 2,
            d84 to 16,
            e50 to 1,
        )

        val result = compatibleRecipes(recipes, pantry)

        assertEquals(
            listOf("a100b", "c100", "a100", "b85"),
            result.map { it.recipe.id },
        )
        assertEquals(listOf(100, 100, 100, 85), result.map { it.match.percentage })
    }

    @Test
    fun ignoresRecipesBelowThresholdIncluding84() {
        val recipe = recipe(id = "pasta", title = "Pasta", required = 19)

        assertEquals(84, calculateMatch(recipe, pantryFor(recipe to 16))?.percentage)
        assertEquals(emptyList<CompatibleRecipe>(), compatibleRecipes(listOf(recipe), pantryFor(recipe to 16)))
    }

    @Test
    fun usesRoundedPercentageForThresholdSoElevenOfThirteenEnters() {
        val recipe = recipe(id = "eleven", title = "Once", required = 13)

        assertEquals(85, calculateMatch(recipe, pantryFor(recipe to 11))?.percentage)
        assertEquals(
            listOf("eleven"),
            compatibleRecipes(listOf(recipe), pantryFor(recipe to 11)).map { it.recipe.id },
        )
    }

    @Test
    fun recipesWithoutIngredientsNeverEnter() {
        // D12: sin Ingredientes no hay Coincidencia (antes, recetas «solo opcionales»).
        val withoutIngredients = Recipe(id = "empty", title = "Vacía")

        assertEquals(
            emptyList<CompatibleRecipe>(),
            compatibleRecipes(listOf(withoutIngredients), pantryFor(withoutIngredients to 1)),
        )
    }

    @Test
    fun userAndSuggestionRecipesEnterEqually() {
        val userRecipe = recipe(id = "user", title = "Receta propia", required = 2)
        val suggestion = recipe(id = "suggestion", title = "De sugerencia", required = 2, origin = RecipeOrigin.SUGGESTION)

        val result = compatibleRecipes(
            listOf(userRecipe, suggestion),
            pantryFor(userRecipe to 2, suggestion to 2),
        )

        // Empate de Coincidencia: el desempate es por título normalizado («de sugerencia»
        // antes que «receta propia»), con igualdad de trato para ambos orígenes.
        assertEquals(listOf("suggestion", "user"), result.map { it.recipe.id })
    }

    @Test
    fun pantryItemsWithZeroQuantityCountAsAvailable() {
        val recipe = recipe(id = "tomato", title = "Tomate", required = 1)

        val result = compatibleRecipes(
            listOf(recipe),
            listOf(PantryItem("tomato", recipe.ingredients.single().name, quantity = 0)),
        )

        assertEquals(100, result.single().match.percentage)
    }

    private fun recipe(
        id: String,
        title: String,
        required: Int,
        origin: RecipeOrigin = RecipeOrigin.USER,
    ): Recipe = Recipe(
        id = id,
        title = title,
        origin = origin,
        ingredients = (1..required).map { index ->
            RecipeIngredient(id = "$id-$index", name = "$id ingredient $index")
        },
    )

    private fun pantryFor(vararg available: Pair<Recipe, Int>): List<PantryItem> =
        available.flatMap { (recipe, count) ->
            recipe.ingredients.take(count).map { ingredient ->
                PantryItem(id = "pantry-${ingredient.id}", name = ingredient.name)
            }
        }
}
