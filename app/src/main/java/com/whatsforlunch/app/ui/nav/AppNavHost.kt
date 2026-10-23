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
import com.whatsforlunch.app.ui.aisuggestions.AiSuggestionsRoute
import com.whatsforlunch.app.ui.createrecipe.CreateRecipeRoute
import com.whatsforlunch.app.ui.myrecipes.MyRecipesRoute
import com.whatsforlunch.app.ui.pantry.PantryRoute
import com.whatsforlunch.app.ui.recipedetail.RecipeDetailRoute
import com.whatsforlunch.app.ui.scanner.ScannerRoute
import com.whatsforlunch.app.ui.settings.SettingsRoute

// Rutas de navegación. Mis Recetas es el destino inicial (RF-4, P18). El formulario
// de Receta acepta un id opcional: null = alta, con id = edición (RF-1). El Detalle
// de Receta abre por id desde la tarjeta (RF-DR1) y su CTA llega al destino
// provisional de Modo Cocina hasta que exista cocinar-paso-a-paso (RF-DR7, D4).
object AppRoutes {
    const val MY_RECIPES = "my_recipes"
    const val CREATE_RECIPE = "create_recipe?recipeId={recipeId}"
    const val SCAN_PANTRY = "scan_pantry"
    const val PANTRY = "pantry"
    const val RECIPE_DETAIL = "recipe_detail/{recipeId}"
    const val COOK_MODE_PROVISIONAL = "cook_mode_provisional"
    const val SETTINGS = "settings"
    const val AI_SUGGESTIONS = "ai_suggestions"
    const val RECIPE_ID_ARG = "recipeId"

    fun createRecipe(): String = "create_recipe"

    fun editRecipe(recipeId: String): String = "create_recipe?$RECIPE_ID_ARG=$recipeId"

    fun recipeDetail(recipeId: String): String = "recipe_detail/$recipeId"
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
                onOpenRecipe = { recipeId -> navController.navigate(AppRoutes.recipeDetail(recipeId)) },
                onOpenPantry = { navController.navigate(AppRoutes.PANTRY) },
                onOpenSettings = { navController.navigate(AppRoutes.SETTINGS) },
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
        composable(
            route = AppRoutes.RECIPE_DETAIL,
            arguments = listOf(
                navArgument(AppRoutes.RECIPE_ID_ARG) { type = NavType.StringType },
            ),
        ) { entry ->
            RecipeDetailRoute(
                recipeId = entry.arguments?.getString(AppRoutes.RECIPE_ID_ARG).orEmpty(),
                onBack = { navController.popBackStack() },
                onStartCooking = { navController.navigate(AppRoutes.COOK_MODE_PROVISIONAL) },
            )
        }
        // RF-DR7 (D4): destino provisional de Modo Cocina con vuelta atrás
        // (Q-arrancar-app-en-mis-recetas-3); cocinar-paso-a-paso lo sustituirá.
        composable(AppRoutes.COOK_MODE_PROVISIONAL) {
            ProvisionalScreen(
                title = stringResource(R.string.cook_mode_title),
                testTag = ProvisionalTestTags.COOK_MODE_SCREEN,
                onBack = { navController.popBackStack() },
            )
        }
        composable(AppRoutes.SCAN_PANTRY) {
            ScannerRoute(
                onBack = { navController.popBackStack() },
                onSaved = {
                    navController.navigate(AppRoutes.PANTRY) {
                        popUpTo(AppRoutes.SCAN_PANTRY) { inclusive = true }
                    }
                },
                onOpenRecipe = { recipeId -> navController.navigate(AppRoutes.recipeDetail(recipeId)) },
                onSuggestWithAi = { navController.navigate(AppRoutes.AI_SUGGESTIONS) },
            )
        }
        // RF-SRC1: destino real de Sugerencias, que sustituye al provisional.
        composable(AppRoutes.AI_SUGGESTIONS) {
            AiSuggestionsRoute(
                onBack = { navController.popBackStack() },
                onOpenSettings = { navController.navigate(AppRoutes.SETTINGS) },
            )
        }
        composable(AppRoutes.PANTRY) {
            PantryRoute(onBack = { navController.popBackStack() })
        }
        composable(AppRoutes.SETTINGS) {
            SettingsRoute(onBack = { navController.popBackStack() })
        }
    }
}
