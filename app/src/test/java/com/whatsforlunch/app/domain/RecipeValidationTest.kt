package com.whatsforlunch.app.domain

import com.whatsforlunch.app.domain.createrecipe.RecipeRequirement
import com.whatsforlunch.app.domain.createrecipe.RecipeValidation
import com.whatsforlunch.app.domain.model.Recipe
import com.whatsforlunch.app.domain.model.RecipeIngredient
import com.whatsforlunch.app.domain.model.RecipeStep
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// verify 6 / RF-EBF3, RF-EBF4: validaciones puras del guardado manual del formulario
// (alta y edición), sin pantalla ni almacenamiento.
class RecipeValidationTest {

    private fun validRecipe(
        title: String = "Tortilla de patatas",
        photoPath: String? = "/tmp/tortilla.jpg",
        ingredients: List<RecipeIngredient> = listOf(RecipeIngredient(name = "Patata", quantity = 700.0, unit = "g")),
        steps: List<RecipeStep> = listOf(RecipeStep(text = "Pelar y pochar")),
    ): Recipe = Recipe(
        id = "1",
        title = title,
        photoPath = photoPath,
        ingredients = ingredients,
        steps = steps,
    )

    @Test
    fun servingsAreLimitedTo1To12() {
        assertFalse(RecipeValidation.isValidServings(0))
        assertTrue(RecipeValidation.isValidServings(1))
        assertTrue(RecipeValidation.isValidServings(12))
        assertFalse(RecipeValidation.isValidServings(13))
    }

    @Test
    fun timesAreDurationMinutesWithinZeroToTwentyThreeFiftyNine() {
        // D7: el reloj es duración HH:MM en 24 h (00:00-23:59 en minutos).
        assertFalse(RecipeValidation.isValidMinutes(-1))
        assertTrue(RecipeValidation.isValidMinutes(0))
        assertTrue(RecipeValidation.isValidMinutes(35))
        assertTrue(RecipeValidation.isValidMinutes(RecipeValidation.MAX_DURATION_MINUTES))
        assertFalse(RecipeValidation.isValidMinutes(RecipeValidation.MAX_DURATION_MINUTES + 1))
    }

    @Test
    fun stepTimerIsNullOrPositiveMinutesUpToTwentyThreeFiftyNine() {
        // D10: null = «Sin temporizador»; 0 equivale a null y no es un temporizador válido.
        assertTrue(RecipeValidation.isValidTimerMinutes(null))
        assertFalse(RecipeValidation.isValidTimerMinutes(0))
        assertTrue(RecipeValidation.isValidTimerMinutes(1))
        assertTrue(RecipeValidation.isValidTimerMinutes(RecipeValidation.MAX_DURATION_MINUTES))
        assertFalse(RecipeValidation.isValidTimerMinutes(RecipeValidation.MAX_DURATION_MINUTES + 1))
    }

    @Test
    fun saveIsBlockedWithoutIngredientsOrSteps() {
        val recipe = validRecipe(ingredients = emptyList(), steps = emptyList())
        val unmet = RecipeValidation.validateForSave(recipe)
        assertTrue(RecipeRequirement.INGREDIENT in unmet)
        assertTrue(RecipeRequirement.STEP in unmet)
    }

    @Test
    fun saveIsBlockedWithoutTitle() {
        val unmet = RecipeValidation.validateForSave(validRecipe(title = ""))
        assertEquals(setOf(RecipeRequirement.TITLE), unmet)
    }

    @Test
    fun photoIsAlwaysOptional() {
        // D11: la foto no es obligatoria en ningún guardado manual (alta o edición).
        val recipe = validRecipe(photoPath = null)
        assertTrue(RecipeValidation.validateForSave(recipe).isEmpty())
    }

    @Test
    fun saveIsBlockedByAnInvalidIngredient() {
        val recipe = validRecipe(
            ingredients = listOf(
                RecipeIngredient(name = "Patata", quantity = 700.0),
                RecipeIngredient(name = "", quantity = 0.0),
            ),
        )
        assertTrue(RecipeRequirement.INVALID_INGREDIENT in RecipeValidation.validateForSave(recipe))
    }

    @Test
    fun saveIsBlockedByAnInvalidStep() {
        val recipe = validRecipe(
            steps = listOf(
                RecipeStep(text = "Pelar"),
                RecipeStep(text = "   "),
            ),
        )
        assertTrue(RecipeRequirement.INVALID_STEP in RecipeValidation.validateForSave(recipe))
    }

    @Test
    fun saveSucceedsWithTitlePhotoValidIngredientAndStep() {
        assertTrue(RecipeValidation.validateForSave(validRecipe()).isEmpty())
    }

    @Test
    fun saveAlwaysRequiresTitle() {
        val unmet = RecipeValidation.validateForSave(validRecipe(title = "  "))
        assertEquals(setOf(RecipeRequirement.TITLE), unmet)
    }
}
