package com.whatsforlunch.app.domain.profile

import com.whatsforlunch.app.domain.model.DietType
import com.whatsforlunch.app.domain.model.UserProfile

// Argumentos de la función `save_profile` (`rellenar-perfil-con-el-pinche`, D5):
// solo los campos que llegan se tocan y los omitidos conservan su valor vigente.
// El tipo de alimentación llega ya mapeado al enum cerrado (D8): `null` = omitido.
data class ProfileUpdate(
    val allergies: List<String>? = null,
    val allergiesNone: Boolean? = null,
    val intolerances: List<String>? = null,
    val intolerancesNone: Boolean? = null,
    val dietType: DietType? = null,
) {
    /** `true` si la llamada trae al menos un campo (sin campos → `invalid_arguments`). */
    fun hasFields(): Boolean =
        allergies != null ||
            allergiesNone != null ||
            intolerances != null ||
            intolerancesNone != null ||
            dietType != null
}

/**
 * Parte del perfil escrita por `save_profile` en un turno, con su **valor final**
 * tal como quedó persistido (RD27): es lo que el chat resume como «He guardado…»
 * (D2, resumen fiel) y lo que el modelo recibe en el resultado de la función.
 */
data class SavedProfilePart(
    val part: PendingProfilePart,
    val chips: List<String> = emptyList(),
    val none: Boolean = false,
    val dietType: DietType? = null,
)

/**
 * Resultado de aplicar una actualización al perfil (RD27): `Saved` con el perfil
 * actualizado y las partes escritas, o `Invalid` con el código de error que se
 * devuelve al modelo para que se corrija. Un resultado inválido **no persiste nada**.
 */
sealed interface ProfileUpdateOutcome {
    data class Saved(
        val updated: UserProfile,
        val savedParts: List<SavedProfilePart>,
    ) : ProfileUpdateOutcome

    data class Invalid(val code: String) : ProfileUpdateOutcome
}

// Códigos de error que `save_profile` devuelve al modelo (RD27): el mismo
// vocabulario en el dominio y en el protocolo para no desincronizarlos.
object ProfileUpdateError {
    const val INVALID_ARGUMENTS = "invalid_arguments"
    const val INVALID_CHIP = "invalid_chip"
    const val DUPLICATE_CHIP = "duplicate_chip"
    const val UNKNOWN_DIET_TYPE = "unknown_diet_type"
}
