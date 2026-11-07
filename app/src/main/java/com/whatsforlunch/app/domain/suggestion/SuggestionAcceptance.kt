package com.whatsforlunch.app.domain.suggestion

import com.whatsforlunch.app.domain.match.calculateMatch
import com.whatsforlunch.app.domain.model.PantryItem
import com.whatsforlunch.app.domain.model.Recipe
import com.whatsforlunch.app.domain.model.RecipeOrigin
import com.whatsforlunch.app.domain.text.normalizeName
import java.util.UUID

// Estado de una Sugerencia dentro de la lista de resultados (D3-D5). Vive solo en
// memoria mientras dura la pantalla; las Sugerencias no se persisten (D4 de
// `sugerir-recetas-con-ia`).
enum class SuggestionState {
    PROPOSED,
    ACCEPTED,
    DISCARDED,
}

/**
 * Materialización de una Sugerencia en Receta (RD07) y selección de las Recetas que
 * el prompt debe evitar (D2).
 *
 * Aceptar guarda una Receta con `origen = SUGGESTION` y sin foto (D1). El id se
 * deriva del título normalizado, de modo que aceptar dos veces la misma propuesta
 * reescribe la misma fila mediante el upsert del repositorio (D3) y nunca duplica la
 * Receta. Sin dependencias de Android ni de la UI.
 */
object SuggestionAcceptance {

    /** Umbral estrictamente mayor que este porcentaje para enviar una Receta al prompt (D2). */
    const val AVOID_MATCH_THRESHOLD_PERCENT = 60

    /**
     * Id estable de la Receta que materializa una Sugerencia (D3): mismo id para el
     * mismo título normalizado, acepte quien acepte y las veces que sea.
     */
    fun suggestionRecipeId(title: String): String =
        UUID.nameUUIDFromBytes("suggestion:${normalizeName(title)}".toByteArray(Charsets.UTF_8)).toString()

    /**
     * Convierte la propuesta en la Receta destino (RF-ASR2): sin foto, no
     * favorita, `origen = SUGGESTION`, con los Ingredientes y Pasos de la propuesta y
     * sus ids derivados del id estable.
     */
    fun suggestionToRecipe(suggestion: RecipeSuggestion): Recipe {
        val id = suggestionRecipeId(suggestion.title)
        return Recipe(
            id = id,
            title = suggestion.title,
            story = suggestion.story,
            mealMoment = suggestion.mealMoment,
            prepMinutes = suggestion.prepMinutes,
            cookMinutes = suggestion.cookMinutes,
            difficulty = suggestion.difficulty,
            baseServings = suggestion.baseServings,
            caloriesPerServing = suggestion.caloriesPerServing,
            photoPath = null,
            favorite = false,
            origin = RecipeOrigin.SUGGESTION,
            ingredients = suggestion.ingredients.mapIndexed { index, ingredient ->
                ingredient.copy(id = "$id-ingredient-$index")
            },
            steps = suggestion.steps.mapIndexed { index, step ->
                step.copy(id = "$id-step-$index")
            },
        )
    }

    /**
     * RF-ASR6 / D2: Recetas de Mis Recetas cuya Coincidencia con los Alimentos
     * seleccionados supera estrictamente [minPercent] (misma base redondeada que
     * RD02). Las Recetas sin requisitos obligatorios no tienen Coincidencia (RD01) y
     * quedan fuera. No modifica `calculateMatch` (RD01/RD10).
     */
    fun recipesToAvoid(
        foods: List<SuggestableFood>,
        recipes: List<Recipe>,
        minPercent: Int = AVOID_MATCH_THRESHOLD_PERCENT,
    ): List<Recipe> {
        if (foods.isEmpty()) return emptyList()
        val pantryItems = foods.map { food ->
            PantryItem(id = food.name, name = food.name, quantity = food.quantity)
        }
        return recipes.filter { recipe ->
            val match = calculateMatch(recipe, pantryItems) ?: return@filter false
            match.percentage > minPercent
        }
    }
}
