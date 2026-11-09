package com.whatsforlunch.app.domain.profile

import com.whatsforlunch.app.domain.model.DietType
import com.whatsforlunch.app.domain.model.ProfileSection
import com.whatsforlunch.app.domain.model.UserProfile

// Parte del perfil que al Pinche le falta completar antes de responder (D5):
// cada sección de restricciones y el tipo de alimentación.
enum class PendingProfilePart {
    ALLERGIES,
    INTOLERANCES,
    DIET_TYPE,
}

/**
 * Reglas puras del perfil (`crear-mi-perfil`, sin dependencias de Android; la UI
 * no contiene reglas de negocio).
 *
 * - **Validación de chip** (T1): se recorta, no puede quedar vacía y no se
 *   duplica ignorando mayúsculas; una chip rechazada no cambia el perfil.
 * - **RD25 / D3 — exclusividad** con «No tengo ninguna»: marcarla limpia las
 *   chips de su sección y bloquea el alta (la UI deshabilita el campo); añadir
 *   una chip la desmarca.
 * - **RD26 / D5 — completitud**: una sección de restricciones está completa con
 *   ≥1 chip o «No tengo ninguna», y el tipo de alimentación al estar elegido
 *   (sin preselección, D2). `pendingParts` lista lo que falta para que el Pinche
 *   deje de pedirlo.
 */
object UserProfileRules {

    /** Chip recortada lista para persistir; `null` si queda vacía. */
    fun normalizeChip(value: String): String? = value.trim().takeIf { it.isNotEmpty() }

    /** `true` si la chip ya existe en la sección (ignorando mayúsculas). */
    fun isDuplicate(chips: List<String>, value: String): Boolean =
        chips.any { it.equals(value.trim(), ignoreCase = true) }

    /**
     * Añade una chip a la sección aplicando RD25: devuelve `null` cuando la chip
     * se rechaza (vacía o duplicada) sin alterar el perfil; en caso contrario la
     * añade y **desmarca** «No tengo ninguna» de esa sección.
     */
    fun addChip(profile: UserProfile, section: ProfileSection, value: String): UserProfile? {
        val chip = normalizeChip(value) ?: return null
        val chips = profile.chips(section)
        if (isDuplicate(chips, chip)) return null
        return profile
            .withChips(section, chips + chip)
            .withNone(section, none = false)
    }

    /**
     * Quita una chip de la sección por su valor (sin duplicados el valor es
     * único); la marca «No tengo ninguna» no cambia al borrar (RD25).
     */
    fun removeChip(profile: UserProfile, section: ProfileSection, value: String): UserProfile =
        profile.withChips(section, profile.chips(section).filterNot { it.equals(value, ignoreCase = true) })

    /**
     * Marca o desmarca «No tengo ninguna» (RD25/D3): al marcarla se limpian las
     * chips de la sección; al desmarcarla la sección queda sin chips y pendiente.
     */
    fun setNone(profile: UserProfile, section: ProfileSection, none: Boolean): UserProfile {
        val cleared = if (none) profile.withChips(section, emptyList()) else profile
        return cleared.withNone(section, none)
    }

    /** Sección completa: ≥1 chip o «No tengo ninguna» (D5). */
    fun isSectionComplete(profile: UserProfile, section: ProfileSection): Boolean =
        profile.chips(section).isNotEmpty() || profile.isNone(section)

    /**
     * Fija el tipo de alimentación (D2): selección única sin preselección, la
     * nueva elección sustituye siempre a la anterior.
     */
    fun setDietType(profile: UserProfile, dietType: DietType?): UserProfile =
        profile.copy(dietType = dietType)

    /** Partes del perfil que siguen pendientes, en el orden en que las pide el Pinche (D5). */
    fun pendingParts(profile: UserProfile): List<PendingProfilePart> = buildList {
        if (!isSectionComplete(profile, ProfileSection.ALLERGY)) add(PendingProfilePart.ALLERGIES)
        if (!isSectionComplete(profile, ProfileSection.INTOLERANCE)) add(PendingProfilePart.INTOLERANCES)
        if (profile.dietType == null) add(PendingProfilePart.DIET_TYPE)
    }

    /** Perfil completo = ninguna parte pendiente (D5). */
    fun isComplete(profile: UserProfile): Boolean = pendingParts(profile).isEmpty()
}
