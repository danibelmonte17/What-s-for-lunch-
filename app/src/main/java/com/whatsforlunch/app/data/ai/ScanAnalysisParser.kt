package com.whatsforlunch.app.data.ai

import com.whatsforlunch.app.domain.scanner.AnalyzedFood
import org.json.JSONObject

// Tolerant parser for the AI answer (D4): it recovers the first JSON object from the
// message content, accepting optional ```json fences and surrounding explanations.
// A response without an object is not parseable and therefore a failure (RD05).
internal object ScanAnalysisParser {

    fun parse(content: String): List<AnalyzedFood>? {
        val json = firstJsonObject(content) ?: return null
        val objectValue = runCatching { JSONObject(json) }.getOrNull() ?: return null
        return objectValue.keys().asSequence().map { name ->
            val raw = objectValue.opt(name)
            val quantity = when (raw) {
                is Number -> raw.toDouble()
                is String -> raw.trim().toDoubleOrNull()
                else -> null
            }
            AnalyzedFood(name = name, quantity = quantity)
        }.toList()
    }

    private fun firstJsonObject(value: String): String? {
        val start = value.indexOf('{')
        if (start < 0) return null
        var depth = 0
        var inString = false
        var escaped = false
        for (index in start until value.length) {
            val char = value[index]
            if (inString) {
                when {
                    escaped -> escaped = false
                    char == '\\' -> escaped = true
                    char == '"' -> inString = false
                }
                continue
            }
            when (char) {
                '"' -> inString = true
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) return value.substring(start, index + 1)
                }
            }
        }
        return null
    }
}
