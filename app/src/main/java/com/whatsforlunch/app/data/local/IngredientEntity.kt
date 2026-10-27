package com.whatsforlunch.app.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// Entidad Room de un Ingrediente de Receta. `position` conserva el orden de inserción.
@Entity(
    tableName = "ingredients",
    foreignKeys = [
        ForeignKey(
            entity = RecipeEntity::class,
            parentColumns = ["id"],
            childColumns = ["recipeId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("recipeId")],
)
data class IngredientEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recipeId: String,
    val name: String,
    val quantity: Double,
    val unit: String,
    val optional: Boolean,
    val position: Int,
    // Id estable del Ingrediente en el dominio (RD21); lo referencian los vínculos
    // de los Pasos (`StepEntity.ingredientIds`). Sin defaultValue declarado para
    // que Room no compare defaults con la columna de MIGRATION_3_4.
    val domainId: String,
)
