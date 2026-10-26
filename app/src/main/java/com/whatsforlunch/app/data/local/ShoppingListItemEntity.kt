package com.whatsforlunch.app.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

// ÍtemLista persistido (RD18): una fila por nombre normalizado. `aisle` y
// `completed` se modelan desde el inicio (D10); en esta feature siempre son
// null y false.
@Entity(
    tableName = "shopping_items",
    indices = [Index(value = ["normalizedName"], unique = true)],
)
data class ShoppingListItemEntity(
    @PrimaryKey val id: String,
    val name: String,
    val normalizedName: String,
    val quantity: Double?,
    val unit: String,
    val aisle: String?,
    val completed: Boolean,
)

// Origen de un ÍtemLista (RD09): una fila por fuente y ítem. La clave compuesta
// (itemId, sourceKey) garantiza que una misma fuente no se registre dos veces.
@Entity(
    tableName = "shopping_item_sources",
    primaryKeys = ["itemId", "sourceKey"],
    foreignKeys = [
        ForeignKey(
            entity = ShoppingListItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("itemId")],
)
data class ShoppingItemSourceEntity(
    val itemId: String,
    val sourceKey: String,
    val sourceTitle: String,
)

// ÍtemLista con sus fuentes, como lo devuelve Room (@Relation).
data class ShoppingItemWithSources(
    @Embedded val item: ShoppingListItemEntity,
    @Relation(parentColumn = "id", entityColumn = "itemId")
    val sources: List<ShoppingItemSourceEntity>,
)
