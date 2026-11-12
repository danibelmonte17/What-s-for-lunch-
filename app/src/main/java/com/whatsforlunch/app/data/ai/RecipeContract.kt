package com.whatsforlunch.app.data.ai

import org.json.JSONArray
import org.json.JSONObject

// Contrato JSON compacto compartido por las Sugerencias y las Propuestas del Pinche.
// Los prompts lo insertan con el marcador `{RECIPE_CONTRACT}` para que el formato de
// salida y el parser no puedan desincronizarse. Desde `redisenar-formulario-de-receta`
// el contrato no tiene tiempo de cocción (`cm`) ni opcionalidad (`o`, D8).
internal const val RECIPE_CONTRACT =
    "{\"s\":[{\"t\":\"title\",\"st\":\"short story\",\"mm\":\"BREAKFAST|LUNCH|DINNER|DESSERT\"," +
        "\"pm\":10,\"df\":\"EASY|MEDIUM|HARD\",\"bs\":4,\"kcal\":320," +
        "\"i\":[{\"n\":\"name\",\"q\":3,\"u\":\"unit\"}]," +
        "\"p\":[{\"t\":\"step text\",\"m\":10}]}]}"

// Esquema JSON formal del contrato (guía de IA del usuario, 2026-11-12): se envía
// como `response_format` con tipo `json_schema` en Sugerencias y en el Escáner y
// fija enums y límites en el proveedor, no solo en el texto del prompt (RF-MPC2).
// El parser tolerante (`RecipeSuggestionParser`/`ScanAnalysisParser`) sigue siendo
// la capa de validación y normalización tras la respuesta.
internal fun recipeJsonSchema(): JSONObject = JSONObject().apply {
    put("type", "object")
    put(
        "properties",
        JSONObject().put(
            "s",
            JSONObject().apply {
                put("type", "array")
                put("maxItems", 3)
                put("items", recipeItemSchema())
            },
        ),
    )
    put("required", JSONArray(listOf("s")))
}

private fun recipeItemSchema(): JSONObject = JSONObject().apply {
    put("type", "object")
    put(
        "properties",
        JSONObject().apply {
            put("t", JSONObject().put("type", "string"))
            put("st", JSONObject().put("type", "string"))
            put(
                "mm",
                JSONObject().put("type", "string")
                    .put("enum", JSONArray(listOf("BREAKFAST", "LUNCH", "DINNER", "DESSERT"))),
            )
            put("pm", optionalMinutesSchema())
            put(
                "df",
                JSONObject().put("type", "string")
                    .put("enum", JSONArray(listOf("EASY", "MEDIUM", "HARD"))),
            )
            put("bs", JSONObject().put("type", "integer").put("minimum", 1).put("maximum", 12))
            put("kcal", JSONObject().put("type", "integer").put("minimum", 0))
            put(
                "i",
                JSONObject().apply {
                    put("type", "array")
                    put("maxItems", 10)
                    put(
                        "items",
                        JSONObject().apply {
                            put("type", "object")
                            put(
                                "properties",
                                JSONObject().apply {
                                    put("n", JSONObject().put("type", "string"))
                                    put("q", JSONObject().put("type", "number"))
                                    put("u", JSONObject().put("type", "string"))
                                },
                            )
                            put("required", JSONArray(listOf("n")))
                        },
                    )
                },
            )
            put(
                "p",
                JSONObject().apply {
                    put("type", "array")
                    put("maxItems", 12)
                    put(
                        "items",
                        JSONObject().apply {
                            put("type", "object")
                            put(
                                "properties",
                                JSONObject().apply {
                                    put("t", JSONObject().put("type", "string"))
                                    put("m", optionalMinutesSchema())
                                },
                            )
                            put("required", JSONArray(listOf("t")))
                        },
                    )
                },
            )
        },
    )
    put("required", JSONArray(listOf("t", "mm", "df", "i", "p")))
}

// Minutos opcionales 1..1439 (RD: `isValidTimerMinutes` admite null | 1..1439, D10).
private fun optionalMinutesSchema(): JSONObject = JSONObject().apply {
    put("type", JSONArray(listOf("integer", "null")))
    put("minimum", 1)
    put("maximum", 1439)
}

// Escáner: mapa plano {"nombre del alimento": cantidad entera 1..99} (RD13).
internal fun scanJsonSchema(): JSONObject = JSONObject().apply {
    put("type", "object")
    put(
        "additionalProperties",
        JSONObject().put("type", "integer").put("minimum", 1).put("maximum", 99),
    )
}

// Sobre `response_format` con la forma estándar de OpenAI (hallazgo del smoke test
// de pago 2026-11-12: la forma plana de la guía `{"type":"json_schema","schema":…}`
// devuelve HTTP 400 —«response_format.json_schema is required»—; el alias `chat`
// acepta `{"type":"json_schema","json_schema":{name, schema}}`). Si el proveedor
// rechaza el esquema por otro motivo, el cliente reintenta una sola vez con
// `{"type":"json_object"}` (RF-MPC3).
internal fun responseFormatJsonSchema(name: String, schema: JSONObject): JSONObject =
    JSONObject()
        .put("type", "json_schema")
        .put("json_schema", JSONObject().put("name", name).put("schema", schema))

internal fun responseFormatJsonObject(): JSONObject =
    JSONObject().put("type", "json_object")

// RF-MPC3: un 4xx a una petición con `json_schema` se reintenta una sola vez con
// `json_object` (el proveedor puede no soportar el esquema, `⚠ S08`).
internal fun shouldFallbackToJsonObject(httpCode: Int): Boolean = httpCode in 400..499
