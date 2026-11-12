package com.whatsforlunch.app.data.ai

import com.whatsforlunch.app.domain.model.DietType
import com.whatsforlunch.app.domain.model.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PromptAssetsTest {

    private val source = TestPromptSource()
    private val languages = listOf("es", "en")
    private val names = listOf(
        "pinche_system",
        "pinche_profile",
        "suggestion",
        "suggestion_avoid",
        "pantry_scan",
        "dish_photo",
    )

    @Test
    fun everyPromptExistsForSpanishAndEnglish() {
        names.forEach { name ->
            languages.forEach { language ->
                assertTrue("$name ($language) is empty", source.template(name, language).isNotBlank())
            }
        }
    }

    @Test
    fun renderedPromptsNeverKeepTheContractPlaceholder() {
        names.forEach { name ->
            languages.forEach { language ->
                val rendered = renderPrompt(source.template(name, language))
                assertFalse("$name ($language) keeps the placeholder", rendered.contains(CONTRACT_PLACEHOLDER))
            }
        }
    }

    @Test
    fun suggestionPromptsKeepTheirTokensAndTheSharedContract() {
        assertTrue(source.suggestionPrompt("es").contains("{FOODS}"))
        assertTrue(source.suggestionPrompt("en").contains("{FOODS}"))
        assertTrue(source.suggestionPrompt("es").contains(RECIPE_CONTRACT))
        assertTrue(source.suggestionPrompt("en").contains(RECIPE_CONTRACT))
        assertTrue(source.suggestionAvoidPrompt("es").contains("{RECIPES}"))
        assertTrue(source.suggestionAvoidPrompt("en").contains("{RECIPES}"))
        assertTrue(source.suggestionPrompt("es").contains("cocinero experto"))
        assertTrue(source.suggestionPrompt("en").contains("expert cook"))
        assertTrue(source.suggestionAvoidPrompt("es").contains("No propongas recetas iguales"))
        assertTrue(source.suggestionAvoidPrompt("en").contains("Do not propose recipes"))
    }

    @Test
    fun scanPromptsAreLocalized() {
        assertTrue(source.pantryScanPrompt("es").contains("Analiza la foto"))
        assertTrue(source.pantryScanPrompt("en").contains("Analyze the photo"))
    }

    // `crear-mi-perfil` (D6): el bloque de perfil rellena tipo de alimentación y
    // restricciones, con «No tengo ninguna» como «ninguna»/«none» y el texto libre
    // del usuario literal en ambos idiomas; los valores van delimitados (RF-MPC1).
    @Test
    fun profilePromptFillsDietAndRestrictions() {
        val profile = UserProfile(
            allergies = listOf("Frutos secos"),
            intolerances = listOf("Diabetes"),
            dietType = DietType.VEGAN,
        )
        val es = renderProfilePrompt(source.template("pinche_profile", "es"), profile, "es")
        assertTrue(es.contains("Tipo de alimentación: <<<Vegana>>>"))
        assertTrue(es.contains("Alergias: <<<Frutos secos>>>"))
        assertTrue(es.contains("Intolerancias o enfermedades que limitan la dieta: <<<Diabetes>>>"))
        assertFalse(es.contains(DIET_PLACEHOLDER))
        assertFalse(es.contains(ALLERGIES_PLACEHOLDER))
        assertFalse(es.contains(INTOLERANCES_PLACEHOLDER))

        val en = renderProfilePrompt(source.template("pinche_profile", "en"), profile, "en")
        assertTrue(en.contains("Diet type: <<<Vegan>>>"))
        // El texto libre del usuario no se traduce.
        assertTrue(en.contains("Allergies: <<<Frutos secos>>>"))
        assertTrue(en.contains("Intolerances or medical conditions that limit the diet: <<<Diabetes>>>"))

        val noneProfile = UserProfile(allergiesNone = true, intolerancesNone = true, dietType = DietType.OMNIVORE)
        val noneEs = renderProfilePrompt(source.template("pinche_profile", "es"), noneProfile, "es")
        assertTrue(noneEs.contains("Tipo de alimentación: <<<Omnívora>>>"))
        assertTrue(noneEs.contains("Alergias: <<<ninguna>>>"))
        assertTrue(noneEs.contains("Intolerancias o enfermedades que limitan la dieta: <<<ninguna>>>"))
        val noneEn = renderProfilePrompt(source.template("pinche_profile", "en"), noneProfile, "en")
        assertTrue(noneEn.contains("Allergies: <<<none>>>"))
        assertTrue(noneEn.contains("Diet type: <<<Omnivore>>>"))
    }

    // `rellenar-perfil-con-el-pinche` (D4): el system prompt describe tres
    // funciones —las dos de lectura y `save_profile` de escritura—.
    @Test
    fun pincheSystemPromptsDescribeThreeFunctionsWithSaveProfile() {
        val es = source.pincheSystemPrompt("es")
        assertTrue(es.contains("Tienes tres funciones"))
        assertTrue(es.contains("save_profile"))
        assertTrue(es.contains("de escritura"))
        val en = source.pincheSystemPrompt("en")
        assertTrue(en.contains("You have three functions"))
        assertTrue(en.contains("save_profile"))
        assertTrue(en.contains("a write function"))
    }

    // D1/D6: las reglas de entrevista viven en el bloque de perfil (es/en) con el
    // vocabulario cerrado del tipo de alimentación (D8) y las chips sin traducir.
    @Test
    fun profilePromptsCarryTheInterviewRulesInBothLanguages() {
        val es = source.pincheProfilePrompt("es")
        assertTrue(es.contains("no propongas todavía"))
        assertTrue(es.contains("una sola parte pendiente por mensaje"))
        assertTrue(es.contains("save_profile"))
        assertTrue(es.contains("sin traducir"))
        assertTrue(es.contains("Omnívora, Pescetariana, Vegetariana, Vegana o Cetogénica"))
        assertTrue(es.contains("sin entrevistar ni guardar nada"))
        assertTrue(es.contains("«sin declarar»"))
        assertTrue(es.contains("«sin elegir»"))

        val en = source.pincheProfilePrompt("en")
        assertTrue(en.contains("do not propose yet"))
        assertTrue(en.contains("one pending part per message"))
        assertTrue(en.contains("save_profile"))
        assertTrue(en.contains("untranslated"))
        assertTrue(en.contains("Omnivore, Pescetarian, Vegetarian, Vegan or Ketogenic"))
        assertTrue(en.contains("without interviewing or saving anything"))
        assertTrue(en.contains("\"not declared\""))
        assertTrue(en.contains("\"not chosen\""))
    }

    // El vocabulario de la tool `save_profile` es el mismo que el del bloque de
    // perfil: no pueden desincronizarse.
    @Test
    fun dietVocabularyMatchesTheNamesOfTheProfilePrompt() {
        DIET_TYPE_ENUM_VALUES.forEach { name ->
            assertTrue("$name ausente del vocabulario", name.lowercase() in DIET_TYPE_VOCABULARY.keys)
        }
        assertEquals(10, DIET_TYPE_ENUM_VALUES.size)
        assertEquals(DietType.VEGAN, DIET_TYPE_VOCABULARY["vegana"])
        assertEquals(DietType.KETOGENIC, DIET_TYPE_VOCABULARY["ketogenic"])
    }

    @Test
    fun dishPhotoPromptsFillDishAndStory() {
        assertEquals(
            "Fotografía gastronómica apetitosa del plato \"<<<Tortilla>>>\". Luz natural, " +
                "primer plano apetitoso, fondo neutro, sin texto, sin marcas de agua y sin personas " +
                "ni manos; el plato debe reflejar los ingredientes del título y de la historia. " +
                "<<<Con cebolla.>>>",
            renderDishPhotoPrompt("Tortilla", "Con cebolla", "es", source),
        )
        assertEquals(
            "Appetizing food photography of the dish \"<<<Omelette>>>\". Natural light, " +
                "appetizing close-up, neutral background, no text, no watermarks and no people " +
                "or hands; the dish should reflect the ingredients from the title and the story. " +
                "<<<With onion.>>>",
            renderDishPhotoPrompt("Omelette", "With onion", "en", source),
        )
        assertEquals(
            "Fotografía gastronómica apetitosa del plato \"<<<Tortilla>>>\". Luz natural, " +
                "primer plano apetitoso, fondo neutro, sin texto, sin marcas de agua y sin personas " +
                "ni manos; el plato debe reflejar los ingredientes del título y de la historia.",
            renderDishPhotoPrompt("Tortilla", " ", "es", source),
        )
    }

    // RF-MPC1: los pares es/en tienen los mismos marcadores y las secciones
    // system/user coinciden; los prompts con datos del usuario llevan la nota de
    // «dato del usuario» (no en dish_photo: es un prompt de imagen y la meta-
    // instrucción contaminaría el resultado).
    @Test
    fun promptPairsKeepTheSameMarkersAndSectionsInBothLanguages() {
        val placeholders = Regex("""\{[A-Z_]+\}""")
        names.forEach { name ->
            val es = source.template(name, "es")
            val en = source.template(name, "en")
            assertEquals(
                "$name: marcadores distintos es/en",
                placeholders.findAll(es).map { it.value }.toSet(),
                placeholders.findAll(en).map { it.value }.toSet(),
            )
            assertEquals(
                "$name: secciones system/user distintas es/en",
                es.contains(USER_SECTION_MARKER),
                en.contains(USER_SECTION_MARKER),
            )
        }
        val notes = mapOf("es" to "dato del usuario", "en" to "user data")
        listOf("suggestion", "pantry_scan", "pinche_profile").forEach { name ->
            languages.forEach { language ->
                val template = source.template(name, language)
                assertTrue("$name ($language) sin la nota de dato del usuario", template.contains(notes.getValue(language)))
            }
        }
    }
}
