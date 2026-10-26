package com.whatsforlunch.app.domain.repository

import com.whatsforlunch.app.domain.model.RecipeIngredient
import com.whatsforlunch.app.domain.model.ShoppingListItem
import kotlinx.coroutines.flow.Flow

// Contrato de la Lista de la Compra (RD18). Vive en domain/ y no depende de Room
// ni de la UI.
interface ShoppingListRepository {
    fun observeItems(): Flow<List<ShoppingListItem>>

    // Añade los Faltantes de una fuente (Receta o Propuesta) de forma idempotente:
    // repetir la llamada con la misma `sourceKey` no duplica ni dobla cantidades.
    suspend fun addMissing(
        sourceKey: String,
        sourceTitle: String,
        ingredients: List<RecipeIngredient>,
    )

    suspend fun deleteItem(id: String)

    // Restaura un ítem borrado con su cantidad, unidad y Origen intactos («Deshacer»).
    suspend fun restoreItem(item: ShoppingListItem)
}
