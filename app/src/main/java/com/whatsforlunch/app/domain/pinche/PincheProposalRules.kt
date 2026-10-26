package com.whatsforlunch.app.domain.pinche

import com.whatsforlunch.app.domain.model.PantryItem
import com.whatsforlunch.app.domain.model.Recipe
import com.whatsforlunch.app.domain.model.RecipeOrigin
import com.whatsforlunch.app.domain.model.RecipeStatus
import com.whatsforlunch.app.domain.suggestion.RecipeSuggestion
import com.whatsforlunch.app.domain.text.normalizeName
import java.util.UUID

/**
 * Reglas puras de la Propuesta del Pinche (RF-PCH7, RF-PCH8; RD15, RD16).
 *
 * Los Faltantes son solo los ingredientes obligatorios cuyo nombre normalizado no
 * está en la Despensa, sin duplicados y en el orden de la receta (RD15). Guardar la
 * Propuesta crea un Borrador sin foto con `origen = SUGGESTION` y un id estable
 * derivado del título, de modo que dos guardados reescriben la misma Receta (RD16).
 * Sin dependencias de Android ni de la UI.
 */
object PincheProposalRules {

    /**
     * RD15/D8: nombres de los ingredientes obligatorios ausentes de la Despensa, en
     * orden de la receta y sin duplicados. La comparación usa `normalizeName`.
     */
    fun missingRequired(
        proposal: RecipeSuggestion,
        pantryItems: List<PantryItem>,
    ): List<String> {
        val available = pantryItems.mapTo(mutableSetOf()) { normalizeName(it.name) }
        val seen = mutableSetOf<String>()
        return proposal.ingredients
            .filter { !it.optional }
            .filter { ingredient ->
                val normalized = normalizeName(ingredient.name)
                normalized !in available && seen.add(normalized)
            }
            .map { it.name.trim() }
    }

    /**
     * RD16: id estable de la Receta que materializa la Propuesta; mismo título
     * normalizado, misma fila aunque se guarde varias veces o en dos respuestas.
     */
    fun proposalRecipeId(title: String): String =
        UUID.nameUUIDFromBytes("pinche:${normalizeName(title)}".toByteArray(Charsets.UTF_8)).toString()

    /**
     * RF-PCH8/D9: Borrador sin foto, no favorito, `origen = SUGGESTION`, con los
     * Ingredientes y Pasos de la Propuesta y sus ids derivados del id estable.
     */
    fun proposalToRecipe(proposal: RecipeSuggestion): Recipe {
        val id = proposalRecipeId(proposal.title)
        return Recipe(
            id = id,
            title = proposal.title,
            story = proposal.story,
            mealMoment = proposal.mealMoment,
            prepMinutes = proposal.prepMinutes,
            cookMinutes = proposal.cookMinutes,
            difficulty = proposal.difficulty,
            baseServings = proposal.baseServings,
            caloriesPerServing = proposal.caloriesPerServing,
            photoPath = null,
            favorite = false,
            status = RecipeStatus.DRAFT,
            origin = RecipeOrigin.SUGGESTION,
            ingredients = proposal.ingredients.mapIndexed { index, ingredient ->
                ingredient.copy(id = "$id-ingredient-$index")
            },
            steps = proposal.steps.mapIndexed { index, step ->
                step.copy(id = "$id-step-$index")
            },
        )
    }
}
