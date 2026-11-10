package com.whatsforlunch.app.data.ai

import java.util.TreeMap
import org.json.JSONArray
import org.json.JSONObject

// Parser SSE puro del Pinche (`rellenar-perfil-con-el-pinche`, D9/D16): acumula
// los fragmentos de `choices[0].delta` —texto y `tool_calls` por `index`— hasta
// reconstruir el `message` que ya esperan `PincheChatProtocol.toolCalls` y
// `messageContent`, de modo que el bucle de rondas, el tope de llamadas y
// `parseReply` no cambian. Sin red ni Android para poder probarlo en JVM (en el
// proyecto no hay MockWebServer ni librería SSE).
internal class PincheSseParser {

    private val content = StringBuilder()
    private val toolCalls = TreeMap<Int, ToolCallFragment>()

    /** true cuando un `data:` no es JSON legible: la tanda se da por fallida. */
    var invalid = false
        private set

    /** true cuando el proveedor envía el cierre `data: [DONE]`. */
    var finished = false
        private set

    /** Texto acumulado de la ronda en curso (aún sin cortar con `streamingText`). */
    val accumulatedText: String
        get() = content.toString()

    /** Procesa la carga útil de una línea `data:` (sin el prefijo). */
    fun feed(payload: String) {
        val data = payload.trim()
        if (data.isEmpty()) return
        if (data == DONE) {
            finished = true
            return
        }
        val chunk = try {
            JSONObject(data)
        } catch (_: Exception) {
            invalid = true
            return
        }
        val delta = chunk.optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("delta") ?: return
        (delta.opt("content") as? String)?.let { content.append(it) }
        val fragments = delta.optJSONArray("tool_calls") ?: return
        for (position in 0 until fragments.length()) {
            val fragment = fragments.optJSONObject(position) ?: continue
            val key = fragment.optInt("index", position)
            val accumulator = toolCalls.getOrPut(key) { ToolCallFragment() }
            fragment.optString("id").takeIf { it.isNotBlank() }?.let { accumulator.id = it }
            val function = fragment.optJSONObject("function") ?: continue
            function.optString("name").takeIf { it.isNotBlank() }?.let { accumulator.name = it }
            function.optString("arguments")
                .takeIf { it.isNotEmpty() }
                ?.let { accumulator.arguments.append(it) }
        }
    }

    /**
     * `message` reconstruido con la forma que ya consumen `toolCalls()` y
     * `messageContent()`, o `null` cuando no llegó nada utilizable (JSON
     * ilegible, sin `data:` o stream vacío).
     */
    fun buildMessage(): JSONObject? {
        if (invalid) return null
        val calls = toolCalls.values.filter { it.id.isNotBlank() && it.name.isNotBlank() }
        if (content.isEmpty() && calls.isEmpty()) return null
        val message = JSONObject().put("role", "assistant")
        if (content.isNotEmpty()) message.put("content", content.toString())
        if (calls.isNotEmpty()) {
            val array = JSONArray()
            calls.forEach { call ->
                array.put(
                    JSONObject()
                        .put("id", call.id)
                        .put("type", "function")
                        .put(
                            "function",
                            JSONObject()
                                .put("name", call.name)
                                .put("arguments", call.arguments.toString()),
                        ),
                )
            }
            message.put("tool_calls", array)
        }
        return message
    }

    private class ToolCallFragment {
        var id = ""
        var name = ""
        val arguments = StringBuilder()
    }

    private companion object {
        const val DONE = "[DONE]"
    }
}
