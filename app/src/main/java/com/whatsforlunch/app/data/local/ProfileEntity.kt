package com.whatsforlunch.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

// Fila única del perfil (`crear-mi-perfil`, D1): solo guarda el tipo de
// alimentación (nombre del enum `DietType`, `null` sin elegir, D2) y las marcas
// «No tengo ninguna» de cada sección; las chips viven en `profile_restrictions`
// para conservar su orden de alta.
@Entity(tableName = "profile")
data class ProfileEntity(
    @PrimaryKey val id: String,
    val dietType: String?,
    val allergiesNone: Boolean,
    val intolerancesNone: Boolean,
) {
    companion object {
        /** Clave de la única fila de la tabla. */
        const val SINGLE_ID = "single"
    }
}
