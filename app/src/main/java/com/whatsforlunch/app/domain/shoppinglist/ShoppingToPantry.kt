package com.whatsforlunch.app.domain.shoppinglist

import com.whatsforlunch.app.domain.model.PantryItem
import com.whatsforlunch.app.domain.model.ShoppingListItem
import com.whatsforlunch.app.domain.pantry.PantryItemValidation
import kotlin.math.roundToInt

// RD29 (cambio directo 2026-11-12): pasa los ÍtemsLista marcados de la Lista de
// la Compra a la Despensa. Reglas puras en domain/ (P25): la UI no contiene
// reglas de negocio.
object ShoppingToPantry {

    /**
     * Cantidad del Alimento equivalente: la Despensa guarda enteros 0..99 sin
     * unidad, así que la cantidad decimal de la Lista se redondea al entero más
     * próximo y se recorta a ese rango; «0,4» → sin cantidad. La unidad de la
     * Lista no viaja (la Despensa no tiene unidades, RD13).
     */
    fun quantityOf(item: ShoppingListItem): Int? {
        val quantity = item.quantity?.takeIf { it.isFinite() } ?: return null
        return quantity
            .coerceIn(
                PantryItemValidation.MIN_QUANTITY.toDouble(),
                PantryItemValidation.MAX_QUANTITY.toDouble(),
            )
            .roundToInt()
            .takeUnless { it == PantryItemValidation.MIN_QUANTITY }
    }

    /** Alimento nuevo cuando no hay coincidencia; el id lo aporta el llamador. */
    fun toPantryItem(item: ShoppingListItem, id: String): PantryItem = PantryItem(
        id = id,
        name = item.name.trim(),
        quantity = quantityOf(item),
    )

    /**
     * RD13 sobre un Alimento existente: la cantidad se suma a la del Alimento
     * conservando su id y su nombre, sin crear fila nueva y recortando al
     * máximo de 99; «sin cantidad» cuenta como 0 (`null + N → N`, `M + 0 → M`).
     */
    fun sumInto(existing: PantryItem, item: ShoppingListItem): PantryItem {
        val incoming = quantityOf(item) ?: return existing
        val total = (existing.quantity ?: PantryItemValidation.MIN_QUANTITY) + incoming
        val merged = total.coerceAtMost(PantryItemValidation.MAX_QUANTITY)
        return existing.copy(quantity = merged.takeUnless { it == PantryItemValidation.MIN_QUANTITY })
    }
}
