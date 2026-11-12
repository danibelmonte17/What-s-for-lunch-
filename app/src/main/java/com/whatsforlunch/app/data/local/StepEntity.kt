package com.whatsforlunch.app.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// Entidad Room de un Paso de Receta. `position` conserva el orden de inserción y el
// temporizador es opcional (null = sin temporizador).
@Entity(
    tableName = "steps",
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
data class StepEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recipeId: String,
    val text: String,
    val timerMinutes: Int?,
    val position: Int,
    // Ids de los Ingredientes que usa el Paso (RD21), separados por comas: los ids
    // (UUID o `{recipeId}-ingredient-{n}`) nunca contienen comas. Vacío = sin
    // vínculos. Sin defaultValue declarado para que Room no compare defaults.
    val ingredientIds: String,
)
