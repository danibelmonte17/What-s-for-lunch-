package com.whatsforlunch.app.data.ai

import com.whatsforlunch.app.domain.suggestion.SuggestableFood
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// T2 (D2/RF-ASR6): la sección anti-duplicados solo aparece cuando hay Recetas que
// evitar y lo hace en el idioma de la app, sin cambiar el JSON compacto de D1.
class SuggestionPromptTest {

    private val foods = listOf(SuggestableFood("Manzana", 3))

    @Test
    fun promptWithoutExistingRecipesHasNoAntiDuplicatesSection() {
        val prompt = recipeSuggestionPrompt(foods, "es", emptyList())

        assertTrue(prompt.contains("Manzana (3)"))
        assertTrue(prompt.contains("\"t\":\"título\""))
        assertFalse(prompt.contains("No propongas recetas iguales"))
    }

    @Test
    fun spanishPromptListsExistingRecipesAndAsksToAvoidThem() {
        val prompt = recipeSuggestionPrompt(
            foods = foods,
            language = "es",
            existingRecipes = listOf(
                RecipeAvoidance(title = "Tarta de manzana", ingredientNames = listOf("Manzana", "Harina")),
            ),
        )

        assertTrue(prompt.contains("No propongas recetas iguales ni equivalentes a estas que ya tengo"))
        assertTrue(prompt.contains("Tarta de manzana (Manzana, Harina)"))
        assertTrue(prompt.contains("\"p\":[{\"t\":\"texto del paso\",\"m\":10}]"))
    }

    @Test
    fun englishPromptUsesTheEnglishAntiDuplicatesSection() {
        val prompt = recipeSuggestionPrompt(
            foods = foods,
            language = "en-US",
            existingRecipes = listOf(
                RecipeAvoidance(title = "Apple tart", ingredientNames = listOf("Apple", "Flour")),
            ),
        )

        assertTrue(prompt.contains("Do not propose recipes that are the same as or equivalent to these"))
        assertTrue(prompt.contains("Apple tart (Apple, Flour)"))
        assertFalse(prompt.contains("No propongas recetas iguales"))
    }
}
