package com.whatsforlunch.app.domain.suggestion

import com.whatsforlunch.app.domain.model.Difficulty
import com.whatsforlunch.app.domain.model.MealMoment
import com.whatsforlunch.app.domain.model.Recipe
import com.whatsforlunch.app.domain.model.RecipeIngredient
import com.whatsforlunch.app.domain.model.RecipeOrigin
import com.whatsforlunch.app.domain.model.RecipeStatus
import com.whatsforlunch.app.domain.model.RecipeStep
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// T1 (D1, D2, D3): id estable, materialización como Borrador con origen Sugerencia y
// umbral estricto > 60 % para las Recetas que el prompt debe evitar.
class SuggestionAcceptanceTest {

    @Test
    fun suggestionRecipeIdIsStablePerNormalizedTitle() {
        val id = SuggestionAcceptance.suggestionRecipeId("Tarta de manzana")
        assertEquals(id, SuggestionAcceptance.suggestionRecipeId("  tarta de MANZANA "))
        assertEquals(id, SuggestionAcceptance.suggestionRecipeId("Tarta de manzana"))
        assertEquals(id, SuggestionAcceptance.suggestionRecipeId("Tartá de manzana"))
        assertNotEquals(id, SuggestionAcceptance.suggestionRecipeId("Tarta de pera"))
    }

    @Test
    fun acceptingCreatesADraftSuggestionRecipeWithoutPhoto() {
        val suggestion = suggestion(
            title = "Tarta de manzana",
            ingredients = listOf(
                RecipeIngredient(id = "sugerencia-0", name = "Manzana", quantity = 3.0, unit = "ud"),
                RecipeIngredient(id = "generado-1", name = "Canela", quantity = 0.5, unit = "cdta", optional = true),
            ),
            steps = listOf(
                RecipeStep(id = "paso-0", text = "Pela las manzanas", timerMinutes = 10),
                RecipeStep(id = "paso-1", text = "Hornea", timerMinutes = null),
            ),
        )

        val recipe = SuggestionAcceptance.suggestionToRecipe(suggestion)
        val id = SuggestionAcceptance.suggestionRecipeId("Tarta de manzana")

        assertEquals(id, recipe.id)
        assertEquals("Tarta de manzana", recipe.title)
        assertEquals(RecipeStatus.DRAFT, recipe.status)
        assertEquals(RecipeOrigin.SUGGESTION, recipe.origin)
        assertNull(recipe.photoPath)
        assertEquals(false, recipe.favorite)
        assertEquals(suggestion.story, recipe.story)
        assertEquals(suggestion.mealMoment, recipe.mealMoment)
        assertEquals(suggestion.prepMinutes, recipe.prepMinutes)
        assertEquals(suggestion.cookMinutes, recipe.cookMinutes)
        assertEquals(suggestion.difficulty, recipe.difficulty)
        assertEquals(suggestion.baseServings, recipe.baseServings)
        assertEquals(suggestion.caloriesPerServing, recipe.caloriesPerServing)
        assertEquals(listOf("$id-ingredient-0", "$id-ingredient-1"), recipe.ingredients.map { it.id })
        assertEquals(listOf("$id-step-0", "$id-step-1"), recipe.steps.map { it.id })
        assertEquals(0.5, recipe.ingredients[1].quantity, 0.0)
        assertTrue(recipe.ingredients[1].optional)
        assertEquals(10, recipe.steps[0].timerMinutes)
        assertNull(recipe.steps[1].timerMinutes)
    }

    @Test
    fun acceptingTheSameSuggestionTwiceProducesTheSameRecipeId() {
        val suggestion = suggestion(title = "Tarta de manzana")

        val first = SuggestionAcceptance.suggestionToRecipe(suggestion)
        val second = SuggestionAcceptance.suggestionToRecipe(suggestion)

        assertEquals(first.id, second.id)
    }

    @Test
    fun onlyRecipesAboveSixtyPercentAreSentToThePrompt() {
        val foods = listOf(SuggestableFood("Manzana"), SuggestableFood("Nata"))
        val full = recipe("Tarta de manzana", listOf("Manzana"))
        val above = recipe("Tarta de nata", listOf("Nata", "Manzana", "Harina"))
        val exactlySixty = recipe("Sopa", listOf("Manzana", "Nata", "Caldo", "Pan", "Sal"))
        val below = recipe("Ensalada", listOf("Lechuga", "Tomate", "Manzana"))

        val avoided = SuggestionAcceptance.recipesToAvoid(foods, listOf(full, above, exactlySixty, below))

        assertEquals(listOf("Tarta de manzana", "Tarta de nata"), avoided.map { it.title })
    }

    @Test
    fun recipeWithoutRequiredIngredientsIsNotSentToThePrompt() {
        val foods = listOf(SuggestableFood("Manzana"))
        val onlyOptional = recipe(
            title = "Adorno",
            required = emptyList(),
            optional = listOf("Manzana"),
        )

        assertTrue(SuggestionAcceptance.recipesToAvoid(foods, listOf(onlyOptional)).isEmpty())
    }

    @Test
    fun noSelectedFoodsMeansNoRecipesToAvoid() {
        assertTrue(SuggestionAcceptance.recipesToAvoid(emptyList(), listOf(recipe("Tarta", listOf("Manzana")))).isEmpty())
    }

    private fun suggestion(
        title: String,
        ingredients: List<RecipeIngredient> = listOf(RecipeIngredient(id = "i", name = "Manzana", quantity = 1.0)),
        steps: List<RecipeStep> = listOf(RecipeStep(id = "p", text = "Cocina")),
    ) = RecipeSuggestion(
        title = title,
        story = "Una receta propuesta por la IA",
        mealMoment = MealMoment.DESSERT,
        prepMinutes = 10,
        cookMinutes = 20,
        difficulty = Difficulty.EASY,
        baseServings = 4,
        caloriesPerServing = 320,
        ingredients = ingredients,
        steps = steps,
    )

    private fun recipe(
        title: String,
        required: List<String>,
        optional: List<String> = emptyList(),
    ) = Recipe(
        id = title,
        title = title,
        ingredients = (required + optional).map { name ->
            RecipeIngredient(name = name, quantity = 1.0, optional = name in optional)
        },
        steps = listOf(RecipeStep(text = "Cocina")),
    )
}
