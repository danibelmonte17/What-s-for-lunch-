package com.whatsforlunch.app.data.ai

import com.whatsforlunch.app.domain.model.DietType
import com.whatsforlunch.app.domain.model.MealMoment
import com.whatsforlunch.app.domain.model.PantryItem
import com.whatsforlunch.app.domain.model.Recipe
import com.whatsforlunch.app.domain.model.RecipeIngredient
import com.whatsforlunch.app.domain.model.RecipeStep
import com.whatsforlunch.app.domain.profile.PendingProfilePart
import com.whatsforlunch.app.domain.profile.SavedProfilePart
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PincheChatProtocolTest {

    @Test
    fun toolsExposeTheTwoReadOnlyFunctionsWithoutParameters() {
        val tools = PincheChatProtocol.toolsJson()

        val names = (0 until tools.length()).map {
            tools.getJSONObject(it).getJSONObject("function").getString("name")
        }
        assertEquals(listOf("get_pantry", "get_my_recipes", "save_profile"), names)
        val parameters = tools.getJSONObject(0).getJSONObject("function").getJSONObject("parameters")
        assertEquals("object", parameters.getString("type"))
        assertEquals(0, parameters.getJSONObject("properties").length())
        assertEquals(false, parameters.getBoolean("additionalProperties"))
        val recipes = tools.getJSONObject(1).getJSONObject("function").getJSONObject("parameters")
        assertEquals(0, recipes.getJSONObject("properties").length())
    }

    // RD27 (`rellenar-perfil-con-el-pinche`): la tercera tool es `save_profile`,
    // con los cinco campos opcionales y el vocabulario cerrado del tipo (D8).
    @Test
    fun saveProfileToolExposesFiveOptionalFieldsWithTheClosedDietVocabulary() {
        val tools = PincheChatProtocol.toolsJson()

        val saveProfile = tools.getJSONObject(2).getJSONObject("function")
        assertEquals("save_profile", saveProfile.getString("name"))
        assertTrue(saveProfile.getString("description").contains("Mi perfil"))
        assertTrue(saveProfile.getString("description").contains("only call it when the user states their data"))
        val parameters = saveProfile.getJSONObject("parameters")
        val properties = parameters.getJSONObject("properties")
        assertEquals(
            setOf("allergies", "allergies_none", "intolerances", "intolerances_none", "diet_type"),
            properties.keys().asSequence().toSet(),
        )
        assertEquals(false, parameters.getBoolean("additionalProperties"))
        assertEquals("array", properties.getJSONObject("allergies").getString("type"))
        assertEquals("string", properties.getJSONObject("allergies").getJSONObject("items").getString("type"))
        assertEquals("boolean", properties.getJSONObject("allergies_none").getString("type"))
        val dietEnum = properties.getJSONObject("diet_type").getJSONArray("enum")
        val values = (0 until dietEnum.length()).map { dietEnum.getString(it) }
        assertEquals(10, values.size)
        // Los diez nombres es/en del vocabulario del bloque de perfil.
        listOf("Omnívora", "Omnivore", "Pescetariana", "Pescetarian", "Vegetariana", "Vegetarian", "Vegana", "Vegan", "Cetogénica", "Ketogenic")
            .forEach { assertTrue("$it ausente", it in values) }
    }

    @Test
    fun toolCallsReadIdNameAndArguments() {
        val message = JSONObject(
            """
            {"tool_calls":[
              {"id":"call_1","type":"function","function":{"name":"get_pantry","arguments":"not json"}},
              {"id":"call_2","type":"function","function":{"name":"save_profile","arguments":"{\"allergies\":[\"Frutos secos\"]}"}}
            ]}
            """.trimIndent(),
        )

        val calls = PincheChatProtocol.toolCalls(message)

        assertEquals(
            listOf("call_1" to "get_pantry", "call_2" to "save_profile"),
            calls.map { it.id to it.name },
        )
        assertEquals("not json", calls[0].arguments)
        assertEquals("""{"allergies":["Frutos secos"]}""", calls[1].arguments)
        // Sin campo `arguments` queda vacío (las tools de lectura lo ignoran).
        val withoutArguments = PincheChatProtocol.toolCalls(
            JSONObject("""{"tool_calls":[{"id":"c","function":{"name":"get_pantry"}}]}"""),
        )
        assertEquals("", withoutArguments.single().arguments)
    }

    @Test
    fun parseSaveProfileArgsAcceptsOmittedFieldsAndTheClosedDietVocabulary() {
        val update = PincheChatProtocol.parseSaveProfileArgs(
            """{"allergies":["Frutos secos"],"diet_type":"Vegana"}""",
        )!!

        assertEquals(listOf("Frutos secos"), update.allergies)
        // Campos omitidos = sin tocar (null).
        assertNull(update.allergiesNone)
        assertNull(update.intolerances)
        assertNull(update.intolerancesNone)
        assertEquals(DietType.VEGAN, update.dietType)

        // Vocabulario en inglés, en minúsculas y con mayúsculas iniciales (D8).
        assertEquals(DietType.KETOGENIC, PincheChatProtocol.parseSaveProfileArgs("""{"diet_type":"Ketogenic"}""")!!.dietType)
        assertEquals(DietType.OMNIVORE, PincheChatProtocol.parseSaveProfileArgs("""{"diet_type":"omnívora"}""")!!.dietType)
        assertEquals(DietType.VEGETARIAN, PincheChatProtocol.parseSaveProfileArgs("""{"diet_type":"VEGETARIAN"}""")!!.dietType)
        // Sin campos: el parseo es válido y quien lo rechaza es `applyUpdate`.
        assertNotNull(PincheChatProtocol.parseSaveProfileArgs("{}"))
    }

    @Test
    fun parseSaveProfileArgsRejectsUnknownDietTypeAndMalformedArguments() {
        // D8: fuera del vocabulario cerrado → `unknown_diet_type`.
        assertNull(PincheChatProtocol.parseSaveProfileArgs("""{"diet_type":"Crudivora"}"""))
        assertEquals("unknown_diet_type", PincheChatProtocol.saveProfileArgsError("""{"diet_type":"Crudivora"}"""))

        // Tipos incorrectos y JSON que no es objeto → `invalid_arguments`.
        listOf(
            """{"allergies":"Frutos secos"}""",
            """{"allergies_none":"true"}""",
            """{"allergies":[5]}""",
            "no json",
            "[]",
        ).forEach { arguments ->
            assertNull(arguments, PincheChatProtocol.parseSaveProfileArgs(arguments))
            assertEquals("invalid_arguments", PincheChatProtocol.saveProfileArgsError(arguments))
        }
    }

    @Test
    fun saveProfileToolResultReportsWhatWasSavedAndWhatIsStillPending() {
        val result = JSONObject(
            PincheChatProtocol.saveProfileToolResult(
                saved = listOf(
                    SavedProfilePart(PendingProfilePart.ALLERGIES, chips = listOf("Frutos secos")),
                    SavedProfilePart(PendingProfilePart.DIET_TYPE, dietType = DietType.VEGAN),
                ),
                pending = listOf(PendingProfilePart.INTOLERANCES),
            ),
        )

        val saved = result.getJSONObject("saved")
        val allergies = saved.getJSONArray("allergies")
        assertEquals(1, allergies.length())
        assertEquals("Frutos secos", allergies.getString(0))
        assertFalse(saved.getBoolean("allergies_none"))
        assertEquals("VEGAN", saved.getString("diet_type"))
        // Solo las partes escritas: no se inventa lo que no se guardó.
        assertFalse(saved.has("intolerances"))
        val pending = result.getJSONArray("pending")
        assertEquals(listOf("INTOLERANCES"), (0 until pending.length()).map { pending.getString(it) })
    }

    @Test
    fun saveProfileToolErrorCarriesTheCode() {
        assertEquals(
            "invalid_chip",
            JSONObject(PincheChatProtocol.saveProfileToolError("invalid_chip")).getString("error"),
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

    // Enmienda D9: el texto visible durante el streaming nunca lleva el JSON de
    // la Propuesta, para no pintarlo en la burbuja a medida que llega.

    @Test
    fun streamingTextKeepsPlainConversationText() {
        assertEquals(
            "Para que no se pegue, remueve al principio.",
            PincheChatProtocol.streamingText(" Para que no se pegue, remueve al principio. "),
        )
    }

    @Test
    fun streamingTextCutsBeforeTheRecipeBlock() {
        val content = "Te propongo esta receta.\n```receta\n{\"s\":[{\"t\":\"Arroz\"}]}\n```"

        assertEquals("Te propongo esta receta.", PincheChatProtocol.streamingText(content))
    }

    @Test
    fun streamingTextCutsBeforeAJsonObjectStillStreaming() {
        val complete = "Toma esta receta. " +
            "{\"s\":[{\"t\":\"Tortilla\",\"i\":[{\"n\":\"Huevo\"}],\"p\":[{\"t\":\"Cuaja\"}]}]}"
        val partial = "Toma esta receta. {\"s\":[{\"t\":\"Tor"

        assertEquals("Toma esta receta.", PincheChatProtocol.streamingText(complete))
        assertEquals("Toma esta receta.", PincheChatProtocol.streamingText(partial))
    }

    @Test
    fun streamingTextKeepsBracesThatAreNotJson() {
        assertEquals(
            "Ajusta {el} formato a tu gusto.",
            PincheChatProtocol.streamingText("Ajusta {el} formato a tu gusto."),
        )
    }

}
