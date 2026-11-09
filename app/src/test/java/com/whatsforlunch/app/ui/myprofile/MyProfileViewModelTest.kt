package com.whatsforlunch.app.ui.myprofile

import com.whatsforlunch.app.data.repository.InMemoryUserProfileRepository
import com.whatsforlunch.app.domain.model.DietType
import com.whatsforlunch.app.domain.model.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

// Estado y mutaciones de «Mi perfil» (`crear-mi-perfil`, T3): todo pasa por
// `UserProfileRules` y se persiste en el repositorio.
@OptIn(ExperimentalCoroutinesApi::class)
class MyProfileViewModelTest {

    @Before
    fun setMainDispatcher() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    @Test
    fun addingChipsPersistsThemPerSectionAndClearsTheField() = runTest {
        val repository = InMemoryUserProfileRepository()
        val viewModel = MyProfileViewModel(repository)

        viewModel.onAllergyInputChange("Frutos secos")
        viewModel.addAllergy()
        viewModel.onIntoleranceInputChange("Diabetes")
        viewModel.addIntolerance()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(listOf("Frutos secos"), state.profile.allergies)
        assertEquals(listOf("Diabetes"), state.profile.intolerances)
        assertEquals("", state.allergyInput)
        assertEquals("", state.intoleranceInput)
        assertEquals(state.profile, runBlocking { repository.observeProfile().first() })
    }

    @Test
    fun blankOrDuplicateChipsAreRejectedAndKeepTheInput() = runTest {
        val viewModel = MyProfileViewModel(InMemoryUserProfileRepository())

        viewModel.onAllergyInputChange("Frutos secos")
        viewModel.addAllergy()
        advanceUntilIdle()

        viewModel.onAllergyInputChange("  ")
        viewModel.addAllergy()
        advanceUntilIdle()
        assertEquals(listOf("Frutos secos"), viewModel.uiState.value.profile.allergies)
        assertEquals("  ", viewModel.uiState.value.allergyInput)

        viewModel.onAllergyInputChange("FRUTOS SECOS")
        viewModel.addAllergy()
        advanceUntilIdle()
        assertEquals(listOf("Frutos secos"), viewModel.uiState.value.profile.allergies)
        assertEquals(1, viewModel.uiState.value.profile.allergies.size)
    }

    @Test
    fun markingNoneClearsTheSectionAndAddingAChipUnmarksIt() = runTest {
        val viewModel = MyProfileViewModel(InMemoryUserProfileRepository())
        viewModel.onAllergyInputChange("Frutos secos")
        viewModel.addAllergy()
        advanceUntilIdle()

        viewModel.setAllergyNone(true)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.profile.allergiesNone)
        assertEquals(emptyList<String>(), viewModel.uiState.value.profile.allergies)

        viewModel.onAllergyInputChange("Marisco")
        viewModel.addAllergy()
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.profile.allergiesNone)
        assertEquals(listOf("Marisco"), viewModel.uiState.value.profile.allergies)
    }

    @Test
    fun removingAChipPersistsTheRemainder() = runTest {
        val viewModel = MyProfileViewModel(InMemoryUserProfileRepository())
        viewModel.onIntoleranceInputChange("Diabetes")
        viewModel.addIntolerance()
        viewModel.onIntoleranceInputChange("Celiaquía")
        viewModel.addIntolerance()
        advanceUntilIdle()

        viewModel.removeIntolerance("Diabetes")
        advanceUntilIdle()

        assertEquals(listOf("Celiaquía"), viewModel.uiState.value.profile.intolerances)
    }

    @Test
    fun dietTypeIsSingleSelectionAndKeepsTheLastChoice() = runTest {
        val repository = InMemoryUserProfileRepository()
        val viewModel = MyProfileViewModel(repository)

        viewModel.selectDietType(DietType.VEGAN)
        advanceUntilIdle()
        viewModel.selectDietType(DietType.OMNIVORE)
        advanceUntilIdle()

        assertEquals(DietType.OMNIVORE, viewModel.uiState.value.profile.dietType)
        assertEquals(DietType.OMNIVORE, runBlocking { repository.observeProfile().first() }.dietType)
    }

    @Test
    fun freshProfileStartsWithEverythingPendingAndNothingSelected() = runTest {
        val viewModel = MyProfileViewModel(InMemoryUserProfileRepository())
        advanceUntilIdle()

        val profile = viewModel.uiState.value.profile
        assertEquals(UserProfile.empty, profile)
        assertEquals(null, profile.dietType)
        assertEquals("", viewModel.uiState.value.allergyInput)
        assertEquals("", viewModel.uiState.value.intoleranceInput)
    }
}
