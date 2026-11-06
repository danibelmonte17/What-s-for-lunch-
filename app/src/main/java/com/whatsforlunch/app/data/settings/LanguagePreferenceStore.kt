package com.whatsforlunch.app.data.settings

import com.whatsforlunch.app.domain.model.AppLanguage

// Elección de idioma del usuario en Ajustes (D1 de la spec de
// cambiar-idioma-de-la-interfaz): persiste entre reinicios de la app. `null` =
// sin elección guardada (primer arranque o datos borrados) y manda el idioma del
// sistema. El idioma no es un dato sensible: sin cifrado.
interface LanguagePreferenceStore {
    fun selected(): AppLanguage?

    fun select(language: AppLanguage)
}
