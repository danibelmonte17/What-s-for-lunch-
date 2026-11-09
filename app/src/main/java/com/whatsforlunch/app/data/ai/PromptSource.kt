package com.whatsforlunch.app.data.ai

import android.content.Context
import com.whatsforlunch.app.domain.model.DietType
import com.whatsforlunch.app.domain.model.UserProfile

/**
 * Fuente de los prompts de los clientes de IA. Se inyecta para leer los ficheros de
 * `assets/prompts` en producción y permitir una implementación de test, sin acoplar
 * los parsers ni las reglas de dominio a Android. El español es el idioma de fallback.
 */
interface PromptSource {
    fun pincheSystemPrompt(language: String): String
    fun pincheProfilePrompt(language: String): String
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

    override fun pincheProfilePrompt(language: String): String = read(PINCHE_PROFILE, language)

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

// Marcadores del bloque de perfil del Pinche (`crear-mi-perfil`, D6).
internal const val DIET_PLACEHOLDER = "{DIET_TYPE}"
internal const val ALLERGIES_PLACEHOLDER = "{ALLERGIES}"
internal const val INTOLERANCES_PLACEHOLDER = "{INTOLERANCES}"

private const val PINCHE_SYSTEM = "pinche_system"
private const val PINCHE_PROFILE = "pinche_profile"
private const val SUGGESTION = "suggestion"
private const val SUGGESTION_AVOID = "suggestion_avoid"
private const val PANTRY_SCAN = "pantry_scan"
private const val DISH_PHOTO = "dish_photo"

internal fun isEnglish(language: String): Boolean = language.lowercase().startsWith("en")

internal fun promptPath(name: String, language: String): String =
    "prompts/${name}_${if (isEnglish(language)) "en" else "es"}.txt"

internal fun renderPrompt(template: String): String =
    template.replace(CONTRACT_PLACEHOLDER, RECIPE_CONTRACT).trim()

/**
 * Compone el bloque de perfil con el template `pinche_profile_{es,en}.txt`
 * (`crear-mi-perfil`, D6). El texto libre de las chips viaja **literal** (no se
 * traduce); las secciones marcadas «No tengo ninguna» se escriben como
 * «ninguna»/«none» y el vocabulario de tipos de alimentación es es/en propio de
 * este bloque (los textos de UI viven en strings.xml con paridad).
 */
internal fun renderProfilePrompt(
    template: String,
    profile: UserProfile,
    language: String,
): String = template
    .replace(DIET_PLACEHOLDER, profile.dietType?.let { dietTypeName(it, language) } ?: notChosen(language))
    .replace(ALLERGIES_PLACEHOLDER, sectionText(profile.allergies, profile.allergiesNone, language))
    .replace(INTOLERANCES_PLACEHOLDER, sectionText(profile.intolerances, profile.intolerancesNone, language))
    .trim()

private fun dietTypeName(dietType: DietType, language: String): String =
    if (isEnglish(language)) {
        when (dietType) {
            DietType.OMNIVORE -> "Omnivore"
            DietType.PESCETARIAN -> "Pescetarian"
            DietType.VEGETARIAN -> "Vegetarian"
            DietType.VEGAN -> "Vegan"
            DietType.KETOGENIC -> "Ketogenic"
        }
    } else {
        when (dietType) {
            DietType.OMNIVORE -> "Omnívora"
            DietType.PESCETARIAN -> "Pescetariana"
            DietType.VEGETARIAN -> "Vegetariana"
            DietType.VEGAN -> "Vegana"
            DietType.KETOGENIC -> "Cetogénica"
        }
    }

private fun sectionText(chips: List<String>, none: Boolean, language: String): String = when {
    none -> if (isEnglish(language)) "none" else "ninguna"
    chips.isEmpty() -> if (isEnglish(language)) "not declared" else "sin declarar"
    else -> chips.joinToString(", ")
}

private fun notChosen(language: String): String =
    if (isEnglish(language)) "not chosen" else "sin elegir"

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
