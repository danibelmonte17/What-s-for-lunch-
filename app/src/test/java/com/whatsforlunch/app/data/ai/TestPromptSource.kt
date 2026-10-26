package com.whatsforlunch.app.data.ai

import java.io.File

// Fuente de prompts para tests JVM: lee los mismos assets que producción, sin Android.
internal class TestPromptSource(
    private val root: File = File("src/main/assets"),
) : PromptSource {

    override fun pincheSystemPrompt(language: String): String = render("pinche_system", language)

    override fun suggestionPrompt(language: String): String = render("suggestion", language)

    override fun suggestionAvoidPrompt(language: String): String = render("suggestion_avoid", language)

    override fun pantryScanPrompt(language: String): String = render("pantry_scan", language)

    override fun dishPhotoPrompt(language: String): String = render("dish_photo", language)

    fun template(name: String, language: String): String =
        File(root, promptPath(name, language)).readText()

    private fun render(name: String, language: String): String =
        renderPrompt(template(name, language))
}
