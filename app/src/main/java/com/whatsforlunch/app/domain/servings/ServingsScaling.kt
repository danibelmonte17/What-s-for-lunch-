package com.whatsforlunch.app.domain.servings

import com.whatsforlunch.app.domain.createrecipe.RecipeValidation
import kotlin.math.roundToInt

// RD03: las cantidades de una Receta escalan linealmente con las Raciones
// (entre 1 y 12). Regla de dominio pura, sin dependencias de Android ni de la UI.
object ServingsScaling {

    // Mismos límites que el formulario de crear/editar (una sola fuente: RD03).
    const val MIN_SERVINGS: Int = RecipeValidation.MIN_BASE_SERVINGS
    const val MAX_SERVINGS: Int = RecipeValidation.MAX_BASE_SERVINGS

    // Q-escalar-raciones-1: la selección nunca sale de 1-12.
    fun clampServings(servings: Int): Int = servings.coerceIn(MIN_SERVINGS, MAX_SERVINGS)

    // cantidad = cantidad base × (raciones / raciones base), con el resultado
    // redondeado a máximo 1 decimal para mostrarlo (Q-escalar-raciones-1).
    // «Sin cantidad» (0) sigue sin cantidad y una Receta sin Raciones base
    // válidas (dato no alcanzable desde el formulario) no se escala.
    fun scaleIngredientQuantity(
        baseQuantity: Double,
        baseServings: Int,
        servings: Int,
    ): Double {
        if (baseQuantity <= 0.0) return 0.0
        if (!RecipeValidation.isValidServings(baseServings)) return baseQuantity

        val effectiveServings = clampServings(servings)
        // Raciones = Raciones base: se muestra la cantidad guardada tal cual, con
        // su precisión original (p. ej. 0,25 l no se redondea a 0,3 l).
        if (effectiveServings == baseServings) return baseQuantity

        val scaled = baseQuantity * (effectiveServings.toDouble() / baseServings)
        return (scaled * 10).roundToInt() / 10.0
    }
}
