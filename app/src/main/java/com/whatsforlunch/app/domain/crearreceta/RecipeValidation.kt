package com.whatsforlunch.app.domain.crearreceta

import com.whatsforlunch.app.domain.model.Recipe
import com.whatsforlunch.app.domain.model.RecipeIngredient
import com.whatsforlunch.app.domain.model.RecipeStep

// Requisito incumplido por una Receta en el formulario de alta.
enum class RecipeRequirement {
    TITLE,
    PHOTO,
    INGREDIENT,
    STEP,
    INVALID_INGREDIENT,
    INVALID_STEP,
}

// Validaciones puras del formulario de alta (Parte B, T1): sin dependencias de UI.
// El Borrador solo exige título (Q-crear-receta-2); Publicar exige título, foto,
// al menos un Ingrediente y un Paso válidos y ninguna fila/Paso inválido
// (RF-8, RF-12, RF-15, RF-16, RF-17; revisa ⚠ S07).
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

    // Requisitos incumplidos para guardar como Borrador: solo el título.
    fun validateForDraft(recipe: Recipe): Set<RecipeRequirement> = buildSet {
        if (recipe.title.isBlank()) add(RecipeRequirement.TITLE)
    }

    // Requisitos incumplidos para Publicar: título, foto, un ingrediente y un paso
    // presentes y válidos, y ninguna fila de Ingrediente ni Paso inválidos.
    fun validateForPublish(recipe: Recipe): Set<RecipeRequirement> = buildSet {
        if (recipe.title.isBlank()) add(RecipeRequirement.TITLE)
        if (recipe.photoPath.isNullOrBlank()) add(RecipeRequirement.PHOTO)
        if (recipe.ingredients.isEmpty()) add(RecipeRequirement.INGREDIENT)
        if (recipe.steps.isEmpty()) add(RecipeRequirement.STEP)
        if (recipe.ingredients.any { isIngredientInvalid(it) }) add(RecipeRequirement.INVALID_INGREDIENT)
        if (recipe.steps.any { isStepInvalid(it) }) add(RecipeRequirement.INVALID_STEP)
    }
}
