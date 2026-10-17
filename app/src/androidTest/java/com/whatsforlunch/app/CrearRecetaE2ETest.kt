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
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
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
import com.whatsforlunch.app.ui.crearreceta.CrearRecetaScreen
import com.whatsforlunch.app.ui.crearreceta.CrearRecetaTestTags
import com.whatsforlunch.app.ui.crearreceta.CrearRecetaViewModel
import com.whatsforlunch.app.ui.crearreceta.toActions
import com.whatsforlunch.app.ui.misrecetas.MisRecetasTestTags
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
class CrearRecetaE2ETest {

    @get:Rule
    val composeRule = createComposeRule()

    // El locale se inyecta (mismo patrón que MisRecetasLocalizationE2ETest) para que
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
        val viewModel = CrearRecetaViewModel(repository, FakePhotoStore())
        viewModel.setPhotoAcquirer(photoAcquirer)
        composeRule.setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            CompositionLocalProvider(LocalContext provides formContext) {
                WhatsForLunchTheme {
                    CrearRecetaScreen(uiState = state, actions = viewModel.toActions {})
                }
            }
        }
        return repository
    }

    private fun typeTitle(title: String) {
        composeRule.onNodeWithTag(CrearRecetaTestTags.TITLE_FIELD).performTextInput(title)
    }

    private fun addValidIngredient(index: Int = 0, name: String = "Patata", quantity: String = "700") {
        composeRule.onNodeWithTag(CrearRecetaTestTags.ADD_INGREDIENT).performScrollTo().performClick()
        composeRule.onNodeWithTag(CrearRecetaTestTags.ingredientName(index)).performScrollTo().performTextInput(name)
        composeRule.onNodeWithTag(CrearRecetaTestTags.ingredientQuantity(index)).performScrollTo().performTextInput(quantity)
    }

    private fun addValidStep(index: Int = 0, text: String = "Pochar la cebolla") {
        composeRule.onNodeWithTag(CrearRecetaTestTags.ADD_STEP).performScrollTo().performClick()
        composeRule.onNodeWithTag(CrearRecetaTestTags.stepText(index)).performScrollTo().performTextInput(text)
    }

    @Test
    fun newFormPreselectsLunch() {
        launchForm()
        composeRule.onNodeWithTag(CrearRecetaTestTags.MOMENT_LUNCH).performScrollTo().assertIsSelected()
    }

    @Test
    fun addAndRemoveIngredientRows() {
        launchForm()
        addValidIngredient()
        composeRule.onNodeWithTag(CrearRecetaTestTags.ingredientName(0)).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag(CrearRecetaTestTags.removeIngredient(0)).performScrollTo().performClick()
        composeRule.onNodeWithTag(CrearRecetaTestTags.ADD_INGREDIENT).performScrollTo().assertIsDisplayed()
    }

    // RF-10 y RF-11: modificar y eliminar cualquier Paso existente.
    @Test
    fun editAndRemoveStepRows() {
        launchForm()
        addValidStep(index = 0, text = "Pochar la cebolla")
        addValidStep(index = 1, text = "Añadir el huevo")
        composeRule.onNodeWithTag(CrearRecetaTestTags.stepText(1)).performScrollTo()
            .performTextReplacement("Batir el huevo")
        composeRule.onNodeWithTag(CrearRecetaTestTags.stepTimer(1)).performScrollTo().performTextInput("5")
        composeRule.onNodeWithTag(CrearRecetaTestTags.stepText(1)).assertTextContains("Batir el huevo")
        composeRule.onNodeWithTag(CrearRecetaTestTags.stepTimer(1)).assertTextContains("5")
        composeRule.onNodeWithTag(CrearRecetaTestTags.removeStep(0)).performScrollTo().performClick()
        composeRule.onNodeWithTag(CrearRecetaTestTags.stepText(0)).performScrollTo().assertTextContains("Batir el huevo")
        composeRule.onNodeWithTag(CrearRecetaTestTags.stepTimer(0)).assertTextContains("5")
        composeRule.onNodeWithTag(CrearRecetaTestTags.removeStep(0)).performScrollTo().performClick()
        composeRule.onAllNodesWithTag(CrearRecetaTestTags.stepText(0)).assertCountEquals(0)
        composeRule.onNodeWithTag(CrearRecetaTestTags.ADD_STEP).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun saveDraftIsBlockedWithoutTitle() {
        val repository = launchForm()
        composeRule.onNodeWithTag(CrearRecetaTestTags.SAVE_DRAFT).performScrollTo().performClick()
        composeRule.onNodeWithText(formText(R.string.crear_receta_title_required)).performScrollTo().assertIsDisplayed()
        assertTrue(runBlocking { repository.observeRecipes().first() }.isEmpty())
    }

    // NFR-1: los mismos errores de bloqueo se afirman también en inglés, sin depender
    // del idioma del dispositivo de validación.
    @Test
    fun blockingErrorsAreLocalizedInEnglish() {
        val repository = launchForm(locale = Locale.ENGLISH)
        composeRule.onNodeWithTag(CrearRecetaTestTags.SAVE_DRAFT).performScrollTo().performClick()
        composeRule.onNodeWithText(formText(R.string.crear_receta_title_required)).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag(CrearRecetaTestTags.PUBLISH).performScrollTo().performClick()
        composeRule.onNodeWithText(formText(R.string.crear_receta_publish_error_ingredient)).performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText(formText(R.string.crear_receta_publish_error_step)).performScrollTo().assertIsDisplayed()
        assertTrue(runBlocking { repository.observeRecipes().first() }.isEmpty())
    }

    @Test
    fun saveDraftWithoutPhotoSavesDraft() {
        val repository = launchForm()
        typeTitle("Borrador sin foto")
        composeRule.onNodeWithTag(CrearRecetaTestTags.SAVE_DRAFT).performScrollTo().performClick()
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
        composeRule.onNodeWithTag(CrearRecetaTestTags.PUBLISH).performScrollTo().performClick()
        composeRule.onNodeWithTag(CrearRecetaTestTags.PUBLISH_ERRORS).performScrollTo().assertIsDisplayed()
        assertTrue(runBlocking { repository.observeRecipes().first() }.isEmpty())
    }

    @Test
    fun publishWithoutIngredientIsBlocked() {
        val repository = launchForm(photoAcquirer = FakePhotoAcquirer(gallery = PhotoResult.Success(byteArrayOf(1, 2, 3))))
        typeTitle("Sin ingrediente")
        composeRule.onNodeWithTag(CrearRecetaTestTags.PHOTO_GALLERY).performScrollTo().performClick()
        addValidStep()
        composeRule.onNodeWithTag(CrearRecetaTestTags.PUBLISH).performScrollTo().performClick()
        composeRule.onNodeWithText(formText(R.string.crear_receta_publish_error_ingredient)).performScrollTo()
            .assertIsDisplayed()
        assertTrue(runBlocking { repository.observeRecipes().first() }.isEmpty())
    }

    @Test
    fun publishWithPhotoSavesPublishedRecipe() {
        val repository = launchForm(photoAcquirer = FakePhotoAcquirer(gallery = PhotoResult.Success(byteArrayOf(1, 2, 3))))
        typeTitle("Tortilla publicada")
        composeRule.onNodeWithTag(CrearRecetaTestTags.PHOTO_GALLERY).performScrollTo().performClick()
        addValidIngredient()
        addValidStep()
        composeRule.onNodeWithTag(CrearRecetaTestTags.PUBLISH).performScrollTo().performClick()
        val recipes = runBlocking { repository.observeRecipes().first() }
        assertEquals(1, recipes.size)
        assertEquals(RecipeStatus.PUBLISHED, recipes.first().status)
        assertEquals("Tortilla publicada", recipes.first().title)
    }

    @Test
    fun generatePhotoWithoutApiKeyShowsNotice() {
        launchForm(photoAcquirer = FakePhotoAcquirer(generation = PhotoResult.MissingApiKey))
        composeRule.onNodeWithTag(CrearRecetaTestTags.PHOTO_GENERATE).performScrollTo().performClick()
        composeRule.onNodeWithTag(CrearRecetaTestTags.NOTICE).performScrollTo().assertIsDisplayed()
    }

    // RF-20: la imagen generada se ofrece para aceptarla o descartarla.
    @Test
    fun generatedPhotoCanBeAcceptedOrDiscarded() {
        launchForm(photoAcquirer = FakePhotoAcquirer(generation = PhotoResult.Success(validImageBytes())))
        composeRule.onNodeWithTag(CrearRecetaTestTags.PHOTO_GENERATE).performScrollTo().performClick()
        composeRule.onNodeWithTag(CrearRecetaTestTags.PHOTO_PREVIEW).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag(CrearRecetaTestTags.PHOTO_REMOVE).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag(CrearRecetaTestTags.PHOTO_REMOVE).performScrollTo().performClick()
        composeRule.onAllNodesWithTag(CrearRecetaTestTags.PHOTO_PREVIEW).assertCountEquals(0)
        composeRule.onNodeWithTag(CrearRecetaTestTags.PHOTO_GALLERY).performScrollTo().assertIsDisplayed()
    }

    // RF-22 y RD05: el fallo de generación avisa sin bloquear la foto manual ni el guardado.
    @Test
    fun generationFailureShowsNoticeAndKeepsManualOptions() {
        val repository = launchForm(photoAcquirer = FakePhotoAcquirer(generation = PhotoResult.GenerationFailed))
        typeTitle("Tortilla sin foto generada")
        composeRule.onNodeWithTag(CrearRecetaTestTags.PHOTO_GENERATE).performScrollTo().performClick()
        composeRule.onNodeWithTag(CrearRecetaTestTags.NOTICE).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(formText(R.string.crear_receta_notice_ai_failed)).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag(CrearRecetaTestTags.PHOTO_GALLERY).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag(CrearRecetaTestTags.PHOTO_CAMERA).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag(CrearRecetaTestTags.SAVE_DRAFT).performScrollTo().performClick()
        assertEquals(RecipeStatus.DRAFT, runBlocking { repository.observeRecipes().first() }.single().status)
    }

    @Test
    fun takePhotoWithDeniedPermissionShowsNotice() {
        launchForm(photoAcquirer = FakePhotoAcquirer(camera = PhotoResult.CameraPermissionDenied))
        composeRule.onNodeWithTag(CrearRecetaTestTags.PHOTO_CAMERA).performScrollTo().performClick()
        composeRule.onNodeWithTag(CrearRecetaTestTags.NOTICE).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun favoriteToggleFlipsState() {
        launchForm()
        composeRule.onNodeWithTag(CrearRecetaTestTags.FAVORITE_TOGGLE).performScrollTo().assertIsOff()
        composeRule.onNodeWithTag(CrearRecetaTestTags.FAVORITE_TOGGLE).performClick()
        composeRule.onNodeWithTag(CrearRecetaTestTags.FAVORITE_TOGGLE).assertIsOn()
    }
}

// verify 2 · RF-13, RF-25: la app real guarda un Borrador y lo muestra en Mis Recetas.
@RunWith(AndroidJUnit4::class)
class CrearRecetaAppE2ETest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun savedDraftAppearsInMisRecetas() {
        composeRule.onNodeWithTag(MisRecetasTestTags.CREATE_RECIPE).performClick()
        composeRule.onNodeWithTag(CrearRecetaTestTags.TITLE_FIELD).performTextInput("Borrador E2E aparición")
        composeRule.onNodeWithTag(CrearRecetaTestTags.SAVE_DRAFT).performScrollTo().performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(MisRecetasTestTags.RECIPE_LIST).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(MisRecetasTestTags.SCREEN).assertIsDisplayed()
        composeRule.onNodeWithTag(MisRecetasTestTags.RECIPE_LIST).assertIsDisplayed()
    }
}
