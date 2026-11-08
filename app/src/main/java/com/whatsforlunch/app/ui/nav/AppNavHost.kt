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

// RF-MOB4 (D5): la navegación por pestañas deja el back stack [my_recipes, pestaña
// actual]; `popUpTo` descarta la pestaña anterior y `launchSingleTop` evita
// duplicar entradas (back repetido nunca apila otra Mis Recetas). Mis Recetas
// vuelve a su entrada existente y conserva búsqueda, momento y «Favoritas».
private fun NavHostController.openTab(tab: AppTab) {
    navigate(tab.route) {
        popUpTo(AppRoutes.MY_RECIPES) { inclusive = false }
        launchSingleTop = true
    }
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
                onOpenSettings = { navController.navigate(AppRoutes.SETTINGS) },
                onTabSelected = { tab -> navController.openTab(tab) },
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
                onSaved = {
                    // RF-MOB4 (D5): tras guardar se aterriza en la pestaña Despensa
                    // con el back stack [my_recipes, pantry]; el Escáner no queda en
                    // la pila y el back vuelve a Mis Recetas.
                    navController.openTab(AppTab.PANTRY)
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
        // RF-PCH1: el Pinche es una pestaña de la barra inferior (RF-MOB1).
        composable(AppRoutes.PINCHE) {
            PincheRoute(
                onOpenSettings = { navController.navigate(AppRoutes.SETTINGS) },
                onTabSelected = { tab -> navController.openTab(tab) },
            )
        }
        // RD18: la Lista de la Compra es una pestaña de la barra inferior y su
        // estado vacío lleva al Escáner (RF-AFL6/RF-AFL7).
        composable(AppRoutes.SHOPPING_LIST) {
            ShoppingListRoute(
                onScanPantry = { navController.navigate(AppRoutes.SCAN_PANTRY) },
                onTabSelected = { tab -> navController.openTab(tab) },
            )
        }
        composable(AppRoutes.PANTRY) {
            PantryRoute(onTabSelected = { tab -> navController.openTab(tab) })
        }
        composable(AppRoutes.SETTINGS) {
            SettingsRoute(onBack = { navController.popBackStack() })
        }
    }
}
