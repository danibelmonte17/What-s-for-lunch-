package com.whatsforlunch.app.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.whatsforlunch.app.ui.aisuggestions.AiSuggestionsRoute
import com.whatsforlunch.app.ui.cookmode.CookModeRoute
import com.whatsforlunch.app.ui.createrecipe.CreateRecipeRoute
import com.whatsforlunch.app.ui.myrecipes.MyRecipesRoute
import com.whatsforlunch.app.ui.pantry.PantryRoute
import com.whatsforlunch.app.ui.pinche.PincheRoute
import com.whatsforlunch.app.ui.recommendations.RecommendationsRoute
import com.whatsforlunch.app.ui.recipedetail.RecipeDetailRoute
import com.whatsforlunch.app.ui.scanner.ScannerRoute
import com.whatsforlunch.app.ui.settings.SettingsRoute
import com.whatsforlunch.app.ui.shoppinglist.ShoppingListRoute

// Rutas de navegación. Mis Recetas es el destino inicial (RF-4, P18). El formulario
// de Receta acepta un id opcional: null = alta, con id = edición (RF-1). El Detalle
// de Receta abre por id desde la tarjeta (RF-DR1) y su CTA abre el Modo Cocina real
// con el id de la Receta y las Raciones elegidas (RF-CPP5, RD22).
object AppRoutes {
    const val MY_RECIPES = "my_recipes"
    const val CREATE_RECIPE = "create_recipe?recipeId={recipeId}"
    const val SCAN_PANTRY = "scan_pantry"
    const val PANTRY_RECIPES = "pantry_recipes"
    const val PANTRY = "pantry"
    const val RECIPE_DETAIL = "recipe_detail/{recipeId}"
    const val COOK_MODE = "cook_mode/{recipeId}?servings={servings}"
    const val SETTINGS = "settings"
    const val AI_SUGGESTIONS = "ai_suggestions"
    const val PINCHE = "pinche"
    const val SHOPPING_LIST = "shopping_list"
    const val RECIPE_ID_ARG = "recipeId"
    const val SERVINGS_ARG = "servings"

    fun createRecipe(): String = "create_recipe"

    fun editRecipe(recipeId: String): String = "create_recipe?$RECIPE_ID_ARG=$recipeId"

    fun recipeDetail(recipeId: String): String = "recipe_detail/$recipeId"

    // RF-CPP4: las Raciones viajan como texto opcional (patrón de `recipeId` en el
    // formulario); sin argumento el Modo Cocina usa las Raciones base.
    fun cookMode(recipeId: String, servings: Int): String = "cook_mode/$recipeId?$SERVINGS_ARG=$servings"
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
                onOpenPantryRecipes = { navController.navigate(AppRoutes.PANTRY_RECIPES) },
                onOpenPantry = { navController.navigate(AppRoutes.PANTRY) },
                onOpenSettings = { navController.navigate(AppRoutes.SETTINGS) },
                onOpenPinche = { navController.navigate(AppRoutes.PINCHE) },
                onOpenShoppingList = { navController.navigate(AppRoutes.SHOPPING_LIST) },
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
                onStartCooking = { recipeId, servings ->
                    navController.navigate(AppRoutes.cookMode(recipeId, servings))
                },
            )
        }
        // RF-CPP1 (RD22): pantalla real «Modo Cocina» que sustituye al destino
        // provisional; «Volver» y «Terminar» regresan al Detalle (D2/D7).
        composable(
            route = AppRoutes.COOK_MODE,
            arguments = listOf(
                navArgument(AppRoutes.RECIPE_ID_ARG) { type = NavType.StringType },
                navArgument(AppRoutes.SERVINGS_ARG) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) { entry ->
            CookModeRoute(
                recipeId = entry.arguments?.getString(AppRoutes.RECIPE_ID_ARG).orEmpty(),
                servings = entry.arguments?.getString(AppRoutes.SERVINGS_ARG)?.toIntOrNull(),
                onBack = { navController.popBackStack() },
                onFinish = { navController.popBackStack() },
            )
        }
        composable(AppRoutes.SCAN_PANTRY) {
            ScannerRoute(
                onBack = { navController.popBackStack() },
                // D5: `launchSingleTop` evita duplicar la Despensa en el back stack
                // cuando el Escáner se abre desde ella (MY_RECIPES → PANTRY →
                // SCAN_PANTRY); con la entrada desde Mis Recetas el comportamiento
                // no cambia.
                onSaved = {
                    navController.navigate(AppRoutes.PANTRY) {
                        popUpTo(AppRoutes.SCAN_PANTRY) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }
        // RF-SDR1/RF-SDR6: «Recetas con mi Despensa» sustituye a la sección que vivía
        // dentro del Escáner (RF-MRC5); Detalle y Sugerencias vuelven aquí (D7).
        composable(AppRoutes.PANTRY_RECIPES) {
            RecommendationsRoute(
                onBack = { navController.popBackStack() },
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
        // RF-PCH1: el Pinche se abre desde la top bar de Mis Recetas.
        composable(AppRoutes.PINCHE) {
            PincheRoute(
                onBack = { navController.popBackStack() },
                onOpenSettings = { navController.navigate(AppRoutes.SETTINGS) },
            )
        }
        // RD18: la Lista de la Compra se abre desde la top bar de Mis Recetas. Su
        // estado vacío ya no lleva al Escáner: el escaneo vive en la Despensa
        // (`mover-escaneo-a-la-despensa`, D2).
        composable(AppRoutes.SHOPPING_LIST) {
            ShoppingListRoute(
                onBack = { navController.popBackStack() },
            )
        }
        // La Despensa es donde se escanean los Alimentos (D1): su FAB y su estado
        // vacío abren el Escáner.
        composable(AppRoutes.PANTRY) {
            PantryRoute(
                onBack = { navController.popBackStack() },
                onScanFood = { navController.navigate(AppRoutes.SCAN_PANTRY) },
            )
        }
        composable(AppRoutes.SETTINGS) {
            SettingsRoute(onBack = { navController.popBackStack() })
        }
    }
}
