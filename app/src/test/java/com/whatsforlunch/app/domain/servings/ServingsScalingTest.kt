package com.whatsforlunch.app.domain.servings

import org.junit.Assert.assertEquals
import org.junit.Test

// RD03 y Q-escalar-raciones-1: escalado lineal de cantidades con Raciones 1-12
// y redondeo de visualización a máximo 1 decimal (verify 2 de feature_list.json).
class ServingsScalingTest {

    @Test
    fun scalesQuantityLinearlyWithServings() {
        assertEquals(100.0, ServingsScaling.scaleIngredientQuantity(200.0, 4, 2), 0.0001)
        assertEquals(300.0, ServingsScaling.scaleIngredientQuantity(200.0, 4, 6), 0.0001)
        assertEquals(50.0, ServingsScaling.scaleIngredientQuantity(200.0, 4, 1), 0.0001)
    }

    @Test
    fun roundsScaledQuantityToAtMostOneDecimal() {
        // 1 × (3 / 4) = 0,75 → 0,8 (la decisión del usuario, como el prototipo).
        assertEquals(0.8, ServingsScaling.scaleIngredientQuantity(1.0, 4, 3), 0.0001)
        // 100 × (4 / 3) = 133,33… → 133,3.
        assertEquals(133.3, ServingsScaling.scaleIngredientQuantity(100.0, 3, 4), 0.0001)
        // 0,5 × (2 / 4) = 0,25 → 0,3.
        assertEquals(0.3, ServingsScaling.scaleIngredientQuantity(0.5, 4, 2), 0.0001)
    }

    @Test
    fun clampsServingsToAllowedRange() {
        assertEquals(1, ServingsScaling.clampServings(0))
        assertEquals(1, ServingsScaling.clampServings(-5))
        assertEquals(4, ServingsScaling.clampServings(4))
        assertEquals(12, ServingsScaling.clampServings(13))
        assertEquals(12, ServingsScaling.clampServings(99))
    }

    @Test
    fun servingsOutsideRangeCannotScaleBeyondLimits() {
        // Fuera de rango se recorta a 1 o 12 antes de escalar (verify 2).
        assertEquals(50.0, ServingsScaling.scaleIngredientQuantity(200.0, 4, 0), 0.0001)
        assertEquals(600.0, ServingsScaling.scaleIngredientQuantity(200.0, 4, 13), 0.0001)
    }

    @Test
    fun quantityWithoutAmountStaysWithoutAmount() {
        assertEquals(0.0, ServingsScaling.scaleIngredientQuantity(0.0, 4, 12), 0.0001)
    }

    @Test
    fun invalidBaseServingsLeavesQuantityUnscaled() {
        assertEquals(200.0, ServingsScaling.scaleIngredientQuantity(200.0, 0, 2), 0.0001)
    }

    @Test
    fun ingredientAtBaseServingsKeepsItsQuantity() {
        assertEquals(200.0, ServingsScaling.scaleIngredientQuantity(200.0, 4, 4), 0.0001)
        // Sin cambio de raciones no se pierde precisión guardada (0,25 l sigue 0,25).
        assertEquals(0.25, ServingsScaling.scaleIngredientQuantity(0.25, 4, 4), 0.0001)
    }
}
