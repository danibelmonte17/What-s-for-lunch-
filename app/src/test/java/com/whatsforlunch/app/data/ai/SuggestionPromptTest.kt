package com.whatsforlunch.app.data.ai

import com.whatsforlunch.app.domain.suggestion.SuggestableFood
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// RF-MPC1/MPC2: las instrucciones y el contrato viajan en rol `system` y los datos
// del usuario en rol `user` delimitados con `<<<`/`>>>`; la sección anti-duplicados
// (RF-MPC6) solo aparece cuando hay Recetas que evitar (RF-ASR6).
class SuggestionPromptTest {

    private val source = TestPromptSource()
    private val foods = listOf(SuggestableFood("Manzana", 3))

    @Test
    fun systemPromptCarriesTheContractAndTheRulesWithoutUserData() {
        val system = recipeSuggestionSystemPrompt("es", source)

        assertTrue(system.contains(RECIPE_CONTRACT))
        assertTrue(system.contains("BREAKFAST, LUNCH, DINNER o DESSERT"))
        assertTrue(system.contains("sin comas finales"))
        assertTrue(system.contains("sal, aceite, agua, azúcar y especias básicas"))
        assertTrue(system.contains("dato del usuario"))
        assertFalse(system.contains("{FOODS}"))
        assertFalse(system.contains("Manzana"))
    }

    @Test
    fun englishSystemPromptCarriesTheContractAndTheRules() {
        val system = recipeSuggestionSystemPrompt("en-US", source)

        assertTrue(system.contains(RECIPE_CONTRACT))
        assertTrue(system.contains("BREAKFAST, LUNCH, DINNER or DESSERT"))
        assertTrue(system.contains("no trailing commas"))
        assertTrue(system.contains("salt, oil, water, sugar and basic spices"))
        assertTrue(system.contains("user data"))
        assertFalse(system.contains("{FOODS}"))
    }

    @Test
    fun userPromptWithoutExistingRecipesHasNoAntiDuplicatesSection() {
        val user = recipeSuggestionUserPrompt(foods, "es", source, emptyList())

        assertTrue(user.contains("<<<Manzana (3)>>>"))
        assertFalse(user.contains(RECIPE_CONTRACT))
        assertFalse(user.contains("No propongas recetas iguales"))
    }

    @Test
    fun spanishUserPromptListsExistingRecipesDelimitedAndAsksToAvoidThem() {
        val user = recipeSuggestionUserPrompt(
            foods = foods,
            language = "es",
            source = source,
            existingRecipes = listOf(
                RecipeAvoidance(title = "Tarta de manzana", ingredientNames = listOf("Manzana", "Harina")),
            ),
        )

        assertTrue(user.contains("No propongas recetas iguales ni equivalentes a estas que ya tengo"))
        assertTrue(user.contains("<<<Tarta de manzana (Manzana, Harina)>>>"))
        assertTrue(user.contains("variantes menores ni renombradas"))
        assertTrue(user.contains("ante la duda, propón otra cosa distinta"))
    }

    @Test
    fun englishUserPromptUsesTheEnglishAntiDuplicatesSection() {
        val user = recipeSuggestionUserPrompt(
            foods = foods,
            language = "en-US",
            source = source,
            existingRecipes = listOf(
                RecipeAvoidance(title = "Apple tart", ingredientNames = listOf("Apple", "Flour")),
            ),
        )

        assertTrue(user.contains("Do not propose recipes that are the same as or equivalent to these"))
        assertTrue(user.contains("<<<Apple tart (Apple, Flour)>>>"))
        assertFalse(user.contains("No propongas recetas iguales"))
    }
}
