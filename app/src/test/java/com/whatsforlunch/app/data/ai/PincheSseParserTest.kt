package com.whatsforlunch.app.data.ai

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// Enmienda D9/D16 (`rellenar-perfil-con-el-pinche`): el parser SSE es puro y se
// prueba en JVM (no hay MockWebServer en el proyecto).
class PincheSseParserTest {

    @Test
    fun contentFragmentsAccumulateInTheOrderTheyArrive() {
        val parser = PincheSseParser()

        parser.feed(chunk(content = "Hola "))
        parser.feed(chunk(content = "mundo"))
        parser.feed(chunk(content = ", sin JSON"))

        assertEquals("Hola mundo, sin JSON", parser.accumulatedText)
        assertFalse(parser.invalid)
        assertEquals("Hola mundo, sin JSON", parser.buildMessage()?.getString("content"))
    }

    @Test
    fun toolCallFragmentsAreReassembledByIndex() {
        val parser = PincheSseParser()

        parser.feed(chunk(toolCall = toolCall(index = 0, id = "call_1", name = "save_profile")))
        parser.feed(chunk(toolCall = toolCall(index = 0, arguments = "{\"allergies\":[\"Frutos")))
        parser.feed(chunk(toolCall = toolCall(index = 0, arguments = " secos\"]}")))

        val message = parser.buildMessage()
        assertNotNull(message)
        val calls = message!!.getJSONArray("tool_calls")
        assertEquals(1, calls.length())
        val function = calls.getJSONObject(0).getJSONObject("function")
        assertEquals("call_1", calls.getJSONObject(0).getString("id"))
        assertEquals("save_profile", function.getString("name"))
        assertEquals("""{"allergies":["Frutos secos"]}""", function.getString("arguments"))
        assertFalse(message.has("content"))
    }

    @Test
    fun severalToolCallsKeepTheirOwnIndexOrder() {
        val parser = PincheSseParser()

        parser.feed(chunk(toolCall = toolCall(index = 1, id = "call_2", name = "get_pantry")))
        parser.feed(chunk(toolCall = toolCall(index = 0, id = "call_1", name = "get_my_recipes")))

        val calls = parser.buildMessage()!!.getJSONArray("tool_calls")
        assertEquals(listOf("call_1", "call_2"), listOf(calls.getJSONObject(0).getString("id"), calls.getJSONObject(1).getString("id")))
    }

    @Test
    fun contentAndToolCallsCanShareTheSameRound() {
        val parser = PincheSseParser()

        parser.feed(chunk(content = "Voy a guardarlo. "))
        parser.feed(chunk(toolCall = toolCall(index = 0, id = "call_9", name = "save_profile", arguments = "{}")))

        val message = parser.buildMessage()
        assertEquals("Voy a guardarlo. ", message!!.getString("content"))
        assertEquals(1, message.getJSONArray("tool_calls").length())
    }

    @Test
    fun doneMarksTheStreamAsFinished() {
        val parser = PincheSseParser()

        parser.feed(chunk(content = "hola"))
        assertFalse(parser.finished)
        parser.feed("[DONE]")

        assertTrue(parser.finished)
        assertFalse(parser.invalid)
        assertEquals("hola", parser.buildMessage()?.getString("content"))
    }

    @Test
    fun invalidPayloadMarksTheParserAndBuildsNothing() {
        val parser = PincheSseParser()

        parser.feed(chunk(content = "hola"))
        parser.feed("{not json")

        assertTrue(parser.invalid)
        assertNull(parser.buildMessage())
    }

    @Test
    fun streamWithoutUsableDataBuildsNothing() {
        val empty = PincheSseParser()
        assertNull(empty.buildMessage())

        val onlyRole = PincheSseParser()
        onlyRole.feed(JSONObject().put("choices", JSONArray().put(JSONObject().put("delta", JSONObject().put("role", "assistant")))).toString())
        assertNull(onlyRole.buildMessage())
    }

    private fun chunk(content: String? = null, toolCall: JSONObject? = null): String {
        val delta = JSONObject()
        if (content != null) delta.put("content", content)
        if (toolCall != null) delta.put("tool_calls", JSONArray().put(toolCall))
        return JSONObject()
            .put(
                "choices",
                JSONArray().put(JSONObject().put("index", 0).put("delta", delta)),
            )
            .toString()
    }

    private fun toolCall(
        index: Int,
        id: String? = null,
        name: String? = null,
        arguments: String? = null,
    ): JSONObject {
        val fragment = JSONObject().put("index", index)
        if (id != null) fragment.put("id", id)
        if (name != null || arguments != null) {
            val function = JSONObject()
            if (name != null) function.put("name", name)
            if (arguments != null) function.put("arguments", arguments)
            fragment.put("function", function)
        }
        return fragment
    }
}
