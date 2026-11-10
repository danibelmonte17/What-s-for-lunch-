package com.whatsforlunch.app.domain.suggestion

import com.whatsforlunch.app.domain.model.Difficulty
import com.whatsforlunch.app.domain.model.MealMoment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SuggestionRulesTest {

    @Test
    fun normalizesCompleteProposal() {
        val suggestion = SuggestionRules.normalize(listOf(proposal())).single()

        assertEquals("Tarta de manzana", suggestion.title)
        assertEquals("Clásica", suggestion.story)
        assertEquals(MealMoment.DESSERT, suggestion.mealMoment)
        assertEquals(10, suggestion.prepMinutes)
        assertEquals(Difficulty.EASY, suggestion.difficulty)
        assertEquals(4, suggestion.baseServings)
        assertEquals(320, suggestion.caloriesPerServing)
    }

    @Test
    fun discardsBlankTitleOrEmptyIngredientsOrSteps() {
        val suggestions = SuggestionRules.normalize(
            listOf(
                proposal(title = "  "),
                proposal(title = "Sin ingredientes", ingredients = emptyList()),
                proposal(title = "Sin pasos", steps = emptyList()),
                proposal(title = "Válida"),
            ),
        )

        assertEquals(listOf("Válida"), suggestions.map { it.title })
    }

    @Test
    fun invalidQuantitiesBecomeOneAndBlankIngredientNamesAreDropped() {
        val suggestion = SuggestionRules.normalize(
            listOf(
                proposal(
                    ingredients = listOf(
                        RawRecipeIngredient(name = "Sal", quantity = null),
                        RawRecipeIngredient(name = "Azúcar", quantity = 0.0),
                        RawRecipeIngredient(name = "Harina", quantity = -2.0),
                        RawRecipeIngredient(name = " ", quantity = 3.0),
                    ),
                ),
            ),
        ).single()

        assertEquals(listOf("Sal", "Azúcar", "Harina"), suggestion.ingredients.map { it.name })
        assertEquals(listOf(1.0, 1.0, 1.0), suggestion.ingredients.map { it.quantity })
    }

    @Test
    fun unknownTokensAndOutOfRangeValuesFallBackToDefaults() {
        val suggestion = SuggestionRules.normalize(
            listOf(
                proposal(
                    mealMoment = "BRUNCH",
                    difficulty = "EXTREME",
                    prepMinutes = -5.0,
                    baseServings = 30.0,
                    calories = 0.0,
                    steps = listOf(
                        RawRecipeStep(text = "Paso", timerMinutes = -1.0),
                        RawRecipeStep(text = " ", timerMinutes = 5.0),
                    ),
                ),
            ),
        ).single()

        assertEquals(MealMoment.LUNCH, suggestion.mealMoment)
        assertEquals(Difficulty.MEDIUM, suggestion.difficulty)
        assertEquals(0, suggestion.prepMinutes)
        assertEquals(4, suggestion.baseServings)
        assertNull(suggestion.caloriesPerServing)
        assertEquals(listOf("Paso"), suggestion.steps.map { it.text })
        assertNull(suggestion.steps.single().timerMinutes)
    }

    @Test
    fun baseServingsWithinRangeAreKept() {
        val suggestion = SuggestionRules.normalize(listOf(proposal(baseServings = 12.0))).single()

        assertEquals(12, suggestion.baseServings)
    }

    // D2 (unificar-unidades): la unidad que llega de la IA se normaliza al enum
    // cerrado; la vacía o desconocida cae a «ud».
    @Test
    fun aiUnitsAreNormalizedToTheClosedEnum() {
        val suggestion = SuggestionRules.normalize(
            listOf(
                proposal(
                    ingredients = listOf(
                        RawRecipeIngredient(name = "Harina", quantity = 200.0, unit = "gramos"),
                        RawRecipeIngredient(name = "Leche", quantity = 1.0, unit = " ML "),
                        RawRecipeIngredient(name = "Huevo", quantity = 2.0, unit = ""),
                        RawRecipeIngredient(name = "Sal", quantity = 1.0, unit = null),
                        RawRecipeIngredient(name = "Azúcar", quantity = 1.0, unit = "kg"),
                    ),
                ),
            ),
        ).single()

        assertEquals(listOf("ud", "ml", "ud", "ud", "kg"), suggestion.ingredients.map { it.unit })
    }

    @Test
    fun duplicateTitlesKeepTheFirstProposal() {
        val suggestions = SuggestionRules.normalize(
            listOf(
                proposal(title = "Tarta de manzana", story = "primera"),
                proposal(title = "  TARTA DE MANZANA ", story = "segunda"),
            ),
        )

        assertEquals(listOf("primera"), suggestions.map { it.story })
    }

    @Test
    fun onlyThreeSuggestionsAreKept() {
        val suggestions = SuggestionRules.normalize(
            (1..5).map { index -> proposal(title = "Receta $index") },
        )

        assertEquals(listOf("Receta 1", "Receta 2", "Receta 3"), suggestions.map { it.title })
    }

    @Test
    fun matchCountsAllIngredientsAgainstSelectedFoods() {
        val suggestion = SuggestionRules.normalize(
            listOf(
                proposal(
                    ingredients = listOf(
                        RawRecipeIngredient(name = "Manzana"),
                        RawRecipeIngredient(name = "Nata"),
                        RawRecipeIngredient(name = "Canela"),
                    ),
                ),
            ),
        ).single()

        val matches = SuggestionRules.suggestionMatches(
            suggestions = listOf(suggestion),
            availableFoods = listOf(SuggestableFood("manzana")),
        )

        val match = matches.single().match
        assertEquals(33, match?.percentage)
        assertEquals(1, match?.availableRequired)
        assertEquals(3, match?.totalRequired)
    }

    @Test
    fun proposalWithASingleIngredientHasFullBadge() {
        val suggestion = SuggestionRules.normalize(
            listOf(proposal(ingredients = listOf(RawRecipeIngredient(name = "Sal")))),
        ).single()

        val matches = SuggestionRules.suggestionMatches(
            suggestions = listOf(suggestion),
            availableFoods = listOf(SuggestableFood("Sal")),
        )

        assertEquals(100, matches.single().match?.percentage)
    }

    private fun proposal(
        title: String = "Tarta de manzana",
        story: String = "Clásica",
        mealMoment: String = "DESSERT",
        prepMinutes: Double? = 10.0,
        difficulty: String = "EASY",
        baseServings: Double? = 4.0,
        calories: Double? = 320.0,
        ingredients: List<RawRecipeIngredient> = listOf(
            RawRecipeIngredient(name = "Manzana", quantity = 3.0, unit = "ud"),
            RawRecipeIngredient(name = "Nata", quantity = 200.0, unit = "ml"),
        ),
        steps: List<RawRecipeStep> = listOf(RawRecipeStep(text = "Pela las manzanas", timerMinutes = 10.0)),
    ) = RawRecipeSuggestion(
        title = title,
        story = story,
        mealMoment = mealMoment,
        prepMinutes = prepMinutes,
        difficulty = difficulty,
        baseServings = baseServings,
        calories = calories,
        ingredients = ingredients,
        steps = steps,
    )
}
