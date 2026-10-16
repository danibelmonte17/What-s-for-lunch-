package com.whatsforlunch.app.ui.nav

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.whatsforlunch.app.R

// Identificadores estables de los destinos provisionales (E2E).
object ProvisionalTestTags {
    const val FORM_SCREEN = "provisional_form_screen"
    const val SCANNER_SCREEN = "provisional_scanner_screen"
}

// Destino provisional con título y acción de vuelta (Q-arrancar-app-en-mis-recetas-3).
// `crear-receta` y `escanear-despensa-con-camara` lo sustituirán por las pantallas reales.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProvisionalScreen(
    title: String,
    testTag: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.testTag(testTag),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = { Text(text = title) },
                navigationIcon = {
                    TextButton(
                        onClick = onBack,
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) {
                        Text(text = stringResource(R.string.action_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.surface,
        ) {}
    }
}
