package com.whatsforlunch.app.data.ai

import android.content.Context

/**
 * Fuente de los prompts de los clientes de IA. Se inyecta para leer los ficheros de
 * `assets/prompts` en producción y permitir una implementación de test, sin acoplar
 * los parsers ni las reglas de dominio a Android. El español es el idioma de fallback.
 */
interface PromptSource {
    fun pincheSystemPrompt(language: String): String
    fun suggestionPrompt(language: String): String
    fun suggestionAvoidPrompt(language: String): String
    fun pantryScanPrompt(language: String): String
    fun dishPhotoPrompt(language: String): String
}

/**
 * Lee los prompts es/en de `assets/prompts` e inyecta el contrato JSON compartido con
 * `PincheChatProtocol` mediante el marcador `{RECIPE_CONTRACT}`.
 */
class AssetPromptSource(private val context: Context) : PromptSource {
    override fun pincheSystemPrompt(language: String): String = read(PINCHE_SYSTEM, language)

    override fun suggestionPrompt(language: String): String = read(SUGGESTION, language)

    override fun suggestionAvoidPrompt(language: String): String = read(SUGGESTION_AVOID, language)

    override fun pantryScanPrompt(language: String): String = read(PANTRY_SCAN, language)

    override fun dishPhotoPrompt(language: String): String = read(DISH_PHOTO, language)

    private fun read(name: String, language: String): String =
        context.assets.open(promptPath(name, language))
            .bufferedReader()
            .use { it.readText() }
            .let(::renderPrompt)
}

internal const val CONTRACT_PLACEHOLDER = "{RECIPE_CONTRACT}"
internal const val DISH_PLACEHOLDER = "{DISH}"
internal const val STORY_PLACEHOLDER = "{STORY}"

private const val PINCHE_SYSTEM = "pinche_system"
private const val SUGGESTION = "suggestion"
private const val SUGGESTION_AVOID = "suggestion_avoid"
private const val PANTRY_SCAN = "pantry_scan"
private const val DISH_PHOTO = "dish_photo"

internal fun isEnglish(language: String): Boolean = language.lowercase().startsWith("en")

internal fun promptPath(name: String, language: String): String =
    "prompts/${name}_${if (isEnglish(language)) "en" else "es"}.txt"

internal fun renderPrompt(template: String): String =
    template.replace(CONTRACT_PLACEHOLDER, RECIPE_CONTRACT).trim()

internal fun renderDishPhotoPrompt(
    title: String,
    story: String,
    language: String,
    source: PromptSource,
): String {
    val dish = title.ifBlank { story }.trim()
    val trimmedStory = story.trim()
    val storyPart = if (trimmedStory.isEmpty()) "" else " $trimmedStory."
    return source.dishPhotoPrompt(language)
        .replace(DISH_PLACEHOLDER, dish)
        .replace(STORY_PLACEHOLDER, storyPart)
        .trim()
}
