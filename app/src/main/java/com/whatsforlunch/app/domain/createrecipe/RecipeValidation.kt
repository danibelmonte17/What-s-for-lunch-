package com.whatsforlunch.app.domain.createrecipe

import com.whatsforlunch.app.domain.model.Recipe
import com.whatsforlunch.app.domain.model.RecipeIngredient
import com.whatsforlunch.app.domain.model.RecipeStep

// Requisito incumplido por una Receta en el formulario de alta.
enum class RecipeRequirement {
    TITLE,
    INGREDIENT,
    STEP,
    INVALID_INGREDIENT,
    INVALID_STEP,
}

// Validaciones puras del guardado manual del formulario (alta y edición): sin
// dependencias de UI. Guardar exige título, al menos un Ingrediente y un Paso
// válidos y ninguna fila/Paso inválido (revisa ⚠ S07). La foto es siempre
// opcional (D11): también en el alta y en la edición de Recetas con foto.
object RecipeValidation {

    const val MIN_BASE_SERVINGS = 1
    const val MAX_BASE_SERVINGS = 12

    fun isValidServings(servings: Int): Boolean = servings in MIN_BASE_SERVINGS..MAX_BASE_SERVINGS

    fun isValidQuantity(quantity: Double): Boolean = quantity > 0.0

    fun isValidMinutes(minutes: Int): Boolean = minutes >= 0

    fun isValidTimerMinutes(minutes: Int?): Boolean = minutes == null || minutes >= 0

    fun isValidCalories(calories: Int?): Boolean = calories == null || calories >= 0

    fun isIngredientInvalid(ingredient: RecipeIngredient): Boolean =
        ingredient.name.isBlank() || !isValidQuantity(ingredient.quantity)

    fun isStepInvalid(step: RecipeStep): Boolean = step.text.isBlank()

    // Requisitos incumplidos para guardar con el botón único del formulario:
    // título, un ingrediente y un paso presentes y válidos, y ninguna fila de
    // Ingrediente ni Paso inválidos. La foto nunca bloquea el guardado (D11).
    fun validateForSave(recipe: Recipe): Set<RecipeRequirement> = buildSet {
        if (recipe.title.isBlank()) add(RecipeRequirement.TITLE)
        if (recipe.ingredients.isEmpty()) add(RecipeRequirement.INGREDIENT)
        if (recipe.steps.isEmpty()) add(RecipeRequirement.STEP)
        if (recipe.ingredients.any { isIngredientInvalid(it) }) add(RecipeRequirement.INVALID_INGREDIENT)
        if (recipe.steps.any { isStepInvalid(it) }) add(RecipeRequirement.INVALID_STEP)
    }
}
