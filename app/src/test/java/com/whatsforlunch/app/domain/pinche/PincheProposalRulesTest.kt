package com.whatsforlunch.app.domain.pinche

import com.whatsforlunch.app.domain.model.Difficulty
import com.whatsforlunch.app.domain.model.MealMoment
import com.whatsforlunch.app.domain.model.PantryItem
import com.whatsforlunch.app.domain.model.RecipeIngredient
import com.whatsforlunch.app.domain.model.RecipeOrigin
import com.whatsforlunch.app.domain.model.RecipeStatus
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
    fun missingRequiredKeepsOnlyAbsentRequiredIngredientsInRecipeOrder() {
        val proposal = proposal(
            required = listOf("Tomate", "Harina", "Albahaca"),
            optional = listOf("Queso"),
        )
        val pantry = listOf(PantryItem("1", "tomate", 3))

        assertEquals(
            listOf("Harina", "Albahaca"),
            PincheProposalRules.missingRequired(proposal, pantry),
        )
    }

    @Test
    fun missingRequiredNormalizesAccentsCaseAndSpacesAndAvoidsDuplicates() {
        val proposal = proposal(required = listOf("Tomate", "TOMATE", "Harina", "Harina"))
        val pantry = listOf(PantryItem("1", "  Tomaté  ", 1))

        assertEquals(listOf("Harina"), PincheProposalRules.missingRequired(proposal, pantry))
    }

    @Test
    fun emptyPantryMakesEveryRequiredIngredientMissing() {
        val proposal = proposal(required = listOf("Tomate", "Harina"), optional = listOf("Albahaca"))

        assertEquals(
            listOf("Tomate", "Harina"),
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
    fun proposalToRecipeBuildsADraftWithSuggestionOriginAndNoPhoto() {
        val suggestion = proposal(required = listOf("Tomate", "Harina"), optional = listOf("Albahaca"))
        val recipe = PincheProposalRules.proposalToRecipe(suggestion)
        val id = PincheProposalRules.proposalRecipeId(suggestion.title)

        assertEquals(id, recipe.id)
        assertEquals(RecipeStatus.DRAFT, recipe.status)
        assertEquals(RecipeOrigin.SUGGESTION, recipe.origin)
        assertNull(recipe.photoPath)
        assertFalse(recipe.favorite)
        assertEquals(suggestion.title, recipe.title)
        assertEquals(listOf("$id-ingredient-0", "$id-ingredient-1", "$id-ingredient-2"), recipe.ingredients.map { it.id })
        assertEquals(listOf("$id-step-0"), recipe.steps.map { it.id })
    }

    @Test
    fun savingTheSameProposalTwiceProducesTheSameId() {
        val suggestion = proposal(required = listOf("Tomate"))

        assertEquals(
            PincheProposalRules.proposalToRecipe(suggestion).id,
            PincheProposalRules.proposalToRecipe(suggestion).id,
        )
    }

    private fun proposal(
        required: List<String> = listOf("Tomate"),
        optional: List<String> = emptyList(),
    ): RecipeSuggestion = RecipeSuggestion(
        title = "Arroz con tomate",
        story = "Una receta sencilla",
        mealMoment = MealMoment.LUNCH,
        prepMinutes = 10,
        cookMinutes = 20,
        difficulty = Difficulty.EASY,
        baseServings = 4,
        caloriesPerServing = 320,
        ingredients = (required + optional).mapIndexed { index, name ->
            RecipeIngredient(
                id = "$index",
                name = name,
                quantity = 1.0,
                unit = "ud",
                optional = name in optional,
            )
        },
        steps = listOf(RecipeStep(text = "Cocina la receta", timerMinutes = 5)),
    )
}
