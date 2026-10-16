package com.whatsforlunch.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.whatsforlunch.app.ui.nav.WhatsForLunchApp
import com.whatsforlunch.app.ui.theme.WhatsForLunchTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WhatsForLunchTheme {
                WhatsForLunchApp()
            }
        }
    }
}
