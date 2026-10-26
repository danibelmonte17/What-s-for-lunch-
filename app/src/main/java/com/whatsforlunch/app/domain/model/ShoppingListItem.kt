package com.whatsforlunch.app.domain.model

// Origen de un ÍtemLista (RD09): la Receta o Propuesta del Pinche que aportó el
// ingrediente. `sourceKey` es el id de la Receta o el id estable de la Propuesta
// (RD16), de modo que la misma fuente nunca suma dos veces.
data class ShoppingItemSource(
    val sourceKey: String,
    val sourceTitle: String,
)

// ÍtemLista de la Lista de la Compra (docs/domain-model.md): un ítem por nombre
// normalizado, con la cantidad/unidad sin conversiones, sin Pasillo en esta feature
// (siempre null) y `completed = false` hasta agrupar-y-completar-la-lista.
data class ShoppingListItem(
    val id: String,
    val name: String,
    val quantity: Double? = null,
    val unit: String = "",
    val aisle: String? = null,
    val completed: Boolean = false,
    val sources: List<ShoppingItemSource> = emptyList(),
)
