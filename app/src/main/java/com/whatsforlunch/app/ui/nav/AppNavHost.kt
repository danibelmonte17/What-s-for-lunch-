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
import com.whatsforlunch.app.ui.createrecipe.CreateRecipeRoute
import com.whatsforlunch.app.ui.myrecipes.MyRecipesRoute

// Rutas de navegación. Mis Recetas es el destino inicial (RF-4, P18). El formulario
// de Receta acepta un id opcional: null = alta, con id = edición (RF-1).
object AppRoutes {
    const val MY_RECIPES = "my_recipes"
    const val CREATE_RECIPE = "create_recipe?recipeId={recipeId}"
    const val SCAN_PANTRY = "scan_pantry"
    const val RECIPE_ID_ARG = "recipeId"

    fun createRecipe(): String = "create_recipe"

    fun editRecipe(recipeId: String): String = "create_recipe?$RECIPE_ID_ARG=$recipeId"
}

@Composable
fun WhatsForLunchApp(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = AppRoutes.MY_RECIPES,
        modifier = modifier,
    ) {
        composable(AppRoutes.MY_RECIPES) {
            MyRecipesRoute(
                onCreateRecipe = { navController.navigate(AppRoutes.createRecipe()) },
                onScanPantry = { navController.navigate(AppRoutes.SCAN_PANTRY) },
                onEditRecipe = { recipeId -> navController.navigate(AppRoutes.editRecipe(recipeId)) },
            )
        }
        composable(
            route = AppRoutes.CREATE_RECIPE,
            arguments = listOf(
                navArgument(AppRoutes.RECIPE_ID_ARG) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) { entry ->
            CreateRecipeRoute(
                onDone = { navController.popBackStack() },
                recipeId = entry.arguments?.getString(AppRoutes.RECIPE_ID_ARG),
            )
        }
        composable(AppRoutes.SCAN_PANTRY) {
            ProvisionalScreen(
                title = stringResource(R.string.action_scan_pantry),
                testTag = ProvisionalTestTags.SCANNER_SCREEN,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
