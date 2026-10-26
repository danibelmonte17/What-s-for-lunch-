package com.whatsforlunch.app.domain.repository

import com.whatsforlunch.app.domain.model.Aisle
import com.whatsforlunch.app.domain.model.RecipeIngredient
import com.whatsforlunch.app.domain.model.ShoppingListItem
import kotlinx.coroutines.flow.Flow

// Contrato de la Lista de la Compra (RD18/RD19). Vive en domain/ y no depende de
// Room ni de la UI.
interface ShoppingListRepository {
    fun observeItems(): Flow<List<ShoppingListItem>>

    // Añade los Faltantes de una fuente (Receta o Propuesta) de forma idempotente:
    // repetir la llamada con la misma `sourceKey` no duplica ni dobla cantidades.
    suspend fun addMissing(
        sourceKey: String,
        sourceTitle: String,
        ingredients: List<RecipeIngredient>,
    )

    // RD19: alta manual por nombre normalizado (upsert, D3): edita el ítem
    // existente (cantidad, unidad y Pasillo; conserva nombre y fuentes) o crea
    // uno nuevo sin fuentes. Con `id` se edita ese ítem concreto.
    suspend fun saveManualItem(
        id: String?,
        name: String,
        quantity: Double?,
        unit: String,
        aisle: Aisle?,
    )

    suspend fun deleteItem(id: String)

    // Restaura un ítem borrado con su cantidad, unidad y Origen intactos («Deshacer»).
    suspend fun restoreItem(item: ShoppingListItem)
}
