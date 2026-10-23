package com.whatsforlunch.app

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.annotation.StringRes
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.filter
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.whatsforlunch.app.data.photo.PhotoAcquirer
import com.whatsforlunch.app.data.photo.PhotoResult
import com.whatsforlunch.app.data.photo.PhotoStore
import com.whatsforlunch.app.domain.model.RecipeStatus
import com.whatsforlunch.app.domain.repository.RecipeRepository
import com.whatsforlunch.app.data.repository.InMemoryRecipeRepository
import com.whatsforlunch.app.domain.myrecipes.MyRecipesState
import com.whatsforlunch.app.domain.model.Difficulty
import com.whatsforlunch.app.domain.model.MealMoment
import com.whatsforlunch.app.domain.model.Recipe
import com.whatsforlunch.app.ui.createrecipe.CreateRecipeScreen
import com.whatsforlunch.app.ui.createrecipe.CreateRecipeTestTags
import com.whatsforlunch.app.ui.createrecipe.CreateRecipeViewModel
import com.whatsforlunch.app.ui.createrecipe.toActions
import com.whatsforlunch.app.ui.myrecipes.MyRecipesScreen
import com.whatsforlunch.app.ui.myrecipes.MyRecipesTestTags
import com.whatsforlunch.app.ui.myrecipes.MyRecipesUiState
import com.whatsforlunch.app.ui.scanner.ScannerTestTags
import com.whatsforlunch.app.ui.theme.WhatsForLunchTheme
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Locale
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// Fakes: la foto se sustituye por un fake que inyecta bytes de prueba y la IA por
// resultados prefijados (Parte B, T10). El almacén de fotos escribe el archivo de
// verdad (en la caché de la instrumentación) para que la vista previa de la foto
// aceptada sea real en el test de RF-20.
private class FakePhotoStore : PhotoStore {
    private val dir = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir, "crear-receta-e2e")

    override suspend fun save(recipeId: String, bytes: ByteArray): String {
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, "$recipeId.jpg")
        file.writeBytes(bytes)
        return file.absolutePath
    }

    override suspend fun delete(path: String) {
        val file = File(path)
        if (file.exists()) file.delete()
    }
}

// Imagen válida y decodificable (PNG 1×1) para el escenario de foto generada.
private fun validImageBytes(): ByteArray {
    val bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    val output = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
    bitmap.recycle()
    return output.toByteArray()
}

private class FakePhotoAcquirer(
    private val gallery: PhotoResult = PhotoResult.Cancelled,
    private val camera: PhotoResult = PhotoResult.Cancelled,
    private val generation: PhotoResult = PhotoResult.Cancelled,
) : PhotoAcquirer {
    override suspend fun pickFromGallery(): PhotoResult = gallery

    override suspend fun takePhoto(): PhotoResult = camera

    override suspend fun generate(prompt: String): PhotoResult = generation
}

// verify 1 · RF-1 a RF-26: el formulario de alta montado sobre un repositorio en
// memoria con fakes de foto e IA. Sin depender de pantallas del sistema.
@RunWith(AndroidJUnit4::class)
class CreateRecipeE2ETest {

    @get:Rule
    val composeRule = createComposeRule()

    // El locale se inyecta (mismo patrón que MyRecipesLocalizationE2ETest) para que
    // las aserciones sobre textos no dependan del idioma del dispositivo (NFR-1).
    private lateinit var formContext: Context

    private fun formText(@StringRes resId: Int): String = formContext.getString(resId)

    private fun launchForm(
        repository: RecipeRepository = InMemoryRecipeRepository(),
        photoAcquirer: PhotoAcquirer = FakePhotoAcquirer(),
        locale: Locale = Locale("es", "ES"),
    ): RecipeRepository {
        val baseContext = InstrumentationRegistry.getInstrumentation().targetContext
        val configuration = Configuration(baseContext.resources.configuration).apply { setLocale(locale) }
        formContext = baseContext.createConfigurationContext(configuration)
        val viewModel = CreateRecipeViewModel(repository, FakePhotoStore())
        viewModel.setPhotoAcquirer(photoAcquirer)
        composeRule.setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            CompositionLocalProvider(LocalContext provides formContext) {
                WhatsForLunchTheme {
                    CreateRecipeScreen(uiState = state, actions = viewModel.toActions {})
                }
            }
        }
        return repository
    }

    private fun typeTitle(title: String) {
        composeRule.onNodeWithTag(CreateRecipeTestTags.TITLE_FIELD).performTextInput(title)
    }

    private fun addValidIngredient(index: Int = 0, name: String = "Patata", quantity: String = "700") {
        composeRule.onNodeWithTag(CreateRecipeTestTags.ADD_INGREDIENT).performScrollTo().performClick()
        composeRule.onNodeWithTag(CreateRecipeTestTags.ingredientName(index)).performScrollTo().performTextInput(name)
        composeRule.onNodeWithTag(CreateRecipeTestTags.ingredientQuantity(index)).performScrollTo().performTextInput(quantity)
    }

    private fun addValidStep(index: Int = 0, text: String = "Pochar la cebolla") {
        composeRule.onNodeWithTag(CreateRecipeTestTags.ADD_STEP).performScrollTo().performClick()
        composeRule.onNodeWithTag(CreateRecipeTestTags.stepText(index)).performScrollTo().performTextInput(text)
    }

    @Test
    fun newFormPreselectsLunch() {
        launchForm()
        composeRule.onNodeWithTag(CreateRecipeTestTags.MOMENT_LUNCH).performScrollTo().assertIsSelected()
    }

    @Test
    fun draftActionShowsOnlyItsLabel() {
        launchForm()
        composeRule.onNodeWithTag(CreateRecipeTestTags.SAVE_DRAFT)
            .assertTextContains(formText(R.string.create_recipe_save_draft))
    }

    @Test
    fun addAndRemoveIngredientRows() {
        launchForm()
        addValidIngredient()
        composeRule.onNodeWithTag(CreateRecipeTestTags.ingredientName(0)).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag(CreateRecipeTestTags.removeIngredient(0)).performScrollTo().performClick()
        composeRule.onNodeWithTag(CreateRecipeTestTags.ADD_INGREDIENT).performScrollTo().assertIsDisplayed()
    }

    // RF-10 y RF-11: modificar y eliminar cualquier Paso existente.
    @Test
    fun editAndRemoveStepRows() {
        launchForm()
        addValidStep(index = 0, text = "Pochar la cebolla")
        addValidStep(index = 1, text = "Añadir el huevo")
        composeRule.onNodeWithTag(CreateRecipeTestTags.stepText(1)).performScrollTo()
            .performTextReplacement("Batir el huevo")
        composeRule.onNodeWithTag(CreateRecipeTestTags.stepTimer(1)).performScrollTo().performTextInput("5")
        composeRule.onNodeWithTag(CreateRecipeTestTags.stepText(1)).assertTextContains("Batir el huevo")
        composeRule.onNodeWithTag(CreateRecipeTestTags.stepTimer(1)).assertTextContains("5")
        composeRule.onNodeWithTag(CreateRecipeTestTags.removeStep(0)).performScrollTo().performClick()
        composeRule.onNodeWithTag(CreateRecipeTestTags.stepText(0)).performScrollTo().assertTextContains("Batir el huevo")
        composeRule.onNodeWithTag(CreateRecipeTestTags.stepTimer(0)).assertTextContains("5")
        composeRule.onNodeWithTag(CreateRecipeTestTags.removeStep(0)).performScrollTo().performClick()
        composeRule.onAllNodesWithTag(CreateRecipeTestTags.stepText(0)).assertCountEquals(0)
        composeRule.onNodeWithTag(CreateRecipeTestTags.ADD_STEP).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun saveDraftIsBlockedWithoutTitle() {
        val repository = launchForm()
        composeRule.onNodeWithTag(CreateRecipeTestTags.SAVE_DRAFT).performClick()
        composeRule.onNodeWithText(formText(R.string.create_recipe_title_required)).performScrollTo().assertIsDisplayed()
        assertTrue(runBlocking { repository.observeRecipes().first() }.isEmpty())
    }

    // NFR-1: los mismos errores de bloqueo se afirman también en inglés, sin depender
    // del idioma del dispositivo de validación. Con Publicar deshabilitado, los
    // requisitos incumplidos se enumeran junto al dock (RF-15 de la fidelidad visual).
    @Test
    fun blockingErrorsAreLocalizedInEnglish() {
        val repository = launchForm(locale = Locale.ENGLISH)
        composeRule.onNodeWithTag(CreateRecipeTestTags.SAVE_DRAFT).performClick()
        composeRule.onNodeWithText(formText(R.string.create_recipe_title_required)).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag(CreateRecipeTestTags.PUBLISH).assertIsNotEnabled()
        composeRule.onNodeWithText(formText(R.string.create_recipe_publish_error_ingredient)).assertIsDisplayed()
        composeRule.onNodeWithText(formText(R.string.create_recipe_publish_error_step)).assertIsDisplayed()
        assertTrue(runBlocking { repository.observeRecipes().first() }.isEmpty())
    }

    @Test
    fun saveDraftWithoutPhotoSavesDraft() {
        val repository = launchForm()
        typeTitle("Borrador sin foto")
        composeRule.onNodeWithTag(CreateRecipeTestTags.SAVE_DRAFT).performClick()
        val recipes = runBlocking { repository.observeRecipes().first() }
        assertEquals(1, recipes.size)
        assertEquals(RecipeStatus.DRAFT, recipes.first().status)
    }

    @Test
    fun publishWithoutPhotoIsBlocked() {
        val repository = launchForm()
        typeTitle("Sin foto")
        addValidIngredient()
        addValidStep()
        // RF-14/RF-15 de la fidelidad visual: Publicar queda deshabilitado y el
        // requisito incumplido se enumera junto al dock, sin guardar nada.
        composeRule.onNodeWithTag(CreateRecipeTestTags.PUBLISH).assertIsNotEnabled()
        composeRule.onNodeWithText(formText(R.string.create_recipe_publish_error_photo)).assertIsDisplayed()
        assertTrue(runBlocking { repository.observeRecipes().first() }.isEmpty())
    }

    @Test
    fun publishWithoutIngredientIsBlocked() {
        val repository = launchForm(photoAcquirer = FakePhotoAcquirer(gallery = PhotoResult.Success(byteArrayOf(1, 2, 3))))
        typeTitle("Sin ingrediente")
        composeRule.onNodeWithTag(CreateRecipeTestTags.PHOTO_GALLERY).performScrollTo().performClick()
        addValidStep()
        composeRule.onNodeWithTag(CreateRecipeTestTags.PUBLISH).assertIsNotEnabled()
        composeRule.onNodeWithText(formText(R.string.create_recipe_publish_error_ingredient)).assertIsDisplayed()
        assertTrue(runBlocking { repository.observeRecipes().first() }.isEmpty())
    }

    @Test
    fun publishWithPhotoSavesPublishedRecipe() {
        val repository = launchForm(photoAcquirer = FakePhotoAcquirer(gallery = PhotoResult.Success(byteArrayOf(1, 2, 3))))
        typeTitle("Tortilla publicada")
        composeRule.onNodeWithTag(CreateRecipeTestTags.PHOTO_GALLERY).performScrollTo().performClick()
        addValidIngredient()
        addValidStep()
        composeRule.onNodeWithTag(CreateRecipeTestTags.PUBLISH).assertIsEnabled()
        composeRule.onNodeWithTag(CreateRecipeTestTags.PUBLISH).performClick()
        val recipes = runBlocking { repository.observeRecipes().first() }
        assertEquals(1, recipes.size)
        assertEquals(RecipeStatus.PUBLISHED, recipes.first().status)
        assertEquals("Tortilla publicada", recipes.first().title)
    }

    // RF-14 y RF-16 de la fidelidad visual: Publicar está deshabilitado mientras
    // falte algún requisito y se habilita cuando el formulario los cumple.
    @Test
    fun publishIsDisabledUntilRequirementsAreMet() {
        launchForm(photoAcquirer = FakePhotoAcquirer(gallery = PhotoResult.Success(byteArrayOf(1, 2, 3))))
        composeRule.onNodeWithTag(CreateRecipeTestTags.PUBLISH).assertIsNotEnabled()
        composeRule.onNodeWithTag(CreateRecipeTestTags.PUBLISH_ERRORS).assertIsDisplayed()
        composeRule.onNodeWithText(formText(R.string.create_recipe_publish_error_title)).assertIsDisplayed()
        composeRule.onNodeWithText(formText(R.string.create_recipe_publish_error_photo)).assertIsDisplayed()
        typeTitle("Con requisitos")
        addValidIngredient()
        addValidStep()
        composeRule.onNodeWithTag(CreateRecipeTestTags.PHOTO_GALLERY).performScrollTo().performClick()
        composeRule.onNodeWithTag(CreateRecipeTestTags.PUBLISH).assertIsEnabled()
        composeRule.onAllNodesWithTag(CreateRecipeTestTags.PUBLISH_ERRORS).assertCountEquals(0)
    }

    // RF-15 de la fidelidad visual: las filas de Ingrediente inválidas también se
    // enumeran junto al dock como requisito que impide Publicar.
    @Test
    fun invalidIngredientRowsAreListedAsBlockingRequirement() {
        launchForm()
        typeTitle("Con fila inválida")
        composeRule.onNodeWithTag(CreateRecipeTestTags.ADD_INGREDIENT).performScrollTo().performClick()
        composeRule.onNodeWithTag(CreateRecipeTestTags.PUBLISH).assertIsNotEnabled()
        composeRule.onNodeWithText(formText(R.string.create_recipe_publish_error_invalid_ingredient)).assertIsDisplayed()
    }

    @Test
    fun generatePhotoWithoutApiKeyShowsNotice() {
        launchForm(photoAcquirer = FakePhotoAcquirer(generation = PhotoResult.MissingApiKey))
        composeRule.onNodeWithTag(CreateRecipeTestTags.PHOTO_GENERATE).performScrollTo().performClick()
        composeRule.onNodeWithTag(CreateRecipeTestTags.NOTICE).performScrollTo().assertIsDisplayed()
    }

    // RF-20: la imagen generada se ofrece para aceptarla o descartarla.
    @Test
    fun generatedPhotoCanBeAcceptedOrDiscarded() {
        launchForm(photoAcquirer = FakePhotoAcquirer(generation = PhotoResult.Success(validImageBytes())))
        composeRule.onNodeWithTag(CreateRecipeTestTags.PHOTO_GENERATE).performScrollTo().performClick()
        composeRule.onNodeWithTag(CreateRecipeTestTags.PHOTO_PREVIEW).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag(CreateRecipeTestTags.PHOTO_REMOVE).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag(CreateRecipeTestTags.PHOTO_REMOVE).performScrollTo().performClick()
        composeRule.onAllNodesWithTag(CreateRecipeTestTags.PHOTO_PREVIEW).assertCountEquals(0)
        composeRule.onNodeWithTag(CreateRecipeTestTags.PHOTO_GALLERY).performScrollTo().assertIsDisplayed()
    }

    // RF-22 y RD05: el fallo de generación avisa sin bloquear la foto manual ni el guardado.
    @Test
    fun generationFailureShowsNoticeAndKeepsManualOptions() {
        val repository = launchForm(photoAcquirer = FakePhotoAcquirer(generation = PhotoResult.GenerationFailed))
        typeTitle("Tortilla sin foto generada")
        composeRule.onNodeWithTag(CreateRecipeTestTags.PHOTO_GENERATE).performScrollTo().performClick()
        composeRule.onNodeWithTag(CreateRecipeTestTags.NOTICE).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(formText(R.string.create_recipe_notice_ai_failed)).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag(CreateRecipeTestTags.PHOTO_GALLERY).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag(CreateRecipeTestTags.PHOTO_CAMERA).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag(CreateRecipeTestTags.SAVE_DRAFT).performClick()
        assertEquals(RecipeStatus.DRAFT, runBlocking { repository.observeRecipes().first() }.single().status)
    }

    @Test
    fun takePhotoWithDeniedPermissionShowsNotice() {
        launchForm(photoAcquirer = FakePhotoAcquirer(camera = PhotoResult.CameraPermissionDenied))
        composeRule.onNodeWithTag(CreateRecipeTestTags.PHOTO_CAMERA).performScrollTo().performClick()
        composeRule.onNodeWithTag(CreateRecipeTestTags.NOTICE).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun favoriteToggleFlipsState() {
        launchForm()
        composeRule.onNodeWithTag(CreateRecipeTestTags.FAVORITE_TOGGLE).performScrollTo().assertIsOff()
        composeRule.onNodeWithTag(CreateRecipeTestTags.FAVORITE_TOGGLE).performClick()
        composeRule.onNodeWithTag(CreateRecipeTestTags.FAVORITE_TOGGLE).assertIsOn()
    }

    // RF-21 de la fidelidad visual (actualizado 2026-10-19): la cabecera ya no dibuja
    // «Guardar»; el único guardado es «Borrador» del dock.
    @Test
    fun headerHasNoSaveActionAndDockSavesDraft() {
        val repository = launchForm()
        composeRule.onAllNodesWithTag(CreateRecipeTestTags.HEADER_SAVE).assertCountEquals(0)
        typeTitle("Borrador desde el dock")
        composeRule.onNodeWithTag(CreateRecipeTestTags.SAVE_DRAFT).performClick()
        val recipes = runBlocking { repository.observeRecipes().first() }
        assertEquals(1, recipes.size)
        assertEquals(RecipeStatus.DRAFT, recipes.first().status)
        assertEquals("Borrador desde el dock", recipes.first().title)
    }

    // RF-1 a RF-8 de la fidelidad visual: están los bloques y controles del
    // prototipo (cabecera con distintivo, portada con controles de foto,
    // información básica con chips de momento, rejilla de métricas, ingredientes,
    // pasos con número en círculo, favorito y dock).
    @Test
    fun formShowsPrototypeBlocksAndControls() {
        launchForm()
        composeRule.onAllNodesWithTag(CreateRecipeTestTags.HEADER_SAVE).assertCountEquals(0)
        composeRule.onNodeWithTag(CreateRecipeTestTags.STATUS_BADGE).assertIsDisplayed()
        composeRule.onNodeWithText(formText(R.string.create_recipe_header_subtitle)).assertIsDisplayed()
        composeRule.onNodeWithTag(CreateRecipeTestTags.COVER).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag(CreateRecipeTestTags.PHOTO_GALLERY).assertIsDisplayed()
        composeRule.onNodeWithTag(CreateRecipeTestTags.PHOTO_CAMERA).assertIsDisplayed()
        composeRule.onNodeWithTag(CreateRecipeTestTags.PHOTO_GENERATE).assertIsDisplayed()
        composeRule.onNodeWithTag(CreateRecipeTestTags.BASIC_SECTION).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag(CreateRecipeTestTags.MOMENT_BREAKFAST).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag(CreateRecipeTestTags.MOMENT_LUNCH).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag(CreateRecipeTestTags.MOMENT_DINNER).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag(CreateRecipeTestTags.MOMENT_DESSERT).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag(CreateRecipeTestTags.METRICS_GRID).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag(CreateRecipeTestTags.METRICS_TIME).assertIsDisplayed()
        composeRule.onNodeWithTag(CreateRecipeTestTags.METRICS_DIFFICULTY).assertIsDisplayed()
        composeRule.onNodeWithTag(CreateRecipeTestTags.METRICS_SERVINGS).assertIsDisplayed()
        composeRule.onNodeWithTag(CreateRecipeTestTags.SERVINGS_DECREASE).assertIsDisplayed()
        composeRule.onNodeWithTag(CreateRecipeTestTags.SERVINGS_INCREASE).assertIsDisplayed()
        composeRule.onNodeWithTag(CreateRecipeTestTags.INGREDIENTS_SECTION).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag(CreateRecipeTestTags.ADD_INGREDIENT).assertIsDisplayed()
        composeRule.onNodeWithTag(CreateRecipeTestTags.STEPS_SECTION).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag(CreateRecipeTestTags.ADD_STEP).performClick()
        composeRule.onNodeWithTag(CreateRecipeTestTags.stepNumber(0)).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag(CreateRecipeTestTags.FAVORITE_SECTION).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag(CreateRecipeTestTags.ACTION_DOCK).assertIsDisplayed()
        composeRule.onNodeWithTag(CreateRecipeTestTags.SAVE_DRAFT).assertIsDisplayed()
        composeRule.onNodeWithTag(CreateRecipeTestTags.PUBLISH).assertIsDisplayed()
    }
}

// verify 2 · RF-13, RF-25: la app real guarda un Borrador y lo muestra en Mis Recetas.
@RunWith(AndroidJUnit4::class)
class CreateRecipeAppE2ETest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun savedDraftAppearsInMyRecipes() {
        composeRule.onNodeWithTag(MyRecipesTestTags.CREATE_RECIPE).performClick()
        composeRule.onNodeWithTag(CreateRecipeTestTags.TITLE_FIELD).performTextInput("Borrador E2E aparición")
        composeRule.onNodeWithTag(CreateRecipeTestTags.SAVE_DRAFT).performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(MyRecipesTestTags.RECIPE_LIST).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(MyRecipesTestTags.SCREEN).assertIsDisplayed()
        composeRule.onNodeWithTag(MyRecipesTestTags.RECIPE_LIST).assertIsDisplayed()
        composeRule.onNodeWithTag(MyRecipesTestTags.SCAN_PANTRY_FAB)
            .assertIsDisplayed()
            .performClick()
        composeRule.onNodeWithTag(ScannerTestTags.SCREEN).assertIsDisplayed()
    }
}

// RF-17 a RF-20 y RF-22 (fidelidad visual): tarjetas de Mis Recetas con portada y
// badge de momento superpuesto, título, metadatos con iconos, píldora de estado y
// badge junto al título cuando la Receta no tiene foto.
@RunWith(AndroidJUnit4::class)
class MyRecipesCardsE2ETest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var cardsContext: Context

    private fun cardText(@StringRes resId: Int, vararg args: Any): String =
        cardsContext.getString(resId, *args)

    private fun showCards(recipes: List<Recipe>) {
        val baseContext = InstrumentationRegistry.getInstrumentation().targetContext
        val configuration = Configuration(baseContext.resources.configuration).apply {
            setLocale(Locale("es", "ES"))
        }
        cardsContext = baseContext.createConfigurationContext(configuration)
        composeRule.setContent {
            CompositionLocalProvider(LocalContext provides cardsContext) {
                WhatsForLunchTheme {
                    MyRecipesScreen(
                        uiState = MyRecipesUiState(
                            state = MyRecipesState.WithRecipes,
                            recipes = recipes,
                        ),
                        onCreateRecipe = {},
                        onScanPantry = {},
                    )
                }
            }
        }
    }

    private fun writePhoto(): String {
        val file = File(
            InstrumentationRegistry.getInstrumentation().targetContext.cacheDir,
            "mis-recetas-card-photo.png",
        )
        file.writeBytes(validImageBytes())
        return file.absolutePath
    }

    private fun recipe(id: String, photoPath: String?, status: RecipeStatus): Recipe = Recipe(
        id = id,
        title = "Tortilla Española",
        story = "Huevos camperos y patata agria",
        mealMoment = MealMoment.LUNCH,
        prepMinutes = 10,
        cookMinutes = 15,
        difficulty = Difficulty.EASY,
        baseServings = 4,
        photoPath = photoPath,
        status = status,
    )

    // RF-17, RF-18, RF-19 y RF-20: portada con badge de momento, título, metadatos
    // (tiempo total = preparación + cocción, dificultad y raciones) y píldora.
    @Test
    fun cardWithPhotoShowsCoverBadgeAndMetadata() {
        showCards(listOf(recipe("1", writePhoto(), RecipeStatus.DRAFT)))
        composeRule.onNodeWithTag(MyRecipesTestTags.RECIPE_PHOTO).assertIsDisplayed()
        composeRule.onNodeWithTag(MyRecipesTestTags.RECIPE_MOMENT).assertIsDisplayed()
        composeRule
            .onAllNodesWithText(cardText(R.string.meal_moment_lunch), useUnmergedTree = true)
            .filter(hasAnyAncestor(hasTestTag(MyRecipesTestTags.RECIPE_CARD)))[0]
            .assertIsDisplayed()
        composeRule.onNodeWithTag(MyRecipesTestTags.RECIPE_TITLE).assertIsDisplayed()
        composeRule.onNodeWithText("Tortilla Española").assertIsDisplayed()
        composeRule.onNodeWithTag(MyRecipesTestTags.RECIPE_TIME).assertIsDisplayed()
        composeRule.onNodeWithText(cardText(R.string.my_recipes_card_time, 25)).assertIsDisplayed()
        composeRule.onNodeWithTag(MyRecipesTestTags.RECIPE_DIFFICULTY).assertIsDisplayed()
        composeRule.onNodeWithText(cardText(R.string.difficulty_easy)).assertIsDisplayed()
        composeRule.onNodeWithTag(MyRecipesTestTags.RECIPE_SERVINGS).assertIsDisplayed()
        composeRule.onNodeWithText(cardText(R.string.my_recipes_card_servings, 4)).assertIsDisplayed()
        composeRule.onNodeWithTag(MyRecipesTestTags.RECIPE_STATUS).assertIsDisplayed()
        composeRule.onNodeWithText(cardText(R.string.my_recipes_status_draft)).assertIsDisplayed()
    }

    // RF-22: sin foto, la tarjeta arranca por el título y el badge del momento va
    // junto a él; la fila de metadatos y la píldora de estado siguen presentes.
    @Test
    fun cardWithoutPhotoShowsMomentBadgeNextToTitle() {
        showCards(listOf(recipe("2", null, RecipeStatus.PUBLISHED)))
        composeRule.onAllNodesWithTag(MyRecipesTestTags.RECIPE_PHOTO).assertCountEquals(0)
        composeRule.onNodeWithTag(MyRecipesTestTags.RECIPE_MOMENT).assertIsDisplayed()
        composeRule.onNodeWithTag(MyRecipesTestTags.RECIPE_TITLE).assertIsDisplayed()
        composeRule.onNodeWithTag(MyRecipesTestTags.RECIPE_TIME).assertIsDisplayed()
        composeRule.onNodeWithText(cardText(R.string.my_recipes_card_time, 25)).assertIsDisplayed()
        composeRule.onNodeWithTag(MyRecipesTestTags.RECIPE_STATUS).assertIsDisplayed()
        composeRule.onNodeWithText(cardText(R.string.my_recipes_status_published)).assertIsDisplayed()
    }
}
