package com.whatsforlunch.app.domain.pinche

import com.whatsforlunch.app.domain.model.Difficulty
import com.whatsforlunch.app.domain.model.MealMoment
import com.whatsforlunch.app.domain.model.PantryItem
import com.whatsforlunch.app.domain.model.RecipeIngredient
import com.whatsforlunch.app.domain.model.RecipeOrigin
import com.whatsforlunch.app.domain.model.RecipeStep
import com.whatsforlunch.app.domain.suggestion.RecipeSuggestion
import com.whatsforlunch.app.domain.suggestion.SuggestionAcceptance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PincheProposalRulesTest {

    @Test
    fun missingRequiredKeepsOnlyAbsentIngredientsInRecipeOrder() {
        // D5: todos los Ingredientes cuentan (sin distinción obligatorio/opcional).
        val proposal = proposal(listOf("Tomate", "Harina", "Albahaca", "Queso"))
        val pantry = listOf(PantryItem("1", "tomate", 3))

        assertEquals(
            listOf("Harina", "Albahaca", "Queso"),
            PincheProposalRules.missingRequired(proposal, pantry),
        )
    }

    @Test
    fun missingRequiredNormalizesAccentsCaseAndSpacesAndAvoidsDuplicates() {
        val proposal = proposal(listOf("Tomate", "TOMATE", "Harina", "Harina"))
        val pantry = listOf(PantryItem("1", "  Tomaté  ", 1))

        assertEquals(listOf("Harina"), PincheProposalRules.missingRequired(proposal, pantry))
    }

    @Test
    fun emptyPantryMakesEveryIngredientMissing() {
        val proposal = proposal(listOf("Tomate", "Harina", "Albahaca"))

        assertEquals(
            listOf("Tomate", "Harina", "Albahaca"),
            PincheProposalRules.missingRequired(proposal, emptyList()),
        )
    }

    @Test
    fun proposalRecipeIdIsStableAndDoesNotReuseTheSuggestionPrefix() {
        val first = PincheProposalRules.proposalRecipeId("Tarta de manzana")
        val second = PincheProposalRules.proposalRecipeId("  TARTA de Manzana ")

        assertEquals(first, second)
        assertNotEquals(SuggestionAcceptance.suggestionRecipeId("Tarta de manzana"), first)
    }

    @Test
    fun proposalToRecipeBuildsARecipeWithSuggestionOriginAndNoPhoto() {
        val suggestion = proposal(listOf("Tomate", "Harina", "Albahaca"))
        val recipe = PincheProposalRules.proposalToRecipe(suggestion)
        val id = PincheProposalRules.proposalRecipeId(suggestion.title)

        assertEquals(id, recipe.id)
        assertEquals(RecipeOrigin.SUGGESTION, recipe.origin)
        assertNull(recipe.photoPath)
        assertFalse(recipe.favorite)
        assertEquals(suggestion.title, recipe.title)
        assertEquals(listOf("$id-ingredient-0", "$id-ingredient-1", "$id-ingredient-2"), recipe.ingredients.map { it.id })
        assertEquals(listOf("$id-step-0"), recipe.steps.map { it.id })
    }

    @Test
    fun savingTheSameProposalTwiceProducesTheSameId() {
        val suggestion = proposal(listOf("Tomate"))

        assertEquals(
            PincheProposalRules.proposalToRecipe(suggestion).id,
            PincheProposalRules.proposalToRecipe(suggestion).id,
        )
    }

    // D2 (unificar-unidades): al materializar la Propuesta la unidad queda
    // garantizada en el enum cerrado (lo vacío y lo desconocido caen a «ud»).
    @Test
    fun proposalToRecipeNormalizesUnitsToTheClosedEnum() {
        val base = proposal(listOf("Tomate", "Harina"))
        val suggestion = base.copy(
            ingredients = base.ingredients.mapIndexed { index, ingredient ->
                ingredient.copy(unit = if (index == 0) "gramos" else "")
            },
        )

        val recipe = PincheProposalRules.proposalToRecipe(suggestion)

        assertEquals(listOf("ud", "ud"), recipe.ingredients.map { it.unit })
    }

    private fun proposal(ingredients: List<String> = listOf("Tomate")): RecipeSuggestion = RecipeSuggestion(
        title = "Arroz con tomate",
        story = "Una receta sencilla",
        mealMoment = MealMoment.LUNCH,
        prepMinutes = 10,
        difficulty = Difficulty.EASY,
        baseServings = 4,
        caloriesPerServing = 320,
        ingredients = ingredients.mapIndexed { index, name ->
            RecipeIngredient(
                id = "$index",
                name = name,
                quantity = 1.0,
                unit = "ud",
            )
        },
        steps = listOf(RecipeStep(text = "Cocina la receta", timerMinutes = 5)),
    )
}
