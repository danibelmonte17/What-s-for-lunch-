// Plugins declarados en la raíz y aplicados en el módulo :app.
// AGP 9 integra Kotlin: el plugin `kotlin-android` ya no se aplica (ver AGENTS/docs).
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
