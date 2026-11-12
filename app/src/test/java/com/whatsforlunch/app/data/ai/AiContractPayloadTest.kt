package com.whatsforlunch.app.data.ai

import com.whatsforlunch.app.domain.suggestion.SuggestableFood
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// RF-MPC2/MPC3 (guía de IA del usuario): esquema formal del contrato, sobre
// `response_format` en la forma estándar de OpenAI (`json_schema: {name, schema}`,
// hallazgo del smoke test: la forma plana de la guía devuelve 400), payloads
// system/user delimitados y decisión de fallback a `json_object`. Los builders son
// funciones puras: se verifican sin red ni MockWebServer (patrón del repo).
class AiContractPayloadTest {

    private val source = TestPromptSource()
    private val noKey = object : AiKeyProvider {
        override fun apiKey(): String? = null
    }
    private val suggester = OpenAiCompatibleRecipeSuggester(OkHttpClient(), noKey, source)
    private val analyzer = OpenAiCompatibleScanImageAnalyzer(OkHttpClient(), noKey, source)
    private val foods = listOf(SuggestableFood("Manzana", 3))

    @Test
    fun recipeSchemaCarriesTheEnumsAndLimitsOfTheContract() {
        val schema = recipeJsonSchema()

        assertEquals("object", schema.getString("type"))
        assertEquals(listOf("s"), (0 until schema.getJSONArray("required").length()).map {
            schema.getJSONArray("required").getString(it)
        })
        val items = schema.getJSONObject("properties").getJSONObject("s")
        assertEquals("array", items.getString("type"))
        assertEquals(3, items.getInt("maxItems"))
        val item = items.getJSONObject("items")
        val required = (0 until item.getJSONArray("required").length()).map {
            item.getJSONArray("required").getString(it)
        }
        assertTrue("faltan required: $required", listOf("t", "mm", "df", "i", "p").all { it in required })
        val properties = item.getJSONObject("properties")
        val mm = properties.getJSONObject("mm").getJSONArray("enum")
        assertEquals(
            listOf("BREAKFAST", "LUNCH", "DINNER", "DESSERT"),
            (0 until mm.length()).map { mm.getString(it) },
        )
        val df = properties.getJSONObject("df").getJSONArray("enum")
        assertEquals(listOf("EASY", "MEDIUM", "HARD"), (0 until df.length()).map { df.getString(it) })
        assertEquals(10, properties.getJSONObject("i").getInt("maxItems"))
        assertEquals(12, properties.getJSONObject("p").getInt("maxItems"))
        assertEquals(1, properties.getJSONObject("bs").getInt("minimum"))
        assertEquals(12, properties.getJSONObject("bs").getInt("maximum"))
        val stepMinutes = properties.getJSONObject("p").getJSONObject("items")
            .getJSONObject("properties").getJSONObject("m")
        assertTrue("m debe admitir null", stepMinutes.getJSONArray("type").toString().contains("null"))
        assertEquals(1, stepMinutes.getInt("minimum"))
        assertEquals(1439, stepMinutes.getInt("maximum"))
    }

    @Test
    fun scanSchemaIsAFlatMapOfQuantitiesBetweenOneAndNinetyNine() {
        val schema = scanJsonSchema()

        assertEquals("object", schema.getString("type"))
        val additional = schema.getJSONObject("additionalProperties")
        assertEquals("integer", additional.getString("type"))
        assertEquals(1, additional.getInt("minimum"))
        assertEquals(99, additional.getInt("maximum"))
    }

    @Test
    fun responseFormatUsesTheOpenAiShapeWithTheSchemaWrapper() {
        val schema = responseFormatJsonSchema("recipe_suggestions", recipeJsonSchema())

        assertEquals("json_schema", schema.getString("type"))
        val wrapper = schema.getJSONObject("json_schema")
        assertEquals("recipe_suggestions", wrapper.getString("name"))
        assertEquals(recipeJsonSchema().toString(), wrapper.getJSONObject("schema").toString())
        assertEquals("json_object", responseFormatJsonObject().getString("type"))
        assertTrue(shouldFallbackToJsonObject(400))
        assertTrue(shouldFallbackToJsonObject(404))
        assertTrue(shouldFallbackToJsonObject(429))
        assertFalse(shouldFallbackToJsonObject(200))
        assertFalse(shouldFallbackToJsonObject(500))
    }

    @Test
    fun suggestionPayloadSeparatesSystemFromDelimitedUserData() {
        val payload = suggester.buildBody(foods, "es", emptyList(), useJsonSchema = true)

        assertEquals("chat", payload.getString("model"))
        assertEquals(0, payload.getInt("temperature"))
        val format = payload.getJSONObject("response_format")
        assertEquals("json_schema", format.getString("type"))
        assertEquals(
            "recipe_suggestions",
            format.getJSONObject("json_schema").getString("name"),
        )
        assertTrue(format.getJSONObject("json_schema").getJSONObject("schema").getJSONObject("properties").has("s"))
        val messages = payload.getJSONArray("messages")
        assertEquals(2, messages.length())
        val system = messages.getJSONObject(0)
        assertEquals("system", system.getString("role"))
        assertTrue(system.getString("content").contains(RECIPE_CONTRACT))
        assertFalse(system.getString("content").contains("@@"))
        val user = messages.getJSONObject(1)
        assertEquals("user", user.getString("role"))
        assertTrue(user.getString("content").contains("<<<Manzana (3)>>>"))
        assertFalse(user.getString("content").contains(RECIPE_CONTRACT))
    }

    @Test
    fun suggestionPayloadFallsBackToJsonObjectOnDemand() {
        val payload = suggester.buildBody(foods, "es", emptyList(), useJsonSchema = false)

        assertEquals("json_object", payload.getJSONObject("response_format").getString("type"))
        assertEquals(2, payload.getJSONArray("messages").length())
    }

    @Test
    fun scanPayloadPutsTheRulesInSystemAndThePhotoInUser() {
        val payload = analyzer.buildBody(byteArrayOf(1, 2, 3), "es", useJsonSchema = true)

        val messages = payload.getJSONArray("messages")
        assertEquals(2, messages.length())
        val system = messages.getJSONObject(0)
        assertEquals("system", system.getString("role"))
        assertTrue(system.getString("content").contains("Analiza la foto"))
        // M1 del validador: ni `@@system@@` ni `@@user@@` pueden llegar al modelo.
        assertFalse(system.getString("content").contains("@@"))
        val user = messages.getJSONObject(1)
        assertEquals("user", user.getString("role"))
        val content = user.getJSONArray("content")
        assertEquals("<<<imagen>>>", content.getJSONObject(0).getString("text"))
        assertFalse(content.getJSONObject(0).getString("text").contains("@@"))
        assertTrue(
            content.getJSONObject(1).getJSONObject("image_url").getString("url")
                .startsWith("data:image/jpeg;base64,"),
        )
        val format = payload.getJSONObject("response_format")
        assertEquals("json_schema", format.getString("type"))
        assertEquals(
            "scan_analysis",
            format.getJSONObject("json_schema").getString("name"),
        )
        assertEquals(
            99,
            format.getJSONObject("json_schema").getJSONObject("schema")
                .getJSONObject("additionalProperties").getInt("maximum"),
        )
    }

    @Test
    fun scanPayloadFallsBackToJsonObjectOnDemand() {
        val payload = analyzer.buildBody(byteArrayOf(1), "en", useJsonSchema = false)

        assertEquals("json_object", payload.getJSONObject("response_format").getString("type"))
        val system = payload.getJSONArray("messages").getJSONObject(0)
        assertTrue(system.getString("content").contains("Analyze the photo"))
    }
}
