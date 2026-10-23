package com.whatsforlunch.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.whatsforlunch.app.ui.nav.WhatsForLunchApp
import com.whatsforlunch.app.ui.theme.WhatsForLunchTheme

class MainActivity : ComponentActivity() {
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
                WhatsForLunchApp()
            }
        }
    }
}
