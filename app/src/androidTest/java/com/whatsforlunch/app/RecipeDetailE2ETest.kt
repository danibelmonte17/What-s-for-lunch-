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
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertTextEquals
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
import com.whatsforlunch.app.data.repository.InMemoryShoppingListRepository
import com.whatsforlunch.app.data.repository.RoomRecipeRepository
import com.whatsforlunch.app.domain.model.PantryItem
import com.whatsforlunch.app.domain.model.Recipe
import com.whatsforlunch.app.domain.model.RecipeIngredient
import com.whatsforlunch.app.domain.model.RecipeStep
import com.whatsforlunch.app.domain.repository.PantrySaveResult
import com.whatsforlunch.app.ui.cookmode.CookModeTestTags
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
// tarjeta con vuelta conservando la búsqueda, badge reactivo, borrador incompleto
// (con la CTA de Modo Cocina deshabilitada, RF-CPP5), CTA hacia el Modo Cocina
// real, no encontrado y checklist de marcado local.
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
        composeRule.onAllNodesWithText("4").assertCountEquals(2)
        composeRule.onNodeWithTag(RecipeDetailTestTags.SERVINGS_VALUE).assertTextEquals("4")

        // RF-DR5: checklist con contador, cantidades en español y píldora «Opcional».
        composeRule.onNodeWithTag(RecipeDetailTestTags.INGREDIENTS).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag(RecipeDetailTestTags.INGREDIENTS_COUNT)
            .performScrollTo()
            .assertIsDisplayed()
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
        composeRule.onNodeWithText("Sin ingredientes").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Sin pasos").performScrollTo().assertIsDisplayed()
        // RF-CPP5 (D3): con 0 Pasos la CTA «Empezar Modo Cocina» está deshabilitada.
        composeRule.onNodeWithTag(RecipeDetailTestTags.START_COOKING).assertIsNotEnabled()
        // RF-ER6: el bloque de raciones se muestra también sin ingredientes.
        composeRule.onNodeWithTag(RecipeDetailTestTags.SERVINGS_SCALER)
            .performScrollTo()
            .assertIsDisplayed()
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

        composeRule.onNodeWithTag(RecipeDetailTestTags.INGREDIENTS_COUNT).performScrollTo()
        composeRule.onAllNodesWithTag(RecipeDetailTestTags.INGREDIENT_CHECK)[0]
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithText("1 de 2").performScrollTo().assertIsDisplayed()
        composeRule.onAllNodesWithTag(RecipeDetailTestTags.INGREDIENT_CHECK)[1]
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithText("2 de 2").performScrollTo().assertIsDisplayed()

        // D3: al salir y volver a entrar, todas las filas están sin marcar.
        composeRule.onNodeWithText("Volver").performClick()
        composeRule.onNodeWithTag(MyRecipesTestTags.SCREEN).assertIsDisplayed()
        composeRule.onNodeWithTag(MyRecipesTestTags.RECIPE_BODY).performClick()
        composeRule.onNodeWithTag(RecipeDetailTestTags.SCREEN).assertIsDisplayed()

        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag(RecipeDetailTestTags.INGREDIENT_CHECK).fetchSemanticsNodes().size == 2
        }
        composeRule.onNodeWithText("0 de 2").performScrollTo().assertIsDisplayed()
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

    // S5 (RF-DR7, RF-CPP5): la CTA abre el Modo Cocina real (la Receta tiene al
    // menos 1 Paso) y la vuelta regresa al Detalle.
    @Test
    fun startCookingCtaOpensCookModeAndBack() {
        seedRoomRecipe(
            Recipe(
                id = "tortilla",
                title = "Tortilla de patatas",
                ingredients = listOf(
                    RecipeIngredient(id = "egg", name = "Huevos", quantity = 4.0, unit = "uds"),
                    RecipeIngredient(id = "potato", name = "Patatas", quantity = 700.0, unit = "g"),
                ),
                steps = listOf(
                    RecipeStep(
                        id = "s1",
                        text = "Corta las patatas",
                        timerMinutes = 10,
                        ingredientIds = listOf("potato"),
                    ),
                    RecipeStep(id = "s2", text = "Cuece la tortilla", ingredientIds = listOf("egg")),
                ),
            ),
        )
        launchFullApp()

        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag(MyRecipesTestTags.RECIPE_CARD).fetchSemanticsNodes().size == 1
        }
        composeRule.onNodeWithTag(MyRecipesTestTags.RECIPE_BODY).performClick()
        composeRule.onNodeWithTag(RecipeDetailTestTags.SCREEN).assertIsDisplayed()

        composeRule.onNodeWithTag(RecipeDetailTestTags.START_COOKING).performClick()

        composeRule.onNodeWithTag(CookModeTestTags.SCREEN).assertIsDisplayed()
        composeRule.onNodeWithText("Modo Cocina").assertIsDisplayed()
        composeRule.onNodeWithText("Paso 1 de 2").assertIsDisplayed()

        composeRule.onNodeWithText("Volver").performClick()
        composeRule.onNodeWithTag(RecipeDetailTestTags.SCREEN).assertIsDisplayed()
    }

    // RF-ER2 (verify 1): una Receta de 4 raciones baja a 2 y las cantidades se
    // reducen a la mitad sin tocar la Receta.
    @Test
    fun loweringServingsHalvesIngredientQuantities() {
        val recipes = InMemoryRecipeRepository()
        runBlocking {
            recipes.saveRecipe(
                Recipe(
                    id = "scaled",
                    title = "Bizcocho",
                    baseServings = 4,
                    ingredients = listOf(
                        RecipeIngredient(id = "flour", name = "Harina", quantity = 200.0, unit = "g"),
                        RecipeIngredient(id = "egg", name = "Huevos", quantity = 2.0, unit = "uds"),
                    ),
                ),
            )
        }

        launchDetail(recipes, InMemoryPantryRepository(), recipeId = "scaled")

        composeRule.onNodeWithTag(RecipeDetailTestTags.SERVINGS_SCALER)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("Raciones a preparar").assertIsDisplayed()
        composeRule.onNodeWithText("200 g Harina").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("2 uds Huevos").performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithTag(RecipeDetailTestTags.SERVINGS_DECREASE).performClick()
        composeRule.onNodeWithTag(RecipeDetailTestTags.SERVINGS_DECREASE).performClick()
        composeRule.onNodeWithTag(RecipeDetailTestTags.SERVINGS_VALUE).assertTextEquals("2")

        composeRule.onNodeWithText("100 g Harina").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("1 uds Huevos").performScrollTo().assertIsDisplayed()
    }

    // RF-ER2 y Q-escalar-raciones-1: el escalado se redondea a máximo 1 decimal.
    @Test
    fun scaledQuantitiesRoundToAtMostOneDecimal() {
        val recipes = InMemoryRecipeRepository()
        runBlocking {
            recipes.saveRecipe(
                Recipe(
                    id = "decimal",
                    title = "Revuelto",
                    baseServings = 4,
                    ingredients = listOf(
                        RecipeIngredient(id = "egg", name = "Huevo", quantity = 1.0, unit = "uds"),
                    ),
                ),
            )
        }

        launchDetail(recipes, InMemoryPantryRepository(), recipeId = "decimal")

        // 1 × (3 / 4) = 0,75 → «0,8».
        composeRule.onNodeWithTag(RecipeDetailTestTags.SERVINGS_DECREASE).performClick()
        composeRule.onNodeWithText("0,8 uds Huevo").performScrollTo().assertIsDisplayed()
    }

    // RF-ER3 (verify 2): en 1 el botón «−» y en 12 el «+» quedan deshabilitados.
    @Test
    fun servingsStepperIsDisabledAtLowerBound() {
        val recipes = InMemoryRecipeRepository()
        runBlocking {
            recipes.saveRecipe(
                Recipe(
                    id = "single",
                    title = "Ración única",
                    baseServings = 1,
                    ingredients = listOf(
                        RecipeIngredient(id = "soup", name = "Sopa", quantity = 250.0, unit = "ml"),
                    ),
                ),
            )
        }

        launchDetail(recipes, InMemoryPantryRepository(), recipeId = "single")

        composeRule.onNodeWithTag(RecipeDetailTestTags.SERVINGS_DECREASE).assertIsNotEnabled()
        composeRule.onNodeWithTag(RecipeDetailTestTags.SERVINGS_INCREASE).assertIsEnabled()
        composeRule.onNodeWithTag(RecipeDetailTestTags.SERVINGS_INCREASE).performClick()
        composeRule.onNodeWithTag(RecipeDetailTestTags.SERVINGS_DECREASE).assertIsEnabled()
        composeRule.onNodeWithTag(RecipeDetailTestTags.SERVINGS_DECREASE).performClick()
        composeRule.onNodeWithTag(RecipeDetailTestTags.SERVINGS_VALUE).assertTextEquals("1")
        composeRule.onNodeWithTag(RecipeDetailTestTags.SERVINGS_DECREASE).assertIsNotEnabled()
    }

    @Test
    fun servingsStepperIsDisabledAtUpperBound() {
        val recipes = InMemoryRecipeRepository()
        runBlocking {
            recipes.saveRecipe(
                Recipe(
                    id = "banquet",
                    title = "Banquete",
                    baseServings = 12,
                    ingredients = listOf(
                        RecipeIngredient(id = "rice", name = "Arroz", quantity = 1200.0, unit = "g"),
                    ),
                ),
            )
        }

        launchDetail(recipes, InMemoryPantryRepository(), recipeId = "banquet")

        composeRule.onNodeWithTag(RecipeDetailTestTags.SERVINGS_INCREASE).assertIsNotEnabled()
        composeRule.onNodeWithTag(RecipeDetailTestTags.SERVINGS_DECREASE).assertIsEnabled()
        composeRule.onNodeWithTag(RecipeDetailTestTags.SERVINGS_DECREASE).performClick()
        composeRule.onNodeWithTag(RecipeDetailTestTags.SERVINGS_INCREASE).assertIsEnabled()
        composeRule.onNodeWithTag(RecipeDetailTestTags.SERVINGS_INCREASE).performClick()
        composeRule.onNodeWithTag(RecipeDetailTestTags.SERVINGS_INCREASE).assertIsNotEnabled()
    }

    // RF-ER4: la selección no se persiste; al volver a abrir el Detalle se ven las
    // Raciones base y sus cantidades originales.
    @Test
    fun servingsSelectionIsNotPersistedWhenReopeningDetail() {
        seedRoomRecipe(
            Recipe(
                id = "scaled",
                title = "Bizcocho",
                baseServings = 4,
                ingredients = listOf(
                    RecipeIngredient(id = "flour", name = "Harina", quantity = 200.0, unit = "g"),
                ),
            ),
        )
        launchFullApp()

        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag(MyRecipesTestTags.RECIPE_CARD).fetchSemanticsNodes().size == 1
        }
        composeRule.onNodeWithTag(MyRecipesTestTags.RECIPE_BODY).performClick()
        composeRule.onNodeWithTag(RecipeDetailTestTags.SCREEN).assertIsDisplayed()

        composeRule.onNodeWithTag(RecipeDetailTestTags.SERVINGS_DECREASE).performClick()
        composeRule.onNodeWithTag(RecipeDetailTestTags.SERVINGS_DECREASE).performClick()
        composeRule.onNodeWithText("100 g Harina").performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithText("Volver").performClick()
        composeRule.onNodeWithTag(MyRecipesTestTags.SCREEN).assertIsDisplayed()
        composeRule.onNodeWithTag(MyRecipesTestTags.RECIPE_BODY).performClick()

        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag(RecipeDetailTestTags.INGREDIENT_ROW)
                .fetchSemanticsNodes().size == 1
        }
        composeRule.onNodeWithText("200 g Harina").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag(RecipeDetailTestTags.SERVINGS_VALUE).assertTextEquals("4")
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
        val viewModel = RecipeDetailViewModel(recipes, pantry, InMemoryShoppingListRepository(), recipeId)
        composeRule.setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            CompositionLocalProvider(
                LocalContext provides context,
                LocalConfiguration provides Configuration(context.resources.configuration),
                LocalResources provides context.resources,
            ) {
                WhatsForLunchTheme {
                    RecipeDetailScreen(
                        uiState = state,
                        onBack = onBack,
                        onStartCooking = { _, _ -> },
                        onServingsChange = viewModel::setServings,
                    )
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
