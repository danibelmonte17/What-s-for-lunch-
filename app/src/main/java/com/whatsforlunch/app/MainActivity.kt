package com.whatsforlunch.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.whatsforlunch.app.data.settings.AppLocaleController
import com.whatsforlunch.app.ui.locale.AppLocaleHost
import com.whatsforlunch.app.ui.nav.WhatsForLunchApp
import com.whatsforlunch.app.ui.theme.WhatsForLunchTheme
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    // Idioma activo de la app (cambiar-idioma-de-la-interfaz): el host de la raíz lo
    // aplica a toda la composición sin reiniciar.
    private val appLocaleController: AppLocaleController by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Edge-to-edge real: barras transparentes y iconos oscuros (la app es
        // light-only y dibuja su superficie cálida también bajo las barras).
        val transparentSystemBar = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        enableEdgeToEdge(
            statusBarStyle = transparentSystemBar,
            navigationBarStyle = transparentSystemBar,
        )
        setContent {
            WhatsForLunchTheme {
                AppLocaleHost(appLocaleController) {
                    WhatsForLunchApp()
                }
            }
        }
    }
}
