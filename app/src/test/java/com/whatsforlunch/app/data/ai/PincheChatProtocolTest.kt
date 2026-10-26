package com.whatsforlunch.app.data.ai

import com.whatsforlunch.app.domain.model.MealMoment
import com.whatsforlunch.app.domain.model.PantryItem
import com.whatsforlunch.app.domain.model.Recipe
import com.whatsforlunch.app.domain.model.RecipeIngredient
import com.whatsforlunch.app.domain.model.RecipeStep
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PincheChatProtocolTest {

    @Test
    fun toolsExposeOnlyTheTwoReadOnlyFunctionsWithoutParameters() {
        val tools = PincheChatProtocol.toolsJson()

        val names = (0 until tools.length()).map {
            tools.getJSONObject(it).getJSONObject("function").getString("name")
        }
        assertEquals(listOf("get_pantry", "get_my_recipes"), names)
        val parameters = tools.getJSONObject(0).getJSONObject("function").getJSONObject("parameters")
        assertEquals("object", parameters.getString("type"))
        assertEquals(0, parameters.getJSONObject("properties").length())
        assertEquals(false, parameters.getBoolean("additionalProperties"))
    }

    @Test
    fun toolCallsReadIdAndNameAndIgnoreArguments() {
        val message = JSONObject(
            """
            {"tool_calls":[
              {"id":"call_1","type":"function","function":{"name":"get_pantry","arguments":"not json"}},
              {"id":"call_2","type":"function","function":{"name":"get_my_recipes","arguments":"{}"}}
            ]}
            """.trimIndent(),
        )

        val calls = PincheChatProtocol.toolCalls(message)

        assertEquals(
            listOf("call_1" to "get_pantry", "call_2" to "get_my_recipes"),
            calls.map { it.id to it.name },
        )
    }

    @Test
    fun pantryResultCapsAtOneHundredItemsAndKeepsNameAndQuantity() {
        val items = (1..120).map { index -> PantryItem("$index", "Alimento $index", index) }

        val result = JSONObject(PincheChatProtocol.pantryToolResult(items))
        val array = result.getJSONArray("items")

        assertEquals(PincheChatProtocol.MAX_PANTRY_ITEMS, array.length())
        assertEquals("Alimento 1", array.getJSONObject(0).getString("name"))
        assertEquals(1, array.getJSONObject(0).getInt("quantity"))
        assertEquals("Alimento 100", array.getJSONObject(99).getString("name"))
    }

    @Test
    fun pantryResultKeepsNullQuantity() {
        val result = JSONObject(PincheChatProtocol.pantryToolResult(listOf(PantryItem("1", "Sal"))))

        assertTrue(result.getJSONArray("items").getJSONObject(0).isNull("quantity"))
    }

    @Test
    fun recipesResultCapsRecipesAndIngredientsAndOmitsStepsAndQuantities() {
        val recipes = (1..60).map { index ->
            Recipe(
                id = "$index",
                title = "Receta $index",
                mealMoment = MealMoment.DINNER,
                ingredients = (1..30).map { RecipeIngredient(name = "Ingrediente $it", quantity = 2.0) },
                steps = listOf(RecipeStep(text = "Paso secreto")),
            )
        }

        val result = JSONObject(PincheChatProtocol.recipesToolResult(recipes))
        val array = result.getJSONArray("recipes")

        assertEquals(PincheChatProtocol.MAX_RECIPES, array.length())
        val first = array.getJSONObject(0)
        assertEquals("Receta 1", first.getString("title"))
        assertEquals("DINNER", first.getString("moment"))
        assertEquals(PincheChatProtocol.MAX_RECIPE_INGREDIENTS, first.getJSONArray("ingredients").length())
        assertTrue(!first.has("steps"))
        assertTrue(!first.has("photoPath"))
    }

    @Test
    fun parseReplySeparatesTextAndUsesTheFirstRecipeBlockOnly() {
        val content = """
            Te propongo esta receta.

            ```receta
            {"s":[{"t":"Arroz con tomate","mm":"LUNCH","i":[{"n":"Arroz"},{"n":"Tomate"}],"p":[{"t":"Cocina"}]},
                  {"t":"Otra receta","i":[{"n":"Tomate"}],"p":[{"t":"Cocina"}]}]}
            ```
            ```receta
            {"s":[{"t":"Ignorada","i":[{"n":"Tomate"}],"p":[{"t":"Cocina"}]}]}
            ```
        """.trimIndent()

        val reply = PincheChatProtocol.parseReply(content)

        assertEquals("Te propongo esta receta.", reply.text)
        assertEquals("Arroz con tomate", reply.proposal?.title)
    }

    @Test
    fun parseReplyFallsBackToTheFirstJsonObjectWithoutABlock() {
        val content = "Toma esta receta. " +
            "{\"s\":[{\"t\":\"Tortilla\",\"i\":[{\"n\":\"Huevo\"}],\"p\":[{\"t\":\"Cuaja\"}]}]}"

        val reply = PincheChatProtocol.parseReply(content)

        assertEquals("Toma esta receta.", reply.text)
        assertEquals("Tortilla", reply.proposal?.title)
    }

    @Test
    fun invalidProposalLeavesOnlyTheText() {
        val content = "No puedo proponerte una receta.\n```receta\n{\"s\":[{\"t\":\"Sin pasos\",\"i\":[]}]}\n```"

        val reply = PincheChatProtocol.parseReply(content)

        assertEquals("No puedo proponerte una receta.", reply.text)
        assertNull(reply.proposal)
    }

    @Test
    fun plainTextWithoutJsonKeepsTheWholeText() {
        val reply = PincheChatProtocol.parseReply(" Para que no se pegue, remueve al principio. ")

        assertEquals("Para que no se pegue, remueve al principio.", reply.text)
        assertNull(reply.proposal)
    }

    @Test
    fun systemPromptIsLocalizedAndAsksForTheRecipeBlock() {
        val spanish = PincheChatProtocol.systemPrompt("es")
        val english = PincheChatProtocol.systemPrompt("en")

        assertTrue(spanish.contains("receta"))
        assertTrue(spanish.contains("get_pantry"))
        assertTrue(english.contains("fenced block"))
        assertTrue(english.contains("get_my_recipes"))
    }
}
