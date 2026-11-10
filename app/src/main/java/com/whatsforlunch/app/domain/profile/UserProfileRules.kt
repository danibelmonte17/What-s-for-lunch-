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
 * - **RD27 — `save_profile`**: `applyUpdate` compone estas mismas reglas para
 *   guardar desde el chat lo que el usuario declare (solo los campos recibidos).
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

    /**
     * Aplica una actualización de `save_profile` al perfil (RD27): valida con estas
     * mismas reglas y, si algo es inválido, devuelve el código de error **sin
     * persistir nada**. Solo se modifican los campos que llegan (D5) y, por sección,
     * la lista completa sustituye la anterior (permite correcciones). `savedParts`
     * lista las partes escritas con su valor final, en el orden en que las pide el
     * Pinche, para el resumen fiel del chat (D2).
     */
    fun applyUpdate(profile: UserProfile, update: ProfileUpdate): ProfileUpdateOutcome {
        if (!update.hasFields()) {
            return ProfileUpdateOutcome.Invalid(ProfileUpdateError.INVALID_ARGUMENTS)
        }
        var current = profile
        val saved = mutableListOf<SavedProfilePart>()
        sectionPatches(update).forEach { patch ->
            when (val outcome = applySection(current, patch)) {
                is SectionOutcome.Rejected ->
                    return ProfileUpdateOutcome.Invalid(outcome.code)

                is SectionOutcome.Applied -> {
                    current = outcome.profile
                    saved += outcome.part
                }

                SectionOutcome.Untouched -> Unit
            }
        }
        update.dietType?.let { dietType ->
            current = setDietType(current, dietType)
            saved += SavedProfilePart(part = PendingProfilePart.DIET_TYPE, dietType = dietType)
        }
        return ProfileUpdateOutcome.Saved(current, saved)
    }

    private data class SectionPatch(
        val part: PendingProfilePart,
        val section: ProfileSection,
        val chips: List<String>?,
        val none: Boolean?,
    )

    private sealed interface SectionOutcome {
        data class Applied(val profile: UserProfile, val part: SavedProfilePart) : SectionOutcome
        data class Rejected(val code: String) : SectionOutcome
        data object Untouched : SectionOutcome
    }

    private fun sectionPatches(update: ProfileUpdate): List<SectionPatch> = listOf(
        SectionPatch(
            part = PendingProfilePart.ALLERGIES,
            section = ProfileSection.ALLERGY,
            chips = update.allergies,
            none = update.allergiesNone,
        ),
        SectionPatch(
            part = PendingProfilePart.INTOLERANCES,
            section = ProfileSection.INTOLERANCE,
            chips = update.intolerances,
            none = update.intolerancesNone,
        ),
    )

    private fun applySection(profile: UserProfile, patch: SectionPatch): SectionOutcome {
        val (part, section, chips, none) = patch
        if (chips == null && none == null) return SectionOutcome.Untouched
        if (chips != null) {
            val normalized = mutableListOf<String>()
            chips.forEach { raw ->
                val chip = normalizeChip(raw)
                    ?: return SectionOutcome.Rejected(ProfileUpdateError.INVALID_CHIP)
                if (isDuplicate(normalized, chip)) {
                    return SectionOutcome.Rejected(ProfileUpdateError.DUPLICATE_CHIP)
                }
                normalized += chip
            }
            // RD25/D3: «No tengo ninguna» es exclusiva con las chips: si llegan
            // juntas se rechaza sin persistir (el modelo debe corregirse).
            if (none == true && normalized.isNotEmpty()) {
                return SectionOutcome.Rejected(ProfileUpdateError.INVALID_CHIP)
            }
            val updated = if (none == true) {
                setNone(profile, section, none = true)
            } else {
                profile.withChips(section, normalized).withNone(section, none = false)
            }
            return SectionOutcome.Applied(updated, savedPart(part, updated, section))
        }
        // Solo la marca: marcarla limpia la sección; desmarcarla la deja pendiente.
        val updated = setNone(profile, section, none = none == true)
        return SectionOutcome.Applied(updated, savedPart(part, updated, section))
    }

    private fun savedPart(part: PendingProfilePart, profile: UserProfile, section: ProfileSection): SavedProfilePart =
        SavedProfilePart(part = part, chips = profile.chips(section), none = profile.isNone(section))
}
