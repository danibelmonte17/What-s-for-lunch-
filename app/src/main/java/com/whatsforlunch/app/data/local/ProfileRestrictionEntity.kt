package com.whatsforlunch.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

// Chip de restricción del perfil (`crear-mi-perfil`): una fila por alergia o
// intolerancia, con su sección (`ALLERGY`/`INTOLERANCE`, nombre del enum en
// inglés). El orden de la lista es el `id` autoincremental (orden de alta).
@Entity(tableName = "profile_restrictions")
data class ProfileRestrictionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val section: String,
    val value: String,
)
