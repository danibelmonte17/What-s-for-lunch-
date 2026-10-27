package com.whatsforlunch.app.domain.shoppinglist

import com.whatsforlunch.app.domain.model.Aisle
import com.whatsforlunch.app.domain.model.ShoppingListItem

// Modo de agrupación de la Lista de la Compra (RD20, RF-AGL2): por Pasillo o por
// Receta. La selección vive en memoria en la UI y se reinicia al salir de la
// pantalla (D1).
enum class ShoppingGroupMode { BY_AISLE, BY_RECIPE }

// Grupo de la Lista agrupada (RD20). `key` identifica el grupo: el nombre del
// enum `Aisle` o "" para «Sin clasificar» (BY_AISLE) y el `sourceKey` de la
// fuente o "" para «Sin receta» (BY_RECIPE). `title` es el título de la fuente
// (solo BY_RECIPE; "" cuando el grupo es «Sin receta»). En BY_RECIPE un ítem
// multi-fuente se replica en cada grupo de sus fuentes (D2); todas sus
// apariciones comparten el mismo `completed`.
data class ShoppingGroup(
    val key: String,
    val title: String,
    val items: List<ShoppingListItem>,
)

// Progreso de la Lista (RD20, RF-AGL1): cuenta ítems únicos con independencia
// del modo de agrupación (los multi-fuente una vez, D9).
data class ShoppingProgress(
    val completed: Int,
    val total: Int,
    val percent: Int,
)

// Clave de un grupo por Pasillo como enum, o null para «Sin clasificar».
fun ShoppingGroup.aisleKey(): Aisle? = Aisle.entries.firstOrNull { it.name == key }
