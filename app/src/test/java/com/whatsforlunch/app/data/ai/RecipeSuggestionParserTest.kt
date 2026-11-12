package com.whatsforlunch.app.data.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class RecipeSuggestionParserTest {

    // El contrato ya no tiene «cm» (cocción) ni «o» (opcionalidad, D8): las claves
    // legadas que sigan llegando se ignoran sin romper el parseo.
    private val compact = """
        {"s":[
          {"t":"Tarta de manzana","st":"Clásica","mm":"DESSERT","pm":10,"cm":20,"df":"EASY","bs":4,"kcal":320,
           "i":[{"n":"Manzana","q":3,"u":"ud","o":false},{"n":"Nata","q":"200","u":"ml"}],
           "p":[{"t":"Pela las manzanas","m":10},{"t":"Hornea"}]}
        ]}
    """.trimIndent()

    @Test
    fun parsesTheCompactRecipeJson() {
        val proposals = RecipeSuggestionParser.parse(compact)

        val proposal = proposals?.single()
        assertEquals("Tarta de manzana", proposal?.title)
        assertEquals("Clásica", proposal?.story)
        assertEquals("DESSERT", proposal?.mealMoment)
        assertEquals(10.0, proposal?.prepMinutes)
        assertEquals("EASY", proposal?.difficulty)
        assertEquals(4.0, proposal?.baseServings)
        assertEquals(320.0, proposal?.calories)
        assertEquals(listOf("Manzana", "Nata"), proposal?.ingredients?.map { it.name })
        assertEquals(200.0, proposal?.ingredients?.last()?.quantity)
        assertEquals(listOf("Pela las manzanas", "Hornea"), proposal?.steps?.map { it.text })
        assertEquals(10.0, proposal?.steps?.first()?.timerMinutes)
        assertNull(proposal?.steps?.last()?.timerMinutes)
    }

    @Test
    fun toleratesMarkdownFencesAndSurroundingText() {
        val content = "Aquí tienes:\n```json\n$compact\n```\nEspero que ayude."

        val proposals = RecipeSuggestionParser.parse(content)

        assertEquals("Tarta de manzana", proposals?.single()?.title)
    }

    @Test
    fun recoversOnlyTheFirstJsonObject() {
        val proposals = RecipeSuggestionParser.parse("""{"s":[{"t":"Uno"}]} {"s":[{"t":"Dos"}]}""")

        assertEquals(listOf("Uno"), proposals?.map { it.title })
    }

    @Test
    fun missingObjectIsAParseFailure() {
        assertNull(RecipeSuggestionParser.parse("No hay ninguna receta."))
        assertNull(RecipeSuggestionParser.parse(""))
        assertNull(RecipeSuggestionParser.parse("{\"s\": ["))
    }

    @Test
    fun missingProposalArrayIsAnEmptySuccessfulAnswer() {
        assertEquals(emptyList<Any>(), RecipeSuggestionParser.parse("""{"otra":"cosa"}"""))
        assertEquals(emptyList<Any>(), RecipeSuggestionParser.parse("{}"))
    }

    @Test
    fun keepsMalformedValuesForDomainNormalization() {
        val proposals = RecipeSuggestionParser.parse(
            """{"s":[{"t":7,"st":"ok","o":true,"i":[{"n":"Sal","q":"mucha","o":"sí"},7],"p":[{"t":"Paso","m":-3}]}]}""",
        )

        val proposal = proposals?.single()
        assertNull(proposal?.title)
        assertNull(proposal?.ingredients?.single()?.quantity)
        assertEquals(-3.0, proposal?.steps?.single()?.timerMinutes)
    }
}
