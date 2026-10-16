package com.whatsforlunch.app.ui.misrecetas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.whatsforlunch.app.R
import com.whatsforlunch.app.domain.misrecetas.MisRecetasState
import com.whatsforlunch.app.ui.theme.WarmSpacing

// Identificadores estables para las pruebas instrumentadas (E2E).
object MisRecetasTestTags {
    const val SCREEN = "mis_recetas_screen"
    const val EMPTY_TITLE = "mis_recetas_empty_title"
    const val EMPTY_MESSAGE = "mis_recetas_empty_message"
    const val CREATE_RECIPE = "action_create_recipe"
    const val SCAN_PANTRY = "action_scan_pantry"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MisRecetasScreen(
    uiState: MisRecetasUiState,
    onCreateRecipe: () -> Unit,
    onScanPantry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.testTag(MisRecetasTestTags.SCREEN),
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.mis_recetas_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
    ) { innerPadding ->
        when (uiState.state) {
            MisRecetasState.Empty -> EmptyMisRecetas(
                onCreateRecipe = onCreateRecipe,
                onScanPantry = onScanPantry,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
            // El listado de Recetas con tarjetas llega en features posteriores.
            MisRecetasState.WithRecipes -> Unit
        }
    }
}

@Composable
private fun EmptyMisRecetas(
    onCreateRecipe: () -> Unit,
    onScanPantry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = WarmSpacing.lg),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.mis_recetas_empty_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag(MisRecetasTestTags.EMPTY_TITLE),
        )
        Text(
            text = stringResource(R.string.mis_recetas_empty_message),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(top = WarmSpacing.sm)
                .testTag(MisRecetasTestTags.EMPTY_MESSAGE),
        )
        Button(
            onClick = onCreateRecipe,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = WarmSpacing.xl)
                .heightIn(min = 52.dp)
                .testTag(MisRecetasTestTags.CREATE_RECIPE),
        ) {
            Text(text = stringResource(R.string.action_create_recipe))
        }
        FilledTonalButton(
            onClick = onScanPantry,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = WarmSpacing.md)
                .heightIn(min = 48.dp)
                .testTag(MisRecetasTestTags.SCAN_PANTRY),
        ) {
            Text(text = stringResource(R.string.action_scan_pantry))
        }
    }
}
