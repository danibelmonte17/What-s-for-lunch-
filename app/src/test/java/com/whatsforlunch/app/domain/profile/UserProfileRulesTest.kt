package com.whatsforlunch.app.domain.profile

import com.whatsforlunch.app.domain.model.DietType
import com.whatsforlunch.app.domain.model.ProfileSection
import com.whatsforlunch.app.domain.model.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// Reglas puras del perfil (`crear-mi-perfil`, T1): validación de chips,
// exclusividad de «No tengo ninguna» (RD25/D3) y completitud por partes (RD26/D5);
// y `applyUpdate`, la composición que usa `save_profile` (RD27,
// `rellenar-perfil-con-el-pinche`).
class UserProfileRulesTest {

    @Test
    fun blankChipIsRejectedWithoutChangingTheProfile() {
        val profile = UserProfile(allergies = listOf("Frutos secos"))

        assertNull(UserProfileRules.addChip(profile, ProfileSection.ALLERGY, "   "))
        assertNull(UserProfileRules.addChip(profile, ProfileSection.ALLERGY, ""))
        assertEquals(listOf("Frutos secos"), profile.allergies)
    }

    @Test
    fun chipIsTrimmedAndAppendedInOrder() {
        val profile = UserProfile(allergies = listOf("Frutos secos"))

        val updated = UserProfileRules.addChip(profile, ProfileSection.ALLERGY, "  Lacteos  ")!!

        assertEquals(listOf("Frutos secos", "Lacteos"), updated.allergies)
    }

    @Test
    fun duplicateChipIgnoringCaseIsRejected() {
        val profile = UserProfile(allergies = listOf("Frutos secos"))

        assertNull(UserProfileRules.addChip(profile, ProfileSection.ALLERGY, "FRUTOS SECOS"))
        assertNull(UserProfileRules.addChip(profile, ProfileSection.ALLERGY, " frutos secos "))
    }

    @Test
    fun markingNoneClearsTheSectionChips() {
        val profile = UserProfile(allergies = listOf("Frutos secos"), intolerances = listOf("Diabetes"))

        val updated = UserProfileRules.setNone(profile, ProfileSection.ALLERGY, none = true)

        assertTrue(updated.allergiesNone)
        assertEquals(emptyList<String>(), updated.allergies)
        // La otra sección no se toca (RD25).
        assertEquals(listOf("Diabetes"), updated.intolerances)
        assertFalse(updated.intolerancesNone)
    }

    @Test
    fun addingAChipUnmarksNoneOnThatSection() {
        val profile = UserProfile(allergiesNone = true)

        val updated = UserProfileRules.addChip(profile, ProfileSection.ALLERGY, "Frutos secos")!!

        assertFalse(updated.allergiesNone)
        assertEquals(listOf("Frutos secos"), updated.allergies)
    }

    @Test
    fun unmarkingNoneLeavesTheSectionEmptyAndPending() {
        val profile = UserProfile(allergiesNone = true)

        val updated = UserProfileRules.setNone(profile, ProfileSection.ALLERGY, none = false)

        assertFalse(updated.allergiesNone)
        assertEquals(emptyList<String>(), updated.allergies)
        // La sección vuelve a estar pendiente (junto con el resto de partes del perfil).
        assertTrue(PendingProfilePart.ALLERGIES in UserProfileRules.pendingParts(updated))
        assertFalse(UserProfileRules.isComplete(updated))
    }

    @Test
    fun removingAChipKeepsNoneUntouched() {
        val profile = UserProfile(allergies = listOf("Frutos secos", "Marisco"))

        val updated = UserProfileRules.removeChip(profile, ProfileSection.ALLERGY, "frutos secos")

        assertEquals(listOf("Marisco"), updated.allergies)
        assertFalse(updated.allergiesNone)
    }

    @Test
    fun dietTypeIsSingleSelectionAndSubstitutesThePreviousOne() {
        val profile = UserProfile(dietType = DietType.VEGAN)

        val updated = UserProfileRules.setDietType(profile, DietType.OMNIVORE)

        assertEquals(DietType.OMNIVORE, updated.dietType)
    }

    @Test
    fun freshProfileIsPendingOnAllThreeParts() {
        val profile = UserProfile.empty

        assertEquals(
            listOf(PendingProfilePart.ALLERGIES, PendingProfilePart.INTOLERANCES, PendingProfilePart.DIET_TYPE),
            UserProfileRules.pendingParts(profile),
        )
        assertFalse(UserProfileRules.isComplete(profile))
    }

    @Test
    fun eachRestrictionSectionCompletesWithAChipOrWithNone() {
        val chipOnly = UserProfile(allergies = listOf("Frutos secos"))
        assertTrue(UserProfileRules.isSectionComplete(chipOnly, ProfileSection.ALLERGY))
        assertFalse(UserProfileRules.isSectionComplete(chipOnly, ProfileSection.INTOLERANCE))

        val noneOnly = UserProfile(intolerancesNone = true)
        assertFalse(UserProfileRules.isSectionComplete(noneOnly, ProfileSection.ALLERGY))
        assertTrue(UserProfileRules.isSectionComplete(noneOnly, ProfileSection.INTOLERANCE))
    }

    @Test
    fun profileIsCompleteOnlyWhenNoPartIsPending() {
        val almost = UserProfile(
            allergies = listOf("Frutos secos"),
            intolerancesNone = true,
            dietType = DietType.VEGAN,
        )
        assertTrue(UserProfileRules.isComplete(almost))

        // Sin tipo de alimentación sigue pendiente (D2: sin preselección).
        assertFalse(UserProfileRules.isComplete(almost.copy(dietType = null)))
        assertTrue(
            UserProfileRules.pendingParts(almost.copy(dietType = null)) == listOf(PendingProfilePart.DIET_TYPE),
        )
    }

    // RD27 (`rellenar-perfil-con-el-pinche`): `save_profile` compone estas reglas,
    // persiste solo con `Saved` y devuelve el código de error sin tocar nada con
    // `Invalid`.

    @Test
    fun applyUpdateWithoutAnyFieldIsInvalidArguments() {
        val profile = UserProfile(allergies = listOf("Frutos secos"))

        val outcome = UserProfileRules.applyUpdate(profile, ProfileUpdate())

        assertEquals(ProfileUpdateOutcome.Invalid(ProfileUpdateError.INVALID_ARGUMENTS), outcome)
        // El perfil no cambia.
        assertEquals(listOf("Frutos secos"), profile.allergies)
    }

    @Test
    fun applyUpdateRejectsBlankChipWithoutChangingTheProfile() {
        val profile = UserProfile(allergies = listOf("Frutos secos"))

        val outcome = UserProfileRules.applyUpdate(profile, ProfileUpdate(allergies = listOf("  ", "Lacteos")))

        assertEquals(ProfileUpdateOutcome.Invalid(ProfileUpdateError.INVALID_CHIP), outcome)
        assertEquals(listOf("Frutos secos"), profile.allergies)
    }

    @Test
    fun applyUpdateRejectsDuplicateChipsIgnoringCase() {
        val outcome = UserProfileRules.applyUpdate(
            UserProfile.empty,
            ProfileUpdate(allergies = listOf("Frutos secos", " frutos secos ")),
        )

        assertEquals(ProfileUpdateOutcome.Invalid(ProfileUpdateError.DUPLICATE_CHIP), outcome)
    }

    @Test
    fun applyUpdateRejectsChipsTogetherWithNoneOnTheSameSection() {
        // RD25/D3: «No tengo ninguna» es exclusiva con las chips.
        val outcome = UserProfileRules.applyUpdate(
            UserProfile.empty,
            ProfileUpdate(allergies = listOf("Frutos secos"), allergiesNone = true),
        )

        assertEquals(ProfileUpdateOutcome.Invalid(ProfileUpdateError.INVALID_CHIP), outcome)
    }

    @Test
    fun applyUpdateTouchesOnlyTheReceivedFieldsAndListsTheSavedParts() {
        val profile = UserProfile(allergies = listOf("Frutos secos"), intolerancesNone = true)

        val outcome = UserProfileRules.applyUpdate(
            profile,
            ProfileUpdate(intolerances = listOf("Diabetes"), dietType = DietType.VEGAN),
        )

        assertTrue(outcome is ProfileUpdateOutcome.Saved)
        val saved = outcome as ProfileUpdateOutcome.Saved
        // Alergias intacta (campo omitido); la marca de Intolerancias desmarcada.
        assertEquals(listOf("Frutos secos"), saved.updated.allergies)
        assertEquals(listOf("Diabetes"), saved.updated.intolerances)
        assertFalse(saved.updated.intolerancesNone)
        assertEquals(DietType.VEGAN, saved.updated.dietType)
        assertEquals(
            listOf(
                SavedProfilePart(PendingProfilePart.INTOLERANCES, chips = listOf("Diabetes")),
                SavedProfilePart(PendingProfilePart.DIET_TYPE, dietType = DietType.VEGAN),
            ),
            saved.savedParts,
        )
    }

    @Test
    fun applyUpdateOnACompleteSectionSubstitutesIt() {
        // D5: guardar sobre una sección ya completa la sustituye (correcciones).
        val profile = UserProfile(allergies = listOf("Frutos secos", "Marisco"))

        val outcome = UserProfileRules.applyUpdate(profile, ProfileUpdate(allergies = listOf("Lacteos")))

        val saved = outcome as ProfileUpdateOutcome.Saved
        assertEquals(listOf("Lacteos"), saved.updated.allergies)
        assertFalse(saved.updated.allergiesNone)
    }

    @Test
    fun applyUpdateWithNoneClearsTheSectionAndReportsIt() {
        val profile = UserProfile(allergies = listOf("Frutos secos"))

        val outcome = UserProfileRules.applyUpdate(profile, ProfileUpdate(allergiesNone = true))

        val saved = outcome as ProfileUpdateOutcome.Saved
        assertTrue(saved.updated.allergiesNone)
        assertEquals(emptyList<String>(), saved.updated.allergies)
        assertEquals(
            listOf(SavedProfilePart(PendingProfilePart.ALLERGIES, none = true)),
            saved.savedParts,
        )
    }

    // verify 4 de `feature_list.json`: al completar las tres partes con
    // `applyUpdate`, `pendingParts` queda vacío.
    @Test
    fun completingTheProfileWithApplyUpdateLeavesNothingPending() {
        val outcome = UserProfileRules.applyUpdate(
            UserProfile.empty,
            ProfileUpdate(
                allergies = listOf("Frutos secos"),
                intolerancesNone = true,
                dietType = DietType.VEGAN,
            ),
        )

        val saved = outcome as ProfileUpdateOutcome.Saved
        assertTrue(UserProfileRules.isComplete(saved.updated))
        assertEquals(emptyList<PendingProfilePart>(), UserProfileRules.pendingParts(saved.updated))
        assertEquals(
            listOf(
                PendingProfilePart.ALLERGIES,
                PendingProfilePart.INTOLERANCES,
                PendingProfilePart.DIET_TYPE,
            ),
            saved.savedParts.map { it.part },
        )
    }
}
