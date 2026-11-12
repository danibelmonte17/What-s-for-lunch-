package com.whatsforlunch.app.domain.match

import com.whatsforlunch.app.domain.model.PantryItem
import com.whatsforlunch.app.domain.model.Recipe
import com.whatsforlunch.app.domain.text.normalizeName

// Umbral de sugeribilidad de RD02 aplicado al porcentaje redondeado (D2): 85 incluido.
const val MATCH_SUGGESTION_THRESHOLD = 85

// Receta compatible con la Despensa junto a su Coincidencia (RF-MRC1, RF-MRC6).
data class CompatibleRecipe(
    val recipe: Recipe,
    val match: RecipeMatch,
)

// Recetas de Mi recetario que alcanzan el umbral de RD02, ordenadas por Coincidencia
// descendente, título normalizado ascendente e id (RF-MRC2). Sin filtrar por estado
// (D4) y sin obligatorios no entran (RF-MRC4, calculateMatch → null).
fun compatibleRecipes(
    recipes: List<Recipe>,
    pantryItems: List<PantryItem>,
): List<CompatibleRecipe> = recipes
    .mapNotNull { recipe ->
        calculateMatch(recipe, pantryItems)?.let { match -> CompatibleRecipe(recipe, match) }
    }
    .filter { it.match.percentage >= MATCH_SUGGESTION_THRESHOLD }
    .sortedWith(
        compareByDescending<CompatibleRecipe> { it.match.percentage }
            .thenBy { normalizeName(it.recipe.title) }
            .thenBy { it.recipe.id },
    )
