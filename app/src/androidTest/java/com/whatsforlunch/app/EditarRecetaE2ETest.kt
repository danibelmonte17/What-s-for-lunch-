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
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.whatsforlunch.app.data.photo.PhotoAcquirer
import com.whatsforlunch.app.data.photo.PhotoResult
import com.whatsforlunch.app.data.photo.PhotoStore
import com.whatsforlunch.app.data.repository.InMemoryRecipeRepository
import com.whatsforlunch.app.domain.misrecetas.MisRecetasState
import com.whatsforlunch.app.domain.model.Difficulty
import com.whatsforlunch.app.domain.model.MealMoment
import com.whatsforlunch.app.domain.model.Recipe
import com.whatsforlunch.app.domain.model.RecipeIngredient
import com.whatsforlunch.app.domain.model.RecipeOrigin
import com.whatsforlunch.app.domain.model.RecipeStatus
import com.whatsforlunch.app.domain.model.RecipeStep
import com.whatsforlunch.app.domain.repository.RecipeRepository
import com.whatsforlunch.app.ui.crearreceta.CrearRecetaRoute
import com.whatsforlunch.app.ui.crearreceta.CrearRecetaScreen
import com.whatsforlunch.app.ui.crearreceta.CrearRecetaTestTags
import com.whatsforlunch.app.ui.crearreceta.CrearRecetaViewModel
import com.whatsforlunch.app.ui.crearreceta.toActions
import com.whatsforlunch.app.ui.misrecetas.MisRecetasScreen
import com.whatsforlunch.app.ui.misrecetas.MisRecetasTestTags
import com.whatsforlunch.app.ui.misrecetas.MisRecetasUiState
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

// Fakes propios de esta feature: el almacén de fotos escribe el archivo de verdad (en
// la caché de la instrumentación) para que la vista previa precargada sea real y la
// galería inyecte bytes de prueba.
private class EditPhotoStore : PhotoStore {
    private val dir = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir, "editar-receta-e2e")

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

// Imagen válida y decodificable (PNG 1×1) para la foto de las Recetas sembradas.
private fun editImageBytes(): ByteArray {
    val bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    val output = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
    bitmap.recycle()
    return output.toByteArray()
}

private fun editPhotoFile(name: String): String {
    val file = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir, name)
    file.writeBytes(editImageBytes())
    return file.absolutePath
}

private class EditPhotoAcquirer(
    private val gallery: PhotoResult = PhotoResult.Cancelled,
) : PhotoAcquirer {
    override suspend fun pickFromGallery(): PhotoResult = gallery

    override suspend fun takePhoto(): PhotoResult = PhotoResult.Cancelled

    override suspend fun generate(prompt: String): PhotoResult = PhotoResult.Cancelled
}

// verify 1 y escenarios propios · RF-1 a RF-16: el formulario en modo edición montado
// sobre un repositorio en memoria con la Receta ya guardada. Cada valor sembrado es
// distinto del valor por defecto del alta, para que la precarga sea verificable.
@RunWith(AndroidJUnit4::class)
class EditarRecetaE2ETest {

    @get:Rule
    val composeRule = createComposeRule()

    // El locale se inyecta (mismo patrón que CrearRecetaE2ETest) para que las
    // aserciones sobre textos no dependan del idioma del dispositivo (NFR-1).
    private lateinit var editorContext: Context

    private fun editorText(@StringRes resId: Int): String = editorContext.getString(resId)

    private fun launchEditor(
        repository: RecipeRepository,
        recipeId: String,
        onDone: () -> Unit = {},
        photoAcquirer: PhotoAcquirer = EditPhotoAcquirer(),
        locale: Locale = Locale("es", "ES"),
    ): CrearRecetaViewModel {
        val baseContext = InstrumentationRegistry.getInstrumentation().targetContext
        val configuration = Configuration(baseContext.resources.configuration).apply { setLocale(locale) }
        editorContext = baseContext.createConfigurationContext(configuration)
        val viewModel = CrearRecetaViewModel(repository, EditPhotoStore(), recipeId)
        viewModel.setPhotoAcquirer(photoAcquirer)
        composeRule.setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            CompositionLocalProvider(LocalContext provides editorContext) {
                WhatsForLunchTheme {
                    CrearRecetaScreen(uiState = state, actions = viewModel.toActions(onDone))
                }
            }
        }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(seededTitle).fetchSemanticsNodes().isNotEmpty()
        }
        return viewModel
    }

    private fun repositoryWith(recipe: Recipe): RecipeRepository {
        val repository = InMemoryRecipeRepository()
        runBlocking { repository.saveRecipe(recipe) }
        return repository
    }

    private fun storedRecipes(repository: RecipeRepository): List<Recipe> =
        runBlocking { repository.observeRecipes().first() }

    // RF-2: todos los campos de la Receta quedan precargados al abrir en modo edición;
    // RF-3: la cabecera pasa a «Editar Receta» con el distintivo de su estado;
    // RF-5: en una Publicada el dock muestra una única acción primaria «Guardar cambios».
    @Test
    fun editingPreloadsRecipeFieldsAndPublishedDock() {
        launchEditor(repositoryWith(seededRecipe(photoPath = editPhotoFile("editar-precarga.png"))), SEEDED_ID)

        composeRule.onNodeWithText(editorText(R.string.crear_receta_edit_title)).assertIsDisplayed()
        composeRule.onNodeWithTag(CrearRecetaTestTags.STATUS_BADGE).assertIsDisplayed()
        composeRule.onNodeWithText(editorText(R.string.mis_recetas_status_published)).assertIsDisplayed()
        composeRule.onNodeWithText(editorText(R.string.crear_receta_header_subtitle)).assertIsDisplayed()

        composeRule.onNodeWithTag(CrearRecetaTestTags.TITLE_FIELD).assertTextContains(seededTitle)
        composeRule.onNodeWithTag(CrearRecetaTestTags.STORY_FIELD).performScrollTo()
            .assertTextContains("Huevos, patata y cebolla")
        composeRule.onNodeWithTag(CrearRecetaTestTags.MOMENT_DINNER).performScrollTo().assertIsSelected()
        composeRule.onNodeWithTag(CrearRecetaTestTags.PREP_MINUTES_FIELD).performScrollTo().assertTextContains("10")
        composeRule.onNodeWithTag(CrearRecetaTestTags.COOK_MINUTES_FIELD).performScrollTo().assertTextContains("15")
        composeRule.onNodeWithTag(CrearRecetaTestTags.DIFFICULTY_EASY).performScrollTo().assertIsSelected()
        composeRule.onNodeWithTag(CrearRecetaTestTags.SERVINGS_VALUE).performScrollTo().assertTextContains("2")
        composeRule.onNodeWithTag(CrearRecetaTestTags.CALORIES_FIELD).performScrollTo().assertTextContains("250")
        composeRule.onNodeWithTag(CrearRecetaTestTags.PHOTO_PREVIEW).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag(CrearRecetaTestTags.ingredientName(0)).performScrollTo().assertTextContains("Patata")
        composeRule.onNodeWithTag(CrearRecetaTestTags.ingredientQuantity(0)).assertTextContains("700")
        composeRule.onNodeWithTag(CrearRecetaTestTags.ingredientUnit(0)).assertTextContains("g")
        composeRule.onNodeWithTag(CrearRecetaTestTags.ingredientOptional(0)).assertIsOn()
        composeRule.onNodeWithTag(CrearRecetaTestTags.stepText(0)).performScrollTo().assertTextContains("Pochar la cebolla")
        composeRule.onNodeWithTag(CrearRecetaTestTags.stepTimer(0)).assertTextContains("5")
        composeRule.onNodeWithTag(CrearRecetaTestTags.FAVORITE_TOGGLE).performScrollTo().assertIsOn()

        composeRule.onNodeWithTag(CrearRecetaTestTags.SAVE_CHANGES).assertIsDisplayed()
        composeRule.onAllNodesWithTag(CrearRecetaTestTags.SAVE_DRAFT).assertCountEquals(0)
        composeRule.onAllNodesWithTag(CrearRecetaTestTags.PUBLISH).assertCountEquals(0)
    }

    // RF-9: al editar un Borrador, el dock es el del alta («Guardar Borrador» y
    // «Publicar Receta»).
    @Test
    fun draftEditingShowsCreationDock() {
        launchEditor(repositoryWith(seededRecipe(status = RecipeStatus.DRAFT)), SEEDED_ID)

        composeRule.onNodeWithText(editorText(R.string.mis_recetas_status_draft)).assertIsDisplayed()
        composeRule.onNodeWithTag(CrearRecetaTestTags.SAVE_DRAFT).assertIsDisplayed()
        composeRule.onNodeWithTag(CrearRecetaTestTags.PUBLISH).assertIsDisplayed()
        composeRule.onAllNodesWithTag(CrearRecetaTestTags.SAVE_CHANGES).assertCountEquals(0)
    }

    // RF-7 y RF-8: una Publicada que deja de cumplir los requisitos de Publicar mantiene
    // «Guardar cambios» deshabilitado y enumera junto al dock los requisitos incumplidos.
    @Test
    fun publishedEditingWithoutRequirementsBlocksSavingChanges() {
        val repository = repositoryWith(seededRecipeWithoutDetails())
        launchEditor(repository, SEEDED_ID, photoAcquirer = EditPhotoAcquirer(PhotoResult.Success(editImageBytes())))

        composeRule.onNodeWithTag(CrearRecetaTestTags.SAVE_CHANGES).assertIsNotEnabled()
        composeRule.onNodeWithTag(CrearRecetaTestTags.HEADER_SAVE).assertIsNotEnabled()
        composeRule.onNodeWithTag(CrearRecetaTestTags.PUBLISH_ERRORS).assertIsDisplayed()
        composeRule.onNodeWithText(editorText(R.string.crear_receta_publish_error_photo)).assertIsDisplayed()
        composeRule.onNodeWithText(editorText(R.string.crear_receta_publish_error_ingredient)).assertIsDisplayed()
        composeRule.onNodeWithText(editorText(R.string.crear_receta_publish_error_step)).assertIsDisplayed()

        // Completa la foto, un ingrediente y un paso: «Guardar cambios» se habilita y
        // guarda manteniendo el estado Publicada (RF-6).
        composeRule.onNodeWithTag(CrearRecetaTestTags.PHOTO_GALLERY).performScrollTo().performClick()
        composeRule.onNodeWithTag(CrearRecetaTestTags.ADD_INGREDIENT).performScrollTo().performClick()
        composeRule.onNodeWithTag(CrearRecetaTestTags.ingredientName(0)).performScrollTo()
            .performTextReplacement("Patata")
        composeRule.onNodeWithTag(CrearRecetaTestTags.ingredientQuantity(0)).performScrollTo()
            .performTextReplacement("700")
        composeRule.onNodeWithTag(CrearRecetaTestTags.ADD_STEP).performScrollTo().performClick()
        composeRule.onNodeWithTag(CrearRecetaTestTags.stepText(0)).performScrollTo()
            .performTextReplacement("Pochar la cebolla")

        composeRule.onNodeWithTag(CrearRecetaTestTags.SAVE_CHANGES).assertIsEnabled()
        composeRule.onAllNodesWithTag(CrearRecetaTestTags.PUBLISH_ERRORS).assertCountEquals(0)
        composeRule.onNodeWithTag(CrearRecetaTestTags.SAVE_CHANGES).performClick()

        val saved = storedRecipes(repository).single()
        assertEquals(RecipeStatus.PUBLISHED, saved.status)
    }

    // RF-6 y RF-16: guardar los cambios de una Publicada mantiene el estado y el origen.
    @Test
    fun saveChangesKeepsPublishedStatusAndOrigin() {
        val repository = repositoryWith(
            seededRecipe(origin = RecipeOrigin.SUGGESTION, photoPath = editPhotoFile("editar-publicada.png")),
        )
        launchEditor(repository, SEEDED_ID)

        composeRule.onNodeWithTag(CrearRecetaTestTags.TITLE_FIELD).performTextReplacement("Tortilla mejorada")
        composeRule.onNodeWithTag(CrearRecetaTestTags.SAVE_CHANGES).performClick()

        val saved = storedRecipes(repository).single()
        assertEquals(SEEDED_ID, saved.id)
        assertEquals("Tortilla mejorada", saved.title)
        assertEquals(RecipeStatus.PUBLISHED, saved.status)
        assertEquals(RecipeOrigin.SUGGESTION, saved.origin)
        assertEquals(1, saved.ingredients.size)
        assertEquals(1, saved.steps.size)
    }

    // RF-10: «Guardar Borrador» sobre un Borrador en edición guarda manteniendo el estado.
    @Test
    fun saveDraftFromEditingKeepsDraftStatus() {
        val repository = repositoryWith(seededRecipe(status = RecipeStatus.DRAFT, photoPath = null))
        launchEditor(repository, SEEDED_ID)

        composeRule.onNodeWithTag(CrearRecetaTestTags.TITLE_FIELD).performTextReplacement("Borrador corregido")
        composeRule.onNodeWithTag(CrearRecetaTestTags.SAVE_DRAFT).performClick()

        val saved = storedRecipes(repository).single()
        assertEquals("Borrador corregido", saved.title)
        assertEquals(RecipeStatus.DRAFT, saved.status)
    }

    // RF-11 y RF-12: publicar desde la edición de un Borrador que cumple los requisitos
    // deja la Receta Publicada.
    @Test
    fun publishFromEditingSetsPublishedStatus() {
        val repository = repositoryWith(
            seededRecipe(status = RecipeStatus.DRAFT, photoPath = editPhotoFile("editar-borrador.png")),
        )
        launchEditor(repository, SEEDED_ID)

        composeRule.onNodeWithTag(CrearRecetaTestTags.PUBLISH).assertIsEnabled()
        composeRule.onNodeWithTag(CrearRecetaTestTags.PUBLISH).performClick()

        val saved = storedRecipes(repository).single()
        assertEquals("Tortilla de patatas", saved.title)
        assertEquals(RecipeStatus.PUBLISHED, saved.status)
    }

    // RF-12: un Borrador en edición que no cumple los requisitos tiene «Publicar Receta»
    // deshabilitado y los requisitos incumplidos junto al dock.
    @Test
    fun draftEditingWithoutRequirementsBlocksPublish() {
        val repository = repositoryWith(seededRecipe(status = RecipeStatus.DRAFT, photoPath = null))
        launchEditor(repository, SEEDED_ID)

        composeRule.onNodeWithTag(CrearRecetaTestTags.PUBLISH).assertIsNotEnabled()
        composeRule.onNodeWithTag(CrearRecetaTestTags.PUBLISH_ERRORS).assertIsDisplayed()
        composeRule.onNodeWithText(editorText(R.string.crear_receta_publish_error_photo)).assertIsDisplayed()
    }

    // RF-13: «Guardar» de la cabecera ejecuta el guardado del estado: el de «Guardar
    // Borrador» sobre un Borrador.
    @Test
    fun headerSaveOnDraftEditingUpdatesDraft() {
        val repository = repositoryWith(seededRecipe(status = RecipeStatus.DRAFT, photoPath = null))
        launchEditor(repository, SEEDED_ID)

        composeRule.onNodeWithTag(CrearRecetaTestTags.TITLE_FIELD).performTextReplacement("Borrador desde cabecera")
        composeRule.onNodeWithTag(CrearRecetaTestTags.HEADER_SAVE).performClick()

        val saved = storedRecipes(repository).single()
        assertEquals("Borrador desde cabecera", saved.title)
        assertEquals(RecipeStatus.DRAFT, saved.status)
    }

    // RF-13: «Guardar» de la cabecera sobre una Publicada guarda como «Guardar cambios».
    @Test
    fun headerSaveOnPublishedEditingSavesChanges() {
        val repository = repositoryWith(seededRecipe(photoPath = editPhotoFile("editar-cabecera.png")))
        launchEditor(repository, SEEDED_ID)

        composeRule.onNodeWithTag(CrearRecetaTestTags.SAVE_CHANGES).assertIsEnabled()
        composeRule.onNodeWithTag(CrearRecetaTestTags.TITLE_FIELD).performTextReplacement("Publicada desde cabecera")
        composeRule.onNodeWithTag(CrearRecetaTestTags.HEADER_SAVE).performClick()

        val saved = storedRecipes(repository).single()
        assertEquals("Publicada desde cabecera", saved.title)
        assertEquals(RecipeStatus.PUBLISHED, saved.status)
    }

    // RF-14: mientras la acción de guardado del dock está deshabilitada, «Guardar» de la
    // cabecera también lo está.
    @Test
    fun headerSaveIsDisabledWhilePublishedSaveIsDisabled() {
        launchEditor(repositoryWith(seededRecipeWithoutDetails()), SEEDED_ID)

        composeRule.onNodeWithTag(CrearRecetaTestTags.SAVE_CHANGES).assertIsNotEnabled()
        composeRule.onNodeWithTag(CrearRecetaTestTags.HEADER_SAVE).assertIsNotEnabled()
    }

    // RF-15: cerrar sin guardar descarta los cambios (la Receta conserva sus datos).
    @Test
    fun closingWithoutSavingDiscardsChanges() {
        var closed = false
        val repository = repositoryWith(seededRecipe(photoPath = editPhotoFile("editar-cierre.png")))
        launchEditor(repository, SEEDED_ID, onDone = { closed = true })

        composeRule.onNodeWithTag(CrearRecetaTestTags.TITLE_FIELD).performTextReplacement("Cambios descartados")
        composeRule.onNodeWithText(editorText(R.string.action_back)).performClick()

        assertTrue(closed)
        assertEquals(seededTitle, storedRecipes(repository).single().title)
    }

    // NFR-1: los textos de edición se afirman también en inglés.
    @Test
    fun editTextsAreLocalizedInEnglish() {
        launchEditor(
            repositoryWith(seededRecipe(photoPath = editPhotoFile("editar-ingles.png"))),
            SEEDED_ID,
            locale = Locale.ENGLISH,
        )

        composeRule.onNodeWithText(editorText(R.string.crear_receta_edit_title)).assertIsDisplayed()
        composeRule.onNodeWithTag(CrearRecetaTestTags.SAVE_CHANGES).assertIsDisplayed()
        composeRule.onNodeWithText(editorText(R.string.crear_receta_save_changes)).assertIsDisplayed()
        assertEquals("Edit Recipe", editorText(R.string.crear_receta_edit_title))
        assertEquals("Save changes", editorText(R.string.crear_receta_save_changes))
    }

    // RF-4: la tarjeta de Mis Recetas ofrece «Editar receta» (icono `edit` sobre un
    // recuadro de 36 dp con área táctil de 48 dp) y avisa con el id de la Receta.
    @Test
    fun cardEditActionRequestsEditingWithRecipeId() {
        var requestedId: String? = null
        showCards(seededRecipe(photoPath = editPhotoFile("editar-tarjeta.png"))) { requestedId = it }

        composeRule.onNodeWithTag(MisRecetasTestTags.RECIPE_EDIT).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(editorText(R.string.mis_recetas_action_edit)).assertIsDisplayed()
        composeRule.onNodeWithTag(MisRecetasTestTags.RECIPE_EDIT).performClick()

        assertEquals(SEEDED_ID, requestedId)
    }

    // NFR-1: la acción «Editar receta» de la tarjeta también está en inglés.
    @Test
    fun cardEditActionIsLocalizedInEnglish() {
        showCards(seededRecipe(photoPath = editPhotoFile("editar-tarjeta-en.png")), locale = Locale.ENGLISH) {}

        assertEquals("Edit recipe", editorText(R.string.mis_recetas_action_edit))
        composeRule.onNodeWithContentDescription("Edit recipe").assertIsDisplayed()
    }

    private fun showCards(
        recipe: Recipe,
        locale: Locale = Locale("es", "ES"),
        onEditRecipe: (String) -> Unit,
    ) {
        val baseContext = InstrumentationRegistry.getInstrumentation().targetContext
        val configuration = Configuration(baseContext.resources.configuration).apply { setLocale(locale) }
        editorContext = baseContext.createConfigurationContext(configuration)
        composeRule.setContent {
            CompositionLocalProvider(LocalContext provides editorContext) {
                WhatsForLunchTheme {
                    MisRecetasScreen(
                        uiState = MisRecetasUiState(
                            state = MisRecetasState.WithRecipes,
                            recipes = listOf(recipe),
                        ),
                        onCreateRecipe = {},
                        onScanPantry = {},
                        onEditRecipe = onEditRecipe,
                    )
                }
            }
        }
    }

    private companion object {
        const val SEEDED_ID = "receta-editada-1"
        const val seededTitle = "Tortilla de patatas"

        // RF-2: cada valor difiere del valor por defecto del alta (momento Almuerzo,
        // dificultad Media, 4 raciones, sin favorito) para que la precarga se note.
        fun seededRecipe(
            status: RecipeStatus = RecipeStatus.PUBLISHED,
            origin: RecipeOrigin = RecipeOrigin.USER,
            photoPath: String? = null,
        ): Recipe = Recipe(
            id = SEEDED_ID,
            title = seededTitle,
            story = "Huevos, patata y cebolla",
            mealMoment = MealMoment.DINNER,
            prepMinutes = 10,
            cookMinutes = 15,
            difficulty = Difficulty.EASY,
            baseServings = 2,
            caloriesPerServing = 250,
            photoPath = photoPath,
            favorite = true,
            status = status,
            origin = origin,
            ingredients = listOf(
                RecipeIngredient(id = "$SEEDED_ID-i0", name = "Patata", quantity = 700.0, unit = "g", optional = true),
            ),
            steps = listOf(RecipeStep(id = "$SEEDED_ID-s0", text = "Pochar la cebolla", timerMinutes = 5)),
        )

        // Publicada que ha dejado de cumplir los requisitos de Publicar (RF-7, RF-8):
        // sin foto, sin ingredientes y sin pasos.
        fun seededRecipeWithoutDetails(): Recipe = Recipe(
            id = SEEDED_ID,
            title = seededTitle,
            story = "Huevos, patata y cebolla",
            mealMoment = MealMoment.DINNER,
            prepMinutes = 10,
            cookMinutes = 15,
            difficulty = Difficulty.EASY,
            baseServings = 2,
            photoPath = null,
            status = RecipeStatus.PUBLISHED,
        )
    }
}

// RF-18: si el id no corresponde a ninguna Receta, el formulario no se abre y se vuelve
// a Mis Recetas. Se monta la Route real (ViewModel y repositorio de Koin) sobre una base
// de datos vacía.
@RunWith(AndroidJUnit4::class)
class EditarRecetaRouteE2ETest {

    @get:Rule
    val composeRule = createComposeRule()

    @Before
    fun clearDatabase() {
        E2eDatabase.clear()
    }

    @Test
    fun missingRecipeReturnsWithoutOpeningForm() {
        var closed = false
        composeRule.setContent {
            WhatsForLunchTheme {
                CrearRecetaRoute(onDone = { closed = true }, recipeId = "receta-que-no-existe")
            }
        }

        composeRule.waitUntil(timeoutMillis = 5_000) { closed }
        assertTrue(closed)
    }
}

// verify 1 · RF-6, RF-17: la app real edita el título de una Receta Publicada y Mis
// Recetas muestra el título nuevo.
@RunWith(AndroidJUnit4::class)
class EditarRecetaAppE2ETest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun clearDatabase() {
        E2eDatabase.clear()
    }

    @Test
    fun editedTitleAppearsInMisRecetas() {
        val repository: RecipeRepository = GlobalContext.get().get<RecipeRepository>()
        runBlocking {
            repository.saveRecipe(
                Recipe(
                    id = "editar-e2e-1",
                    title = "Tortilla de patatas",
                    story = "Huevos, patata y cebolla",
                    mealMoment = MealMoment.LUNCH,
                    prepMinutes = 10,
                    cookMinutes = 15,
                    difficulty = Difficulty.EASY,
                    baseServings = 4,
                    photoPath = editPhotoFile("editar-app-e2e.png"),
                    status = RecipeStatus.PUBLISHED,
                    ingredients = listOf(
                        RecipeIngredient(name = "Patata", quantity = 700.0, unit = "g"),
                    ),
                    steps = listOf(RecipeStep(text = "Pochar la cebolla")),
                ),
            )
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(MisRecetasTestTags.RECIPE_EDIT).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(MisRecetasTestTags.SCREEN).assertIsDisplayed()
        composeRule.onNodeWithTag(MisRecetasTestTags.RECIPE_EDIT).performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Tortilla de patatas").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(CrearRecetaTestTags.TITLE_FIELD).performTextReplacement("Tortilla mejorada")
        composeRule.onNodeWithTag(CrearRecetaTestTags.SAVE_CHANGES).assertIsEnabled()
        composeRule.onNodeWithTag(CrearRecetaTestTags.SAVE_CHANGES).performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Tortilla mejorada").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(MisRecetasTestTags.SCREEN).assertIsDisplayed()
        composeRule.onNodeWithText("Tortilla mejorada").assertIsDisplayed()
        assertEquals(RecipeStatus.PUBLISHED, runBlocking { repository.getRecipe("editar-e2e-1") }?.status)
    }
}
