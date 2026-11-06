package com.whatsforlunch.app.ui.locale

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.whatsforlunch.app.data.settings.AppLocaleController

// Aplica el idioma elegido en Ajustes a toda la composición sin reiniciar la app
// (D4 de la spec de cambiar-idioma-de-la-interfaz): reprovee LocalConfiguration y
// LocalResources con un contexto localizado (mismo patrón que los E2E de
// localización) para que stringResource, los LocalConfiguration de las Routes
// (prompts de IA y locale de voz) y el formato de cantidades sigan el idioma
// activo. LocalContext se conserva como el Activity real: CreateRecipeRoute lo
// necesita para el PhotoAcquirer del sistema.
@Composable
fun AppLocaleHost(
    appLocaleController: AppLocaleController,
    content: @Composable () -> Unit,
) {
    val language by appLocaleController.language.collectAsStateWithLifecycle()
    val baseContext = LocalContext.current
    val baseConfiguration = LocalConfiguration.current
    val localizedContext = baseContext.createConfigurationContext(
        Configuration(baseConfiguration).apply { setLocale(language.locale) },
    )
    CompositionLocalProvider(
        LocalConfiguration provides localizedContext.resources.configuration,
        LocalResources provides localizedContext.resources,
    ) {
        content()
    }
}
