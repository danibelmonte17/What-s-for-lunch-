package com.whatsforlunch.app.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.whatsforlunch.app.R
import com.whatsforlunch.app.ui.crearreceta.CrearRecetaRoute
import com.whatsforlunch.app.ui.misrecetas.MisRecetasRoute

// Rutas de navegación. Mis Recetas es el destino inicial (RF-4, P18). El formulario
// de Receta acepta un id opcional: null = alta, con id = edición (RF-1).
object AppRoutes {
    const val MIS_RECETAS = "mis_recetas"
    const val CREAR_RECETA = "crear_receta?recipeId={recipeId}"
    const val ESCANEAR_DESPENSA = "escanear_despensa"
    const val RECIPE_ID_ARG = "recipeId"

    fun crearReceta(): String = "crear_receta"

    fun editarReceta(recipeId: String): String = "crear_receta?$RECIPE_ID_ARG=$recipeId"
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
                onCreateRecipe = { navController.navigate(AppRoutes.crearReceta()) },
                onScanPantry = { navController.navigate(AppRoutes.ESCANEAR_DESPENSA) },
                onEditRecipe = { recipeId -> navController.navigate(AppRoutes.editarReceta(recipeId)) },
            )
        }
        composable(
            route = AppRoutes.CREAR_RECETA,
            arguments = listOf(
                navArgument(AppRoutes.RECIPE_ID_ARG) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) { entry ->
            CrearRecetaRoute(
                onDone = { navController.popBackStack() },
                recipeId = entry.arguments?.getString(AppRoutes.RECIPE_ID_ARG),
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
