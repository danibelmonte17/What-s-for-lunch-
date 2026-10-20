package com.whatsforlunch.app.domain

import com.whatsforlunch.app.domain.createrecipe.RecipeRequirement
import com.whatsforlunch.app.domain.createrecipe.RecipeValidation
import com.whatsforlunch.app.domain.model.Recipe
import com.whatsforlunch.app.domain.model.RecipeIngredient
import com.whatsforlunch.app.domain.model.RecipeStatus
import com.whatsforlunch.app.domain.model.RecipeStep
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// verify 3 · RF-3, RF-4, RF-8, RF-12, RF-15: validaciones puras del formulario de alta,
// sin pantalla ni almacenamiento.
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
        status = RecipeStatus.PUBLISHED,
    )

    @Test
    fun servingsAreLimitedTo1To12() {
        assertFalse(RecipeValidation.isValidServings(0))
        assertTrue(RecipeValidation.isValidServings(1))
        assertTrue(RecipeValidation.isValidServings(12))
        assertFalse(RecipeValidation.isValidServings(13))
    }

    @Test
    fun timesMustBeNonNegativeMinutes() {
        assertFalse(RecipeValidation.isValidMinutes(-1))
        assertTrue(RecipeValidation.isValidMinutes(0))
        assertTrue(RecipeValidation.isValidMinutes(35))
    }

    @Test
    fun draftOnlyRequiresTitle() {
        val empty = Recipe(id = "1", title = "")
        assertEquals(setOf(RecipeRequirement.TITLE), RecipeValidation.validateForDraft(empty))

        val withTitle = Recipe(id = "1", title = "Solo título")
        assertTrue(RecipeValidation.validateForDraft(withTitle).isEmpty())
    }

    @Test
    fun publishIsBlockedWithoutIngredientsOrSteps() {
        val recipe = validRecipe(ingredients = emptyList(), steps = emptyList())
        val unmet = RecipeValidation.validateForPublish(recipe)
        assertTrue(RecipeRequirement.INGREDIENT in unmet)
        assertTrue(RecipeRequirement.STEP in unmet)
    }

    @Test
    fun publishIsBlockedWithoutPhotoOrTitle() {
        val recipe = validRecipe(title = "", photoPath = null)
        val unmet = RecipeValidation.validateForPublish(recipe)
        assertTrue(RecipeRequirement.TITLE in unmet)
        assertTrue(RecipeRequirement.PHOTO in unmet)
    }

    @Test
    fun publishIsBlockedByAnInvalidIngredient() {
        val recipe = validRecipe(
            ingredients = listOf(
                RecipeIngredient(name = "Patata", quantity = 700.0),
                RecipeIngredient(name = "", quantity = 0.0),
            ),
        )
        assertTrue(RecipeRequirement.INVALID_INGREDIENT in RecipeValidation.validateForPublish(recipe))
    }

    @Test
    fun publishIsBlockedByAnInvalidStep() {
        val recipe = validRecipe(
            steps = listOf(
                RecipeStep(text = "Pelar"),
                RecipeStep(text = "   "),
            ),
        )
        assertTrue(RecipeRequirement.INVALID_STEP in RecipeValidation.validateForPublish(recipe))
    }

    @Test
    fun publishSucceedsWithTitlePhotoValidIngredientAndStep() {
        assertTrue(RecipeValidation.validateForPublish(validRecipe()).isEmpty())
    }
}
