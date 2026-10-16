package com.whatsforlunch.app.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.whatsforlunch.app.R
import com.whatsforlunch.app.ui.misrecetas.MisRecetasRoute

// Rutas de navegación. Mis Recetas es el destino inicial (RF-4, P18).
object AppRoutes {
    const val MIS_RECETAS = "mis_recetas"
    const val CREAR_RECETA = "crear_receta"
    const val ESCANEAR_DESPENSA = "escanear_despensa"
}

@Composable
fun WhatsForLunchApp(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = AppRoutes.MIS_RECETAS,
        modifier = modifier,
    ) {
        composable(AppRoutes.MIS_RECETAS) {
            MisRecetasRoute(
                onCreateRecipe = { navController.navigate(AppRoutes.CREAR_RECETA) },
                onScanPantry = { navController.navigate(AppRoutes.ESCANEAR_DESPENSA) },
            )
        }
        composable(AppRoutes.CREAR_RECETA) {
            ProvisionalScreen(
                title = stringResource(R.string.action_create_recipe),
                testTag = ProvisionalTestTags.FORM_SCREEN,
                onBack = { navController.popBackStack() },
            )
        }
        composable(AppRoutes.ESCANEAR_DESPENSA) {
            ProvisionalScreen(
                title = stringResource(R.string.action_scan_pantry),
                testTag = ProvisionalTestTags.SCANNER_SCREEN,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
