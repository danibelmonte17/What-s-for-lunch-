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
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.whatsforlunch.app.data.ai.AiFailureCause
import com.whatsforlunch.app.data.ai.AssetPromptSource
import com.whatsforlunch.app.data.ai.ImageGenerationResult
import com.whatsforlunch.app.data.ai.ImageGenerator
import com.whatsforlunch.app.data.photo.PhotoAcquirer
import com.whatsforlunch.app.data.photo.PhotoResult
import com.whatsforlunch.app.data.photo.PhotoStore
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
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext
import org.koin.dsl.module

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

// verify 1 · RF-EBF2, RF-EBF3: el formulario de alta montado sobre un repositorio en
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
        val viewModel = CreateRecipeViewModel(repository, FakePhotoStore(), AssetPromptSource(baseContext))
        viewModel.setPhotoAcquirer(photoAcquirer)
        composeRule.setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            CompositionLocalProvider(LocalContext provides formContext) {
                WhatsForLunchTheme {
                    CreateRecipeScreen(uiState = state, actions = viewModel.toActions({}, locale.language))
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

    // RF-EBF2/D6: el dock tiene un único botón de guardado «Guardar» en el alta.
    @Test
    fun saveActionShowsOnlyItsLabel() {
        launchForm()
        composeRule.onNodeWithTag(CreateRecipeTestTags.SAVE)
            .assertTextContains(formText(R.string.create_recipe_save))
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

    // S2/RF-EBF3: sin obligatorios «Guardar» está deshabilitado, se lista el requisito
    // incumplido junto al dock y no se guarda nada.
    @Test
    fun saveIsBlockedWithoutTitle() {
        val repository = launchForm()
        composeRule.onNodeWithTag(CreateRecipeTestTags.SAVE).assertIsNotEnabled()
        composeRule.onNodeWithTag(CreateRecipeTestTags.SAVE_ERRORS).assertIsDisplayed()
        composeRule.onNodeWithText(formText(R.string.create_recipe_save_error_title)).assertIsDisplayed()
        assertTrue(runBlocking { repository.observeRecipes().first() }.isEmpty())
    }

    // NFR-1: los mismos errores de bloqueo se afirman también en inglés, sin depender
    // del idioma del dispositivo de validación. Con «Guardar» deshabilitado, los
    // requisitos incumplidos se enumeran junto al dock.
    @Test
    fun blockingErrorsAreLocalizedInEnglish() {
        val repository = launchForm(locale = Locale.ENGLISH)
        composeRule.onNodeWithTag(CreateRecipeTestTags.SAVE).assertIsNotEnabled()
        composeRule.onNodeWithText(formText(R.string.create_recipe_save_error_ingredient)).assertIsDisplayed()
        composeRule.onNodeWithText(formText(R.string.create_recipe_save_error_step)).assertIsDisplayed()
        assertTrue(runBlocking { repository.observeRecipes().first() }.isEmpty())
    }

    // S1/RF-EBF2 + D11: la foto es siempre opcional; sin foto, «Guardar» guarda la
    // Receta con el resto de obligatorios completos.
    @Test
    fun saveWithoutPhotoSavesRecipe() {
        val repository = launchForm()
        typeTitle("Sin foto")
        addValidIngredient()
        addValidStep()
        composeRule.onNodeWithTag(CreateRecipeTestTags.SAVE).assertIsEnabled()
        composeRule.onNodeWithTag(CreateRecipeTestTags.SAVE).performClick()
        val recipes = runBlocking { repository.observeRecipes().first() }
        assertEquals(1, recipes.size)
        assertEquals("Sin foto", recipes.first().title)
        assertEquals(null, recipes.first().photoPath)
    }

    // S2/RF-EBF3: sin al menos un Ingrediente válido el guardado está bloqueado.
    @Test
    fun saveWithoutIngredientIsBlocked() {
        val repository = launchForm(photoAcquirer = FakePhotoAcquirer(gallery = PhotoResult.Success(byteArrayOf(1, 2, 3))))
        typeTitle("Sin ingrediente")
        composeRule.onNodeWithTag(CreateRecipeTestTags.PHOTO_GALLERY).performScrollTo().performClick()
        addValidStep()
        composeRule.onNodeWithTag(CreateRecipeTestTags.SAVE).assertIsNotEnabled()
        composeRule.onNodeWithText(formText(R.string.create_recipe_save_error_ingredient)).assertIsDisplayed()
        assertTrue(runBlocking { repository.observeRecipes().first() }.isEmpty())
    }

    // S1/RF-EBF2: con los obligatorios completos, «Guardar» guarda la Receta.
    @Test
    fun saveWithPhotoSavesRecipe() {
        val repository = launchForm(photoAcquirer = FakePhotoAcquirer(gallery = PhotoResult.Success(byteArrayOf(1, 2, 3))))
        typeTitle("Tortilla guardada")
        composeRule.onNodeWithTag(CreateRecipeTestTags.PHOTO_GALLERY).performScrollTo().performClick()
        addValidIngredient()
        addValidStep()
        composeRule.onNodeWithTag(CreateRecipeTestTags.SAVE).assertIsEnabled()
        composeRule.onNodeWithTag(CreateRecipeTestTags.SAVE).performClick()
        val recipes = runBlocking { repository.observeRecipes().first() }
        assertEquals(1, recipes.size)
        assertEquals("Tortilla guardada", recipes.first().title)
    }

    // RF-EBF2/RF-EBF3: «Guardar» está deshabilitado mientras falte algún requisito y se
    // habilita cuando el formulario los cumple; la lista de requisitos desaparece.
    @Test
    fun saveIsDisabledUntilRequirementsAreMet() {
        launchForm()
        composeRule.onNodeWithTag(CreateRecipeTestTags.SAVE).assertIsNotEnabled()
        composeRule.onNodeWithTag(CreateRecipeTestTags.SAVE_ERRORS).assertIsDisplayed()
        composeRule.onNodeWithText(formText(R.string.create_recipe_save_error_title)).assertIsDisplayed()
        typeTitle("Con requisitos")
        addValidIngredient()
        addValidStep()
        composeRule.onNodeWithTag(CreateRecipeTestTags.SAVE).assertIsEnabled()
        composeRule.onAllNodesWithTag(CreateRecipeTestTags.SAVE_ERRORS).assertCountEquals(0)
    }

    // RF-EBF3: las filas de Ingrediente inválidas también se enumeran junto al dock
    // como requisito que impide Guardar.
    @Test
    fun invalidIngredientRowsAreListedAsBlockingRequirement() {
        launchForm()
        typeTitle("Con fila inválida")
        composeRule.onNodeWithTag(CreateRecipeTestTags.ADD_INGREDIENT).performScrollTo().performClick()
        composeRule.onNodeWithTag(CreateRecipeTestTags.SAVE).assertIsNotEnabled()
        composeRule.onNodeWithText(formText(R.string.create_recipe_save_error_invalid_ingredient)).assertIsDisplayed()
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

    // RF-22 y RD05: el fallo de generación avisa con su causa, se descarta y no
    // bloquea la foto manual ni el guardado con el botón único.
    @Test
    fun generationFailureShowsNoticeAndKeepsManualOptions() {
        val repository = launchForm(
            photoAcquirer = FakePhotoAcquirer(
                generation = PhotoResult.GenerationFailed(AiFailureCause.NO_CONNECTION),
                gallery = PhotoResult.Success(byteArrayOf(1, 2, 3)),
            ),
        )
        typeTitle("Tortilla sin foto generada")
        composeRule.onNodeWithTag(CreateRecipeTestTags.PHOTO_GENERATE).performScrollTo().performClick()
        composeRule.onNodeWithTag(CreateRecipeTestTags.NOTICE).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(formText(R.string.create_recipe_notice_ai_failed_no_connection))
            .performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag(CreateRecipeTestTags.PHOTO_GALLERY).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag(CreateRecipeTestTags.PHOTO_CAMERA).performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithText(formText(R.string.action_dismiss)).performClick()
        composeRule.onNodeWithTag(CreateRecipeTestTags.NOTICE).assertDoesNotExist()

        // La foto manual y el guardado siguen disponibles tras el fallo.
        composeRule.onNodeWithTag(CreateRecipeTestTags.PHOTO_GALLERY).performScrollTo().performClick()
        addValidIngredient()
        addValidStep()
        composeRule.onNodeWithTag(CreateRecipeTestTags.SAVE).performClick()
        assertEquals(1, runBlocking { repository.observeRecipes().first() }.size)
    }

    @Test
    fun invalidAiResponseShowsItsOwnNoticeAndKeepsManualPhotoOptions() {
        launchForm(
            photoAcquirer = FakePhotoAcquirer(
                generation = PhotoResult.GenerationFailed(AiFailureCause.INVALID_RESPONSE),
            ),
        )
        typeTitle("Tortilla con respuesta inválida")
        composeRule.onNodeWithTag(CreateRecipeTestTags.PHOTO_GENERATE).performScrollTo().performClick()
        composeRule.onNodeWithText(formText(R.string.create_recipe_notice_ai_failed_invalid_response))
            .performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag(CreateRecipeTestTags.PHOTO_GALLERY).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag(CreateRecipeTestTags.PHOTO_CAMERA).performScrollTo().assertIsDisplayed()
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

    // S1/RF-EBF2: la cabecera no dibuja «Guardar»; el único guardado es el botón único
    // del dock, que guarda la Receta completa.
    @Test
    fun headerHasNoSaveActionAndDockSaves() {
        val repository = launchForm(photoAcquirer = FakePhotoAcquirer(gallery = PhotoResult.Success(byteArrayOf(1, 2, 3))))
        composeRule.onAllNodesWithTag(CreateRecipeTestTags.HEADER_SAVE).assertCountEquals(0)
        typeTitle("Receta desde el dock")
        composeRule.onNodeWithTag(CreateRecipeTestTags.PHOTO_GALLERY).performScrollTo().performClick()
        addValidIngredient()
        addValidStep()
        composeRule.onNodeWithTag(CreateRecipeTestTags.SAVE).performClick()
        val recipes = runBlocking { repository.observeRecipes().first() }
        assertEquals(1, recipes.size)
        assertEquals("Receta desde el dock", recipes.first().title)
    }

    // S1/S7/RF-EBF6: están los bloques y controles del prototipo (cabecera sin
    // distintivo de estado, portada con controles de foto, información básica con chips
    // de momento, rejilla de métricas, ingredientes, pasos con número en círculo,
    // favorito y dock con el único botón de guardado) y no queda ningún resto de los
    // distintivos «Borrador»/«Publicada» ni de los botones retirados.
    @Test
    fun formShowsPrototypeBlocksAndControls() {
        launchForm()
        composeRule.onAllNodesWithTag(CreateRecipeTestTags.HEADER_SAVE).assertCountEquals(0)
        composeRule.onAllNodesWithTag("create_recipe_status_badge").assertCountEquals(0)
        composeRule.onAllNodesWithText("Borrador").assertCountEquals(0)
        composeRule.onAllNodesWithText("Publicada").assertCountEquals(0)
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
        composeRule.onNodeWithTag(CreateRecipeTestTags.SAVE).assertIsDisplayed()
        // Retirados por completo: «Borrador» y «Publicar Receta» (D6/D10).
        composeRule.onAllNodesWithTag("create_recipe_save_draft").assertCountEquals(0)
        composeRule.onAllNodesWithTag("create_recipe_publish").assertCountEquals(0)
    }

    // RF-VPI2 · S1: «Ingredientes de este paso» abre el diálogo de casillas con los
    // ingredientes en orden (nombre + cantidad/unidad); «Aceptar» guarda la selección
    // del Paso y «Cancelar» no toca la Receta (micro-decisión del informe).
    @Test
    fun stepIngredientsDialogAssignsIngredientsToStep() {
        val repository = launchForm(photoAcquirer = FakePhotoAcquirer(gallery = PhotoResult.Success(byteArrayOf(1, 2, 3))))
        typeTitle("Tortilla de patatas")
        composeRule.onNodeWithTag(CreateRecipeTestTags.PHOTO_GALLERY).performScrollTo().performClick()
        addValidIngredient(0, name = "Patatas", quantity = "700")
        addValidIngredient(1, name = "Huevo", quantity = "2")
        addValidStep(0, text = "Pochar la cebolla")
        addValidStep(1, text = "Batir el huevo")

        // Sin vínculos: los dos Pasos muestran «Sin ingredientes».
        stepSummaryShouldBe(0, formText(R.string.create_recipe_step_ingredients_none))
        stepSummaryShouldBe(1, formText(R.string.create_recipe_step_ingredients_none))

        // «Cancelar» descarta la selección sin tocar la Receta.
        openStepIngredientsDialog(0)
        composeRule.onNodeWithText("Patatas (700)").assertIsDisplayed()
        composeRule.onNodeWithText("Huevo (2)").assertIsDisplayed()
        composeRule.onNodeWithTag(CreateRecipeTestTags.stepIngredientOption(0)).assertIsOff()
        composeRule.onNodeWithTag(CreateRecipeTestTags.stepIngredientOption(0)).performClick()
        composeRule.onNodeWithTag(CreateRecipeTestTags.stepIngredientOption(0)).assertIsOn()
        composeRule.onNodeWithTag(CreateRecipeTestTags.STEP_INGREDIENTS_CANCEL).performClick()
        composeRule.onAllNodesWithTag(CreateRecipeTestTags.STEP_INGREDIENTS_DIALOG).assertCountEquals(0)
        stepSummaryShouldBe(0, formText(R.string.create_recipe_step_ingredients_none))

        // «Aceptar» guarda la selección: solo «Patatas» en el Paso 1; el Paso 2 sigue
        // sin ingredientes (S1).
        linkStepIngredient(stepIndex = 0, ingredientIndex = 0)
        stepSummaryShouldBe(0, formQuantity(1))
        stepSummaryShouldBe(1, formText(R.string.create_recipe_step_ingredients_none))

        // El vínculo se guarda con el id del Ingrediente (RF-VPI3).
        composeRule.onNodeWithTag(CreateRecipeTestTags.SAVE).performClick()
        val saved = runBlocking { repository.observeRecipes().first() }.single()
        assertEquals(listOf(saved.ingredients[0].id), saved.steps[0].ingredientIds)
        assertTrue(saved.steps[1].ingredientIds.isEmpty())
    }

    // RF-VPI3 · S3 (verify 2): al quitar la fila de un Ingrediente, sus vínculos
    // desaparecen de todos los Pasos (D6), los de otros Ingredientes se conservan y
    // guardar sigue siendo válido con Pasos sin vínculos.
    @Test
    fun removingIngredientRowClearsItsStepLinks() {
        val repository = launchForm(photoAcquirer = FakePhotoAcquirer(gallery = PhotoResult.Success(byteArrayOf(1, 2, 3))))
        typeTitle("Tortilla de patatas")
        composeRule.onNodeWithTag(CreateRecipeTestTags.PHOTO_GALLERY).performScrollTo().performClick()
        addValidIngredient(0, name = "Patatas", quantity = "700")
        addValidIngredient(1, name = "Huevo", quantity = "2")
        addValidStep(0, text = "Pochar la cebolla")
        addValidStep(1, text = "Batir el huevo")
        addValidStep(2, text = "Cuajar la tortilla")

        // «Patatas» en los Pasos 1 y 3; «Huevo» en el Paso 2.
        linkStepIngredient(stepIndex = 0, ingredientIndex = 0)
        linkStepIngredient(stepIndex = 1, ingredientIndex = 1)
        linkStepIngredient(stepIndex = 2, ingredientIndex = 0)

        composeRule.onNodeWithTag(CreateRecipeTestTags.removeIngredient(0)).performScrollTo().performClick()

        stepSummaryShouldBe(0, formText(R.string.create_recipe_step_ingredients_none))
        stepSummaryShouldBe(1, formQuantity(1))
        stepSummaryShouldBe(2, formText(R.string.create_recipe_step_ingredients_none))

        composeRule.onNodeWithTag(CreateRecipeTestTags.SAVE).performClick()
        val saved = runBlocking { repository.observeRecipes().first() }.single()
        assertEquals(1, saved.ingredients.size)
        assertEquals("Huevo", saved.ingredients.single().name)
        assertTrue(saved.steps[0].ingredientIds.isEmpty())
        assertEquals(listOf(saved.ingredients.single().id), saved.steps[1].ingredientIds)
        assertTrue(saved.steps[2].ingredientIds.isEmpty())
    }

    // D3: el control «Ingredientes de este paso» solo se renderiza con ≥1 fila de
    // Ingrediente; sin filas no hay control que abrir.
    @Test
    fun stepIngredientsControlIsHiddenWithoutIngredientRows() {
        launchForm()
        addValidStep(0)
        composeRule.onAllNodesWithTag(CreateRecipeTestTags.stepIngredients(0)).assertCountEquals(0)
        addValidIngredient(0)
        composeRule.onNodeWithTag(CreateRecipeTestTags.stepIngredients(0)).performScrollTo().assertIsDisplayed()
    }

    // S6 · NFR-1: los textos del control y del diálogo están en inglés con
    // `Locale.ENGLISH` (paridad es/en). La ventana del `Dialog` resuelve sus strings con
    // los recursos del contexto de la Activity (no con el `LocalContext` inyectado), así
    // que la prueba fija también el locale de esos recursos y lo restaura al salir.
    @Suppress("DEPRECATION")
    @Test
    fun stepIngredientsTextsAreLocalizedInEnglish() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val appResources = instrumentation.targetContext.resources
        val originalAppConfiguration = Configuration(appResources.configuration)
        val englishConfiguration = Configuration(originalAppConfiguration).apply { setLocale(Locale.ENGLISH) }
        try {
            launchForm(locale = Locale.ENGLISH)
            instrumentation.runOnMainSync {
                val resumed = ActivityLifecycleMonitorRegistry.getInstance()
                    .getActivitiesInStage(Stage.RESUMED)
                    .first()
                appResources.updateConfiguration(englishConfiguration, appResources.displayMetrics)
                resumed.resources.updateConfiguration(englishConfiguration, resumed.resources.displayMetrics)
            }
            typeTitle("Spanish omelette")
            addValidIngredient(0, name = "Potatoes", quantity = "700")
            addValidStep(0, text = "Fry the onion")

            assertEquals("No ingredients", formText(R.string.create_recipe_step_ingredients_none))
            stepSummaryShouldBe(0, "No ingredients")

            openStepIngredientsDialog(0)
            assertEquals("Ingredients for this step", formText(R.string.create_recipe_step_ingredients))
            assertEquals("Accept", formText(R.string.create_recipe_step_ingredients_accept))
            assertEquals("Cancel", formText(R.string.create_recipe_step_ingredients_cancel))
            composeRule.onAllNodesWithText("Accept")
                .filter(hasTestTag(CreateRecipeTestTags.STEP_INGREDIENTS_ACCEPT))
                .assertCountEquals(1)
            composeRule.onAllNodesWithText("Cancel")
                .filter(hasTestTag(CreateRecipeTestTags.STEP_INGREDIENTS_CANCEL))
                .assertCountEquals(1)
            composeRule.onNodeWithText("Potatoes (700)").assertIsDisplayed()
            composeRule.onNodeWithTag(CreateRecipeTestTags.STEP_INGREDIENTS_CANCEL).performClick()

            linkStepIngredient(stepIndex = 0, ingredientIndex = 0)
            stepSummaryShouldBe(0, formQuantity(1))
        } finally {
            instrumentation.runOnMainSync {
                appResources.updateConfiguration(originalAppConfiguration, appResources.displayMetrics)
            }
        }
    }

    private fun formQuantity(count: Int): String =
        formContext.resources.getQuantityString(R.plurals.create_recipe_step_ingredients_count, count, count)

    // El resumen es un `Text` hijo del control clicleable (nodo merged): se localiza el
    // nodo por su tag y se filtra por el texto del resumen.
    private fun stepSummaryShouldBe(stepIndex: Int, expected: String) {
        composeRule.onAllNodesWithText(expected)
            .filter(hasTestTag(CreateRecipeTestTags.stepIngredients(stepIndex)))
            .assertCountEquals(1)
    }

    private fun openStepIngredientsDialog(stepIndex: Int) {
        composeRule.onNodeWithTag(CreateRecipeTestTags.stepIngredients(stepIndex))
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithTag(CreateRecipeTestTags.STEP_INGREDIENTS_DIALOG).assertIsDisplayed()
    }

    private fun linkStepIngredient(stepIndex: Int, ingredientIndex: Int) {
        openStepIngredientsDialog(stepIndex)
        composeRule.onNodeWithTag(CreateRecipeTestTags.stepIngredientOption(ingredientIndex)).performClick()
        composeRule.onNodeWithTag(CreateRecipeTestTags.STEP_INGREDIENTS_ACCEPT).performClick()
        composeRule.onAllNodesWithTag(CreateRecipeTestTags.STEP_INGREDIENTS_DIALOG).assertCountEquals(0)
    }
}

// Generador de imagen falso para la app real: «Generar con IA» produce bytes de
// prueba sin llamar al proveedor. Se inyecta en Koin (el binding real es
// `single<ImageGenerator>`) antes de navegar al formulario.
private class FakeGeneratedImageGenerator : ImageGenerator {
    override suspend fun generate(prompt: String): ImageGenerationResult =
        ImageGenerationResult.Success(validImageBytes())
}

// verify 2 · S1: la app real guarda una Receta con el botón único y la muestra en
// Mis Recetas sin ningún distintivo de estado. La foto llega por «Generar con IA»
// con el ImageGenerator sustituido en Koin, para no depender de pantallas del sistema.
@RunWith(AndroidJUnit4::class)
class CreateRecipeAppE2ETest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun clearDatabase() {
        E2eDatabase.clear()
    }

    @Test
    fun savedRecipeAppearsInMyRecipesWithoutStatusBadges() {
        GlobalContext.get().loadModules(
            listOf(module { single<ImageGenerator> { FakeGeneratedImageGenerator() } }),
            allowOverride = true,
        )
        composeRule.onNodeWithTag(MyRecipesTestTags.CREATE_RECIPE).performClick()
        composeRule.onNodeWithTag(CreateRecipeTestTags.TITLE_FIELD).performTextInput("Receta E2E aparición")
        composeRule.onNodeWithTag(CreateRecipeTestTags.PHOTO_GENERATE).performScrollTo().performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(CreateRecipeTestTags.PHOTO_PREVIEW).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(CreateRecipeTestTags.ADD_INGREDIENT).performScrollTo().performClick()
        composeRule.onNodeWithTag(CreateRecipeTestTags.ingredientName(0)).performScrollTo().performTextInput("Patata")
        composeRule.onNodeWithTag(CreateRecipeTestTags.ingredientQuantity(0)).performScrollTo().performTextInput("700")
        composeRule.onNodeWithTag(CreateRecipeTestTags.ADD_STEP).performScrollTo().performClick()
        composeRule.onNodeWithTag(CreateRecipeTestTags.stepText(0)).performScrollTo().performTextInput("Pochar")
        // El dock es fijo (fuera del scroll del formulario): clic directo.
        composeRule.onNodeWithTag(CreateRecipeTestTags.SAVE).performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(MyRecipesTestTags.RECIPE_LIST).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(MyRecipesTestTags.SCREEN).assertIsDisplayed()
        composeRule.onNodeWithTag(MyRecipesTestTags.RECIPE_LIST).assertIsDisplayed()
        // S7/RF-EBF6: la tarjeta guardada no lleva distintivo de estado ni textos
        // «Borrador»/«Publicada».
        composeRule.onAllNodesWithTag("my_recipes_card_status").assertCountEquals(0)
        composeRule.onAllNodesWithText("Borrador").assertCountEquals(0)
        composeRule.onAllNodesWithText("Publicada").assertCountEquals(0)
        composeRule.onNodeWithTag(MyRecipesTestTags.SCAN_PANTRY_FAB)
            .assertIsDisplayed()
            .performClick()
        composeRule.onNodeWithTag(ScannerTestTags.SCREEN).assertIsDisplayed()
    }
}

// RF-17 a RF-20 y RF-22 (fidelidad visual): tarjetas de Mis Recetas con portada y
// badge de momento superpuesto, título, metadatos con iconos y badge junto al título
// cuando la Receta no tiene foto. Sin píldora de estado (RF-EBF6).
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

    private fun recipe(id: String, photoPath: String?): Recipe = Recipe(
        id = id,
        title = "Tortilla Española",
        story = "Huevos camperos y patata agria",
        mealMoment = MealMoment.LUNCH,
        prepMinutes = 10,
        cookMinutes = 15,
        difficulty = Difficulty.EASY,
        baseServings = 4,
        photoPath = photoPath,
    )

    // RF-17, RF-18, RF-19 y RF-20: portada con badge de momento, título, metadatos
    // (tiempo total = preparación + cocción, dificultad y raciones). Sin píldora de
    // estado (RF-EBF6/S7).
    @Test
    fun cardWithPhotoShowsCoverBadgeAndMetadata() {
        showCards(listOf(recipe("1", writePhoto())))
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
        composeRule.onAllNodesWithTag("my_recipes_card_status").assertCountEquals(0)
        composeRule.onAllNodesWithText("Borrador").assertCountEquals(0)
        composeRule.onAllNodesWithText("Publicada").assertCountEquals(0)
    }

    // RF-22: sin foto, la tarjeta arranca por el título y el badge del momento va
    // junto a él; la fila de metadatos sigue presente, sin píldora de estado (RF-EBF6).
    @Test
    fun cardWithoutPhotoShowsMomentBadgeNextToTitle() {
        showCards(listOf(recipe("2", null)))
        composeRule.onAllNodesWithTag(MyRecipesTestTags.RECIPE_PHOTO).assertCountEquals(0)
        composeRule.onNodeWithTag(MyRecipesTestTags.RECIPE_MOMENT).assertIsDisplayed()
        composeRule.onNodeWithTag(MyRecipesTestTags.RECIPE_TITLE).assertIsDisplayed()
        composeRule.onNodeWithTag(MyRecipesTestTags.RECIPE_TIME).assertIsDisplayed()
        composeRule.onNodeWithText(cardText(R.string.my_recipes_card_time, 25)).assertIsDisplayed()
        composeRule.onAllNodesWithTag("my_recipes_card_status").assertCountEquals(0)
    }
}
