package com.whatsforlunch.app

import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.whatsforlunch.app.data.local.RecipeDatabase
import com.whatsforlunch.app.data.photo.PhotoStore
import com.whatsforlunch.app.data.repository.InMemoryPantryRepository
import com.whatsforlunch.app.data.repository.InMemoryRecipeRepository
import com.whatsforlunch.app.data.repository.RoomRecipeRepository
import com.whatsforlunch.app.domain.model.PantryItem
import com.whatsforlunch.app.domain.model.Recipe
import com.whatsforlunch.app.domain.model.RecipeIngredient
import com.whatsforlunch.app.domain.model.RecipeStep
import com.whatsforlunch.app.domain.repository.PantrySaveResult
import com.whatsforlunch.app.ui.myrecipes.MyRecipesTestTags
import com.whatsforlunch.app.ui.nav.WhatsForLunchApp
import com.whatsforlunch.app.ui.recipedetail.RecipeDetailScreen
import com.whatsforlunch.app.ui.recipedetail.RecipeDetailTestTags
import com.whatsforlunch.app.ui.recipedetail.RecipeDetailViewModel
import com.whatsforlunch.app.ui.theme.WhatsForLunchTheme
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Locale
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext

// Almacén de fotos sin uso: los tests solo guardan Recetas sin archivo real.
private object NoOpPhotoStore : PhotoStore {
    override suspend fun save(recipeId: String, bytes: ByteArray): String = "photo://$recipeId"

    override suspend fun delete(path: String) = Unit
}

// verify 1 y S2 a S7 de ver-detalle-de-receta: ficha completa, apertura desde la
// tarjeta con vuelta conservando la búsqueda, badge reactivo, borrador incompleto,
// CTA hacia Modo Cocina, no encontrado y checklist de marcado local.
@RunWith(AndroidJUnit4::class)
class RecipeDetailE2ETest {

    @get:Rule
    val composeRule = createComposeRule()

    @Before
    fun clearDatabase() {
        E2eDatabase.clear()
    }

    // S1: Receta con foto, 2 obligatorios + 1 opcional y 2 Pasos (uno con
    // temporizador) → métricas con desglose, checklist con «Opcional» y chips.
    @Test
    fun fullRecipeShowsMetricsChecklistAndSteps() {
        val recipes = InMemoryRecipeRepository()
        val pantry = InMemoryPantryRepository()
        val photoFile = writePhoto()
        runBlocking {
            recipes.saveRecipe(
                Recipe(
                    id = "full",
                    title = "Tortilla de patatas",
                    story = "La receta de la abuela.",
                    prepMinutes = 15,
                    cookMinutes = 30,
                    baseServings = 4,
                    caloriesPerServing = 450,
                    photoPath = photoFile.absolutePath,
                    ingredients = listOf(
                        RecipeIngredient(id = "egg", name = "Huevos", quantity = 4.0, unit = "uds"),
                        RecipeIngredient(id = "potato", name = "Patatas", quantity = 0.5, unit = "kg"),
                        RecipeIngredient(id = "oil", name = "Aceite", quantity = 0.25, unit = "l", optional = true),
                    ),
                    steps = listOf(
                        RecipeStep(id = "s1", text = "Corta las patatas", timerMinutes = 10),
                        RecipeStep(id = "s2", text = "Cuece la tortilla"),
                    ),
                ),
            )
        }

        launchDetail(recipes, pantry, recipeId = "full")

        composeRule.onNodeWithTag(RecipeDetailTestTags.HERO).assertIsDisplayed()
        composeRule.onNodeWithText("Tortilla de patatas").assertIsDisplayed()
        composeRule.onNodeWithText("La receta de la abuela.").assertIsDisplayed()

        // RF-DR4: métricas con tiempo total, desglose, dificultad, calorías y raciones.
        composeRule.onNodeWithTag(RecipeDetailTestTags.TOTAL_TIME).assertIsDisplayed()
        composeRule.onNodeWithText("45 min").assertIsDisplayed()
        composeRule.onNodeWithText("15 min preparación · 30 min cocción").assertIsDisplayed()
        composeRule.onNodeWithTag(RecipeDetailTestTags.DIFFICULTY).assertIsDisplayed()
        composeRule.onNodeWithText("Media").assertIsDisplayed()
        composeRule.onNodeWithTag(RecipeDetailTestTags.CALORIES).assertIsDisplayed()
        composeRule.onNodeWithText("450").assertIsDisplayed()
        composeRule.onNodeWithTag(RecipeDetailTestTags.SERVINGS).assertIsDisplayed()
        composeRule.onNodeWithText("4").assertIsDisplayed()

        // RF-DR5: checklist con contador, cantidades en español y píldora «Opcional».
        composeRule.onNodeWithTag(RecipeDetailTestTags.INGREDIENTS).assertIsDisplayed()
        composeRule.onNodeWithTag(RecipeDetailTestTags.INGREDIENTS_COUNT).assertIsDisplayed()
        composeRule.onNodeWithText("0 de 3").assertIsDisplayed()
        composeRule.onNodeWithText("4 uds Huevos").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("0,5 kg Patatas").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("0,25 l Aceite").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Opcional").performScrollTo().assertIsDisplayed()
        composeRule.onAllNodesWithTag(RecipeDetailTestTags.INGREDIENT_ROW).assertCountEquals(3)

        // RF-DR6: dos pasos con su chip de temporizador.
        composeRule.onAllNodesWithTag(RecipeDetailTestTags.STEP_ROW).assertCountEquals(2)
        composeRule.onNodeWithText("Corta las patatas").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Cuece la tortilla").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag(RecipeDetailTestTags.STEP_TIMER).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("10 min").performScrollTo().assertIsDisplayed()

        // RF-DR7: CTA destacada visible.
        composeRule.onNodeWithTag(RecipeDetailTestTags.START_COOKING).assertIsDisplayed()
        composeRule.onNodeWithText("Empezar Modo Cocina").assertIsDisplayed()
    }

    // S3 (RF-DR2): badge «50%» que pasa a «100% Match» al completar la Despensa
    // sin salir de la pantalla.
    @Test
    fun matchBadgeReactsToPantryChanges() {
        val recipes = InMemoryRecipeRepository()
        val pantry = InMemoryPantryRepository()
        runBlocking {
            recipes.saveRecipe(
                Recipe(
                    id = "partial",
                    title = "Pollo con arroz",
                    ingredients = listOf(
                        RecipeIngredient(id = "chicken", name = "Pollo"),
                        RecipeIngredient(id = "rice", name = "Arroz"),
                    ),
                ),
            )
            assertEquals(PantrySaveResult.Saved, pantry.savePantryItem(PantryItem("chicken", "Pollo")))
        }

        launchDetail(recipes, pantry, recipeId = "partial")

        composeRule.onNodeWithTag(RecipeDetailTestTags.MATCH).assertIsDisplayed()
        composeRule.onNodeWithText("50%").assertIsDisplayed()

        runBlocking { assertEquals(PantrySaveResult.Saved, pantry.savePantryItem(PantryItem("rice", "Arroz"))) }

        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText("100% Match").fetchSemanticsNodes().size == 1
        }
    }

    // S4: borrador incompleto → placeholder sin foto, sin historia, «—» en
    // calorías, «Sin ingredientes» y «Sin pasos».
    @Test
    fun incompleteDraftShowsEmptyStates() {
        val recipes = InMemoryRecipeRepository()
        runBlocking {
            recipes.saveRecipe(Recipe(id = "draft", title = "Borrador vacío"))
        }

        launchDetail(recipes, InMemoryPantryRepository(), recipeId = "draft")

        composeRule.onNodeWithText("Borrador vacío").assertIsDisplayed()
        composeRule.onAllNodesWithText("La receta de la abuela.").assertCountEquals(0)
        composeRule.onNodeWithTag(RecipeDetailTestTags.CALORIES).assertIsDisplayed()
        composeRule.onNodeWithText("—").assertIsDisplayed()
        composeRule.onNodeWithText("Sin ingredientes").assertIsDisplayed()
        composeRule.onNodeWithText("Sin pasos").assertIsDisplayed()
        // Sin obligatorios no hay badge de Coincidencia (RF-MB6).
        composeRule.onAllNodesWithTag(RecipeDetailTestTags.MATCH).assertCountEquals(0)
    }

    // S6 (RF-DR8): id inexistente → «Receta no encontrada» con acción de vuelta.
    @Test
    fun unknownIdShowsNotFoundWithBackAction() {
        var backClicks = 0
        launchDetail(
            recipes = InMemoryRecipeRepository(),
            pantry = InMemoryPantryRepository(),
            recipeId = "missing",
            onBack = { backClicks += 1 },
        )

        composeRule.onNodeWithTag(RecipeDetailTestTags.NOT_FOUND).assertIsDisplayed()
        composeRule.onNodeWithText("Receta no encontrada").assertIsDisplayed()
        composeRule.onNodeWithTag(RecipeDetailTestTags.NOT_FOUND_BACK).performClick()
        assertEquals(1, backClicks)
    }

    // S7 (RF-DR5, D3): el checklist marca y desmarca con su contador «N de M» y el
    // marcado es local: al salir y volver, todas las filas quedan sin marcar.
    @Test
    fun checklistMarksRowsAndResetsWhenReopening() {
        seedRoomRecipe(
            Recipe(
                id = "checklist",
                title = "Ensalada mixta",
                ingredients = listOf(
                    RecipeIngredient(id = "tomato", name = "Tomate"),
                    RecipeIngredient(id = "onion", name = "Cebolla"),
                ),
            ),
        )
        launchFullApp()

        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag(MyRecipesTestTags.RECIPE_CARD).fetchSemanticsNodes().size == 1
        }
        composeRule.onNodeWithTag(MyRecipesTestTags.RECIPE_BODY).performClick()
        composeRule.onNodeWithTag(RecipeDetailTestTags.SCREEN).assertIsDisplayed()

        composeRule.onAllNodesWithTag(RecipeDetailTestTags.INGREDIENT_CHECK)[0].performClick()
        composeRule.onNodeWithText("1 de 2").assertIsDisplayed()
        composeRule.onAllNodesWithTag(RecipeDetailTestTags.INGREDIENT_CHECK)[1].performClick()
        composeRule.onNodeWithText("2 de 2").assertIsDisplayed()

        // D3: al salir y volver a entrar, todas las filas están sin marcar.
        composeRule.onNodeWithText("Volver").performClick()
        composeRule.onNodeWithTag(MyRecipesTestTags.SCREEN).assertIsDisplayed()
        composeRule.onNodeWithTag(MyRecipesTestTags.RECIPE_BODY).performClick()
        composeRule.onNodeWithTag(RecipeDetailTestTags.SCREEN).assertIsDisplayed()

        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag(RecipeDetailTestTags.INGREDIENT_CHECK).fetchSemanticsNodes().size == 2
        }
        composeRule.onNodeWithText("0 de 2").assertIsDisplayed()
        composeRule.onAllNodesWithTag(RecipeDetailTestTags.INGREDIENT_CHECK)[0].assertIsOff()
        composeRule.onAllNodesWithTag(RecipeDetailTestTags.INGREDIENT_CHECK)[1].assertIsOff()
    }

    // S2 (RF-DR1): tap en el cuerpo de la tarjeta → Detalle; «Volver» regresa a
    // Mis Recetas con la búsqueda conservada. Navegación completa con la app real.
    @Test
    fun tappingCardBodyOpensDetailAndBackKeepsSearch() {
        seedRoomRecipe(
            Recipe(
                id = "tortilla",
                title = "Tortilla de patatas",
                ingredients = listOf(RecipeIngredient(id = "egg", name = "Huevos")),
            ),
        )
        seedRoomRecipe(
            Recipe(
                id = "pollo",
                title = "Pollo asado",
                ingredients = listOf(RecipeIngredient(id = "chicken", name = "Pollo")),
            ),
        )
        launchFullApp()

        composeRule.onNodeWithTag(MyRecipesTestTags.SEARCH_FIELD).performClick()
        composeRule.onNodeWithTag(MyRecipesTestTags.SEARCH_FIELD).performTextReplacement("Tortilla")
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag(MyRecipesTestTags.RECIPE_CARD).fetchSemanticsNodes().size == 1
        }

        composeRule.onNodeWithTag(MyRecipesTestTags.RECIPE_BODY).performClick()

        composeRule.onNodeWithTag(RecipeDetailTestTags.SCREEN).assertIsDisplayed()
        composeRule.onNodeWithText("Tortilla de patatas").assertIsDisplayed()

        composeRule.onNodeWithText("Volver").performClick()

        composeRule.onNodeWithTag(MyRecipesTestTags.SCREEN).assertIsDisplayed()
        composeRule.onNodeWithText("Tortilla de patatas").assertIsDisplayed()
        composeRule.onAllNodesWithTag(MyRecipesTestTags.RECIPE_CARD).assertCountEquals(1)
    }

    // S5 (RF-DR7, D4): la CTA abre el destino provisional de Modo Cocina y la
    // vuelta regresa al Detalle.
    @Test
    fun startCookingCtaOpensProvisionalCookModeAndBack() {
        seedRoomRecipe(
            Recipe(
                id = "tortilla",
                title = "Tortilla de patatas",
                ingredients = listOf(RecipeIngredient(id = "egg", name = "Huevos")),
            ),
        )
        launchFullApp()

        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag(MyRecipesTestTags.RECIPE_CARD).fetchSemanticsNodes().size == 1
        }
        composeRule.onNodeWithTag(MyRecipesTestTags.RECIPE_BODY).performClick()
        composeRule.onNodeWithTag(RecipeDetailTestTags.SCREEN).assertIsDisplayed()

        composeRule.onNodeWithTag(RecipeDetailTestTags.START_COOKING).performClick()

        composeRule.onNodeWithTag(com.whatsforlunch.app.ui.nav.ProvisionalTestTags.COOK_MODE_SCREEN)
            .assertIsDisplayed()
        composeRule.onNodeWithText("Modo Cocina").assertIsDisplayed()

        composeRule.onNodeWithText("Volver").performClick()
        composeRule.onNodeWithTag(RecipeDetailTestTags.SCREEN).assertIsDisplayed()
    }

    private fun spanishContext() = InstrumentationRegistry.getInstrumentation().targetContext
        .let { baseContext ->
            baseContext.createConfigurationContext(
                Configuration(baseContext.resources.configuration).apply {
                    setLocale(Locale("es", "ES"))
                },
            )
        }

    private fun launchDetail(
        recipes: InMemoryRecipeRepository,
        pantry: InMemoryPantryRepository,
        recipeId: String,
        onBack: () -> Unit = {},
    ) {
        val context = spanishContext()
        // Fuera del lambda de composición: lint prohíbe construir ViewModels dentro
        // de un composable (patrón de MatchBadgeE2ETest).
        val viewModel = RecipeDetailViewModel(recipes, pantry, recipeId)
        composeRule.setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            CompositionLocalProvider(
                LocalContext provides context,
                LocalConfiguration provides Configuration(context.resources.configuration),
                LocalResources provides context.resources,
            ) {
                WhatsForLunchTheme {
                    RecipeDetailScreen(uiState = state, onBack = onBack, onStartCooking = {})
                }
            }
        }
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag(RecipeDetailTestTags.SCREEN).fetchSemanticsNodes().size == 1
        }
    }

    private fun launchFullApp() {
        val baseContext = InstrumentationRegistry.getInstrumentation().targetContext
        val configuration = Configuration(baseContext.resources.configuration).apply {
            setLocale(Locale("es", "ES"))
        }
        val context = baseContext.createConfigurationContext(configuration)
        composeRule.setContent {
            CompositionLocalProvider(
                LocalContext provides context,
                LocalConfiguration provides Configuration(context.resources.configuration),
                LocalResources provides context.resources,
            ) {
                WhatsForLunchTheme {
                    WhatsForLunchApp()
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun seedRoomRecipe(recipe: Recipe) {
        val database = GlobalContext.get().get<RecipeDatabase>()
        val repository = RoomRecipeRepository(database.recipeDao(), NoOpPhotoStore)
        runBlocking { repository.saveRecipe(recipe) }
    }

    private fun writePhoto(): File {
        val bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        val output = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        bitmap.recycle()
        val photoFile = File(
            InstrumentationRegistry.getInstrumentation().targetContext.cacheDir,
            "recipe-detail-photo-${System.nanoTime()}.png",
        )
        photoFile.writeBytes(output.toByteArray())
        return photoFile
    }
}
