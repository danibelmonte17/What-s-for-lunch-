package com.whatsforlunch.app.data.ai

import com.whatsforlunch.app.data.repository.InMemoryPantryRepository
import com.whatsforlunch.app.data.repository.InMemoryRecipeRepository
import com.whatsforlunch.app.data.repository.InMemoryUserProfileRepository
import com.whatsforlunch.app.domain.model.DietType
import com.whatsforlunch.app.domain.model.UserProfile
import com.whatsforlunch.app.domain.profile.PendingProfilePart
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

// RD27 (`rellenar-perfil-con-el-pinche`): el executor de `save_profile` valida con
// `UserProfileRules`, persiste en el puerto del perfil con caché por turno y
// devuelve `saved`/`pending` al modelo; lo inválido viaja como `error` sin
// persistir nada.
class OpenAiCompatiblePincheChatClientTest {

    private val profileRepository = InMemoryUserProfileRepository()
    private val state = PincheToolTurnState()
    private val client = OpenAiCompatiblePincheChatClient(
        client = OkHttpClient(),
        aiKeyProvider = object : AiKeyProvider {
            override fun apiKey(): String? = null
        },
        pantryRepository = InMemoryPantryRepository(),
        recipeRepository = InMemoryRecipeRepository(),
        userProfileRepository = profileRepository,
        promptSource = TestPromptSource(),
    )

    @Test
    fun saveProfilePersistsValidDataAndReportsWhatIsPending() = runTest {
        val result = client.executeSaveProfile(
            """{"allergies":["Frutos secos"],"allergies_none":false}""",
            state,
        )

        val json = JSONObject(result)
        val saved = json.getJSONObject("saved")
        assertEquals("Frutos secos", saved.getJSONArray("allergies").getString(0))
        assertFalse(saved.getBoolean("allergies_none"))
        val pending = json.getJSONArray("pending")
        assertEquals(
            listOf("INTOLERANCES", "DIET_TYPE"),
            (0 until pending.length()).map { pending.getString(it) },
        )
        assertEquals(
            UserProfile(allergies = listOf("Frutos secos")),
            profileRepository.observeProfile().first(),
        )
    }

    @Test
    fun saveProfileCachesTheProfilePerTurnAndAccumulatesEverySavedPart() = runTest {
        client.executeSaveProfile("""{"allergies":["Frutos secos"]}""", state)
        val second = client.executeSaveProfile(
            """{"intolerances_none":true,"diet_type":"Vegana"}""",
            state,
        )

        // El resultado de la segunda llamada refleja el estado final completo.
        assertEquals(
            emptyList<String>(),
            (0 until JSONObject(second).getJSONArray("pending").length())
                .map { JSONObject(second).getJSONArray("pending").getString(it) },
        )
        // Caché por turno: la segunda parte parte de la primera y todo persiste.
        assertEquals(
            UserProfile(
                allergies = listOf("Frutos secos"),
                intolerancesNone = true,
                dietType = DietType.VEGAN,
            ),
            profileRepository.observeProfile().first(),
        )
        assertEquals(
            listOf(
                PendingProfilePart.ALLERGIES,
                PendingProfilePart.INTOLERANCES,
                PendingProfilePart.DIET_TYPE,
            ),
            state.savedProfileParts()?.map { it.part },
        )
    }

    @Test
    fun invalidSaveProfileReturnsTheErrorAndPersistsNothing() = runTest {
        val blankChip = client.executeSaveProfile("""{"allergies":["  "]}""", state)
        val duplicate = client.executeSaveProfile(
            """{"allergies":["Frutos secos","FRUTOS SECOS"]}""",
            state,
        )
        val exclusivity = client.executeSaveProfile(
            """{"intolerances":["Diabetes"],"intolerances_none":true}""",
            state,
        )

        assertEquals("invalid_chip", JSONObject(blankChip).getString("error"))
        assertEquals("duplicate_chip", JSONObject(duplicate).getString("error"))
        assertEquals("invalid_chip", JSONObject(exclusivity).getString("error"))
        assertEquals(UserProfile.empty, profileRepository.observeProfile().first())
        // Nada se acumula para el resumen del turno.
        assertNull(state.savedProfileParts())
    }

    @Test
    fun unknownDietTypeReturnsItsOwnErrorWithoutPersisting() = runTest {
        val result = client.executeSaveProfile("""{"diet_type":"Crudivora"}""", state)

        assertEquals("unknown_diet_type", JSONObject(result).getString("error"))
        assertEquals(UserProfile.empty, profileRepository.observeProfile().first())
        assertNull(state.profileSnapshot)
        assertNull(state.savedProfileParts())
    }
}
