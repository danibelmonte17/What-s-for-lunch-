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
// exclusividad de «No tengo ninguna» (RD25/D3) y completitud por partes (RD26/D5).
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
}
