package com.whatsforlunch.app.domain.cookmode

import com.whatsforlunch.app.domain.model.Recipe
import com.whatsforlunch.app.domain.model.RecipeIngredient
import com.whatsforlunch.app.domain.servings.ServingsScaling

// RD22: filas del drawer «Ingredientes para este paso» del Modo Cocina. Son los
// Ingredientes vinculados al Paso activo (RD21), en el orden de ingredientes de la
// Receta (D7 de RD21) y con la cantidad escalada a las Raciones heredadas del
// Detalle (RD03). Regla de dominio pura, sin dependencias de Android ni de la UI (D9).
object CookModeRules {

    // Lista vacía = «Sin ingredientes» en el drawer: un Paso sin vínculos no muestra
    // nada más y nunca cae a «todos» los Ingredientes de la Receta (D1/D4). Las
    // referencias huérfanas (ids de Ingrediente inexistentes) se descartan sin error
    // (RD21), igual que los ids repetidos: cada Ingrediente aparece una sola vez.
    fun stepIngredientRows(
        recipe: Recipe,
        stepIndex: Int,
        servings: Int,
    ): List<RecipeIngredient> {
        val step = recipe.steps.getOrNull(stepIndex) ?: return emptyList()
        if (step.ingredientIds.isEmpty()) return emptyList()
        return recipe.ingredients
            .filter { ingredient -> ingredient.id.isNotBlank() && ingredient.id in step.ingredientIds }
            .map { ingredient ->
                ingredient.copy(
                    quantity = ServingsScaling.scaleIngredientQuantity(
                        baseQuantity = ingredient.quantity,
                        baseServings = recipe.baseServings,
                        servings = servings,
                    ),
                )
            }
    }
}
