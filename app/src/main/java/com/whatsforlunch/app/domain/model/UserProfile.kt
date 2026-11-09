package com.whatsforlunch.app.domain.model

// Sección de restricción del perfil (`crear-mi-perfil`): las dos secciones con
// alta por texto en chips y opción «No tengo ninguna». «Tipo de alimentación» no
// es una sección de restricciones: es una selección única (D2).
enum class ProfileSection {
    ALLERGY,
    INTOLERANCE,
}

/**
 * Perfil del usuario (`crear-mi-perfil`, D1): alergias e intolerancias o
 * enfermedades que limitan la dieta como listas de chips con su marca
 * «No tengo ninguna» por sección (RD25), más el tipo de alimentación sin
 * preselección (D2). Todo vive en local (Room) y solo el Pinche lo consume.
 */
data class UserProfile(
    val allergies: List<String> = emptyList(),
    val allergiesNone: Boolean = false,
    val intolerances: List<String> = emptyList(),
    val intolerancesNone: Boolean = false,
    val dietType: DietType? = null,
) {
    /** Chips de la sección, en su orden de alta. */
    fun chips(section: ProfileSection): List<String> = when (section) {
        ProfileSection.ALLERGY -> allergies
        ProfileSection.INTOLERANCE -> intolerances
    }

    /** Marca «No tengo ninguna» de la sección. */
    fun isNone(section: ProfileSection): Boolean = when (section) {
        ProfileSection.ALLERGY -> allergiesNone
        ProfileSection.INTOLERANCE -> intolerancesNone
    }

    /** Copia con las chips de la sección (sin tocar el resto). */
    fun withChips(section: ProfileSection, chips: List<String>): UserProfile = when (section) {
        ProfileSection.ALLERGY -> copy(allergies = chips)
        ProfileSection.INTOLERANCE -> copy(intolerances = chips)
    }

    /** Copia con la marca «No tengo ninguna» de la sección (sin tocar el resto). */
    fun withNone(section: ProfileSection, none: Boolean): UserProfile = when (section) {
        ProfileSection.ALLERGY -> copy(allergiesNone = none)
        ProfileSection.INTOLERANCE -> copy(intolerancesNone = none)
    }

    companion object {
        /** Perfil de una app recién instalada: todo pendiente (caso límite D6). */
        val empty = UserProfile()
    }
}
