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
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.junit4.v2.createComposeRule
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
import com.whatsforlunch.app.data.ai.AssetPromptSource
import com.whatsforlunch.app.data.photo.PhotoAcquirer
import com.whatsforlunch.app.data.photo.PhotoResult
import com.whatsforlunch.app.data.photo.PhotoStore
import com.whatsforlunch.app.data.repository.InMemoryRecipeRepository
import com.whatsforlunch.app.domain.myrecipes.MyRecipesState
import com.whatsforlunch.app.domain.model.Difficulty
import com.whatsforlunch.app.domain.model.MealMoment
import com.whatsforlunch.app.domain.model.Recipe
import com.whatsforlunch.app.domain.model.RecipeIngredient
import com.whatsforlunch.app.domain.model.RecipeOrigin
import com.whatsforlunch.app.domain.model.RecipeStep
import com.whatsforlunch.app.domain.repository.RecipeRepository
import com.whatsforlunch.app.ui.components.optionTag
import com.whatsforlunch.app.ui.createrecipe.CreateRecipeRoute
import com.whatsforlunch.app.ui.createrecipe.CreateRecipeScreen
import com.whatsforlunch.app.ui.createrecipe.CreateRecipeTestTags
import com.whatsforlunch.app.ui.createrecipe.CreateRecipeViewModel
import com.whatsforlunch.app.ui.createrecipe.toActions
import com.whatsforlunch.app.ui.myrecipes.MyRecipesScreen
import com.whatsforlunch.app.ui.myrecipes.MyRecipesTestTags
import com.whatsforlunch.app.ui.myrecipes.MyRecipesUiState
import com.whatsforlunch.app.ui.theme.WhatsForLunchTheme
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Locale
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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

// verify 1 y escenarios propios · RF-EBF2, RF-EBF3, RF-EBF4: el formulario en modo
// edición montado sobre un repositorio en memoria con la Receta ya guardada. Cada
// valor sembrado es distinto del valor por defecto del alta, para que la precarga sea
// verificable. El dock tiene el único botón «Guardar cambios» (D6), sin distintivos de
// estado (D2).
@RunWith(AndroidJUnit4::class)
class EditRecipeE2ETest {

    @get:Rule
    val composeRule = createComposeRule()

    // El locale se inyecta (mismo patrón que CreateRecipeE2ETest) para que las
    // aserciones sobre textos no dependan del idioma del dispositivo (NFR-1).
    private lateinit var editorContext: Context

    private fun editorText(@StringRes resId: Int): String = editorContext.getString(resId)

    private fun launchEditor(
        repository: RecipeRepository,
        recipeId: String,
        onDone: () -> Unit = {},
        photoAcquirer: PhotoAcquirer = EditPhotoAcquirer(),
        locale: Locale = Locale.forLanguageTag("es-ES"),
    ): CreateRecipeViewModel {
        val baseContext = InstrumentationRegistry.getInstrumentation().targetContext
        val configuration = Configuration(baseContext.resources.configuration).apply { setLocale(locale) }
        editorContext = baseContext.createConfigurationContext(configuration)
        val viewModel = CreateRecipeViewModel(repository, EditPhotoStore(), AssetPromptSource(baseContext), recipeId)
        viewModel.setPhotoAcquirer(photoAcquirer)
        composeRule.setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            CompositionLocalProvider(LocalContext provides editorContext) {
                WhatsForLunchTheme {
                    CreateRecipeScreen(uiState = state, actions = viewModel.toActions(onDone, locale.language))
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
    // RF-EBF2/RF-EBF6: el dock muestra el único botón «Guardar cambios», sin
    // distintivo de estado y sin «Guardar» de cabecera (RF-13).
    @Test
    fun editingPreloadsRecipeFieldsAndSingleSaveDock() {
        launchEditor(
            repositoryWith(
                seededRecipe(photoPath = editPhotoFile("editar-precarga.png")).copy(
                    // Un segundo Paso sin temporizador para precargar «Sin temporizador»
                    // (D7/D10) junto al Paso con temporizador.
                    steps = listOf(
                        RecipeStep(id = "$SEEDED_ID-s0", text = "Pochar la cebolla", timerMinutes = 5),
                        RecipeStep(id = "$SEEDED_ID-s1", text = "Dejar reposar"),
                    ),
                ),
            ),
            SEEDED_ID,
        )

        composeRule.onNodeWithText(editorText(R.string.create_recipe_edit_title)).assertIsDisplayed()
        composeRule.onAllNodesWithTag("create_recipe_status_badge").assertCountEquals(0)
        composeRule.onAllNodesWithText("Borrador").assertCountEquals(0)
        composeRule.onAllNodesWithText("Publicada").assertCountEquals(0)
        composeRule.onNodeWithText(editorText(R.string.create_recipe_header_subtitle)).assertIsDisplayed()

        composeRule.onNodeWithTag(CreateRecipeTestTags.TITLE_FIELD).assertTextContains(seededTitle)
        composeRule.onNodeWithTag(CreateRecipeTestTags.STORY_FIELD).performScrollTo()
            .assertTextContains("Huevos, patata y cebolla")
        composeRule.onNodeWithTag(CreateRecipeTestTags.MOMENT_DINNER).performScrollTo().assertIsSelected()
        composeRule.onNodeWithTag(CreateRecipeTestTags.PREP_MINUTES_FIELD).performScrollTo().assertTextContains("00:10")
        // verify 1: el tiempo de cocción ya no existe (D3): ningún campo de cocción.
        composeRule.onAllNodesWithTag("create_recipe_cook_minutes").assertCountEquals(0)
        composeRule.onNodeWithTag(CreateRecipeTestTags.DIFFICULTY_EASY).performScrollTo().assertIsSelected()
        composeRule.onNodeWithTag(CreateRecipeTestTags.SERVINGS_VALUE).performScrollTo().assertTextContains("2")
        composeRule.onNodeWithTag(CreateRecipeTestTags.CALORIES_FIELD).performScrollTo().assertTextContains("250")
        composeRule.onNodeWithTag(CreateRecipeTestTags.PHOTO_PREVIEW).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag(CreateRecipeTestTags.ingredientName(0)).performScrollTo().assertTextContains("Patata")
        composeRule.onNodeWithTag(CreateRecipeTestTags.ingredientQuantity(0)).assertTextContains("700")
        composeRule.onNodeWithTag(CreateRecipeTestTags.ingredientUnit(0)).assertTextContains("g")
        // D5: sin casilla de opcional en la fila de Ingrediente.
        composeRule.onAllNodesWithTag("create_recipe_ingredient_optional_0").assertCountEquals(0)
        composeRule.onNodeWithTag(CreateRecipeTestTags.stepText(0)).performScrollTo().assertTextContains("Pochar la cebolla")
        composeRule.onNodeWithTag(CreateRecipeTestTags.stepTimer(0)).assertTextContains("00:05")
        // D7/D10: un Paso sin temporizador precarga «Sin temporizador».
        composeRule.onNodeWithTag(CreateRecipeTestTags.stepTimer(1)).performScrollTo()
            .assertTextContains(editorText(R.string.create_recipe_step_no_timer))
        composeRule.onNodeWithTag(CreateRecipeTestTags.FAVORITE_TOGGLE).performScrollTo().assertIsOn()

        composeRule.onNodeWithTag(CreateRecipeTestTags.SAVE).assertIsDisplayed()
        composeRule.onNodeWithText(editorText(R.string.create_recipe_save_changes)).assertIsDisplayed()
        // El único guardado es el dock; no hay botones retirados ni «Guardar» de cabecera.
        composeRule.onAllNodesWithTag("create_recipe_save_draft").assertCountEquals(0)
        composeRule.onAllNodesWithTag("create_recipe_publish").assertCountEquals(0)
        composeRule.onAllNodesWithTag(CreateRecipeTestTags.HEADER_SAVE).assertCountEquals(0)
    }

    // unificar-unidades · verify 1 (S2, D1/D5): una unidad legada fuera del enum se
    // muestra como texto en el desplegable y entra como opción extra seleccionada
    // junto a los cinco valores; si no se elige otra, al guardar se conserva igual.
    @Test
    fun legacyUnitIsShownAsTextAndOfferedAsAnExtraOption() {
        val repository = repositoryWith(
            seededRecipe().copy(
                ingredients = listOf(
                    RecipeIngredient(id = "$SEEDED_ID-i0", name = "Patata", quantity = 700.0, unit = "cucharadas"),
                ),
            ),
        )
        launchEditor(repository, SEEDED_ID)

        composeRule.onNodeWithTag(CreateRecipeTestTags.ingredientUnit(0)).performScrollTo()
            .assertTextContains("cucharadas")
        composeRule.onNodeWithTag(CreateRecipeTestTags.ingredientUnit(0)).performClick()
        listOf("ud", "g", "kg", "ml", "l").forEach { code ->
            composeRule.onNodeWithTag(optionTag(CreateRecipeTestTags.ingredientUnit(0), code)).assertIsDisplayed()
        }
        composeRule.onNodeWithTag(optionTag(CreateRecipeTestTags.ingredientUnit(0), "cucharadas"))
            .assertIsDisplayed()
        composeRule.onNodeWithTag(optionTag(CreateRecipeTestTags.ingredientUnit(0), "cucharadas")).performClick()
        composeRule.onNodeWithTag(CreateRecipeTestTags.SAVE).performClick()

        val saved = storedRecipes(repository).single()
        assertEquals("cucharadas", saved.ingredients.single().unit)
    }

    // S4/RF-EBF4 (verify 3, D11): una Receta nacida sin foto (Sugerencia/Pinche) se
    // puede editar y guardar sin foto y sin distintivo de estado.
    @Test
    fun photolessRecipeEditsAndSavesWithoutPhoto() {
        val repository = repositoryWith(seededRecipe(photoPath = null, origin = RecipeOrigin.SUGGESTION))
        launchEditor(repository, SEEDED_ID)

        composeRule.onNodeWithTag(CreateRecipeTestTags.SAVE).assertIsEnabled()
        composeRule.onAllNodesWithTag(CreateRecipeTestTags.SAVE_ERRORS).assertCountEquals(0)

        composeRule.onNodeWithTag(CreateRecipeTestTags.TITLE_FIELD).performTextReplacement("Tortilla sin foto mejorada")
        composeRule.onNodeWithTag(CreateRecipeTestTags.SAVE).performClick()

        val saved = storedRecipes(repository).single()
        assertEquals("Tortilla sin foto mejorada", saved.title)
        assertNull(saved.photoPath)
    }

    // S3/RF-EBF3: una Receta en edición que deja de cumplir los obligatorios tiene
    // «Guardar cambios» deshabilitado y la lista de requisitos junto al dock. D11: la
    // foto es siempre opcional y no aparece en la lista.
    @Test
    fun editingWithoutRequirementsBlocksSavingChanges() {
        val repository = repositoryWith(seededRecipeWithoutDetails())
        launchEditor(repository, SEEDED_ID)

        composeRule.onNodeWithTag(CreateRecipeTestTags.SAVE).assertIsNotEnabled()
        composeRule.onNodeWithTag(CreateRecipeTestTags.SAVE_ERRORS).assertIsDisplayed()
        composeRule.onNodeWithText(editorText(R.string.create_recipe_save_error_ingredient)).assertIsDisplayed()
        composeRule.onNodeWithText(editorText(R.string.create_recipe_save_error_step)).assertIsDisplayed()

        // Completa un ingrediente y un paso: «Guardar cambios» se habilita y guarda.
        composeRule.onNodeWithTag(CreateRecipeTestTags.ADD_INGREDIENT).performScrollTo().performClick()
        composeRule.onNodeWithTag(CreateRecipeTestTags.ingredientName(0)).performScrollTo()
            .performTextReplacement("Patata")
        composeRule.onNodeWithTag(CreateRecipeTestTags.ingredientQuantity(0)).performScrollTo()
            .performTextReplacement("700")
        composeRule.onNodeWithTag(CreateRecipeTestTags.ADD_STEP).performScrollTo().performClick()
        composeRule.onNodeWithTag(CreateRecipeTestTags.stepText(0)).performScrollTo()
            .performTextReplacement("Pochar la cebolla")

        composeRule.onNodeWithTag(CreateRecipeTestTags.SAVE).assertIsEnabled()
        composeRule.onAllNodesWithTag(CreateRecipeTestTags.SAVE_ERRORS).assertCountEquals(0)
        composeRule.onNodeWithTag(CreateRecipeTestTags.SAVE).performClick()

        val saved = storedRecipes(repository).single()
        assertEquals(1, saved.ingredients.size)
        assertEquals(1, saved.steps.size)
    }

    // RF-6 y RF-16: guardar los cambios mantiene el id y el origen de la Receta.
    @Test
    fun savingChangesKeepsIdAndOrigin() {
        val repository = repositoryWith(
            seededRecipe(origin = RecipeOrigin.SUGGESTION, photoPath = editPhotoFile("editar-publicada.png")),
        )
        launchEditor(repository, SEEDED_ID)

        composeRule.onNodeWithTag(CreateRecipeTestTags.TITLE_FIELD).performTextReplacement("Tortilla mejorada")
        composeRule.onNodeWithTag(CreateRecipeTestTags.SAVE).performClick()

        val saved = storedRecipes(repository).single()
        assertEquals(SEEDED_ID, saved.id)
        assertEquals("Tortilla mejorada", saved.title)
        assertEquals(RecipeOrigin.SUGGESTION, saved.origin)
        assertEquals(1, saved.ingredients.size)
        assertEquals(1, saved.steps.size)
    }

    // RF-13: en modo edición la cabecera no dibuja «Guardar»; el único punto de
    // guardado es la acción del dock.
    @Test
    fun editingHasNoHeaderSaveActionAndDockSaves() {
        val repository = repositoryWith(seededRecipe(photoPath = null))
        launchEditor(repository, SEEDED_ID)

        composeRule.onAllNodesWithTag(CreateRecipeTestTags.HEADER_SAVE).assertCountEquals(0)
        composeRule.onNodeWithTag(CreateRecipeTestTags.TITLE_FIELD).performTextReplacement("Tortilla desde dock")
        composeRule.onNodeWithTag(CreateRecipeTestTags.SAVE).performClick()

        val saved = storedRecipes(repository).single()
        assertEquals("Tortilla desde dock", saved.title)
    }

    // RF-14: con el dock deshabilitado no queda ninguna acción de guardado en la
    // cabecera que pudiera saltarse el bloqueo.
    @Test
    fun headerSaveIsAbsentWhileSaveIsDisabled() {
        launchEditor(repositoryWith(seededRecipeWithoutDetails()), SEEDED_ID)

        composeRule.onNodeWithTag(CreateRecipeTestTags.SAVE).assertIsNotEnabled()
        composeRule.onAllNodesWithTag(CreateRecipeTestTags.HEADER_SAVE).assertCountEquals(0)
    }

    // RF-15: cerrar sin guardar descarta los cambios (la Receta conserva sus datos).
    @Test
    fun closingWithoutSavingDiscardsChanges() {
        var closed = false
        val repository = repositoryWith(seededRecipe(photoPath = editPhotoFile("editar-cierre.png")))
        launchEditor(repository, SEEDED_ID, onDone = { closed = true })

        composeRule.onNodeWithTag(CreateRecipeTestTags.TITLE_FIELD).performTextReplacement("Cambios descartados")
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

        composeRule.onNodeWithText(editorText(R.string.create_recipe_edit_title)).assertIsDisplayed()
        composeRule.onNodeWithTag(CreateRecipeTestTags.SAVE).assertIsDisplayed()
        composeRule.onNodeWithText(editorText(R.string.create_recipe_save_changes)).assertIsDisplayed()
        assertEquals("Edit Recipe", editorText(R.string.create_recipe_edit_title))
        assertEquals("Save changes", editorText(R.string.create_recipe_save_changes))
    }

    // RF-4: la tarjeta de Mi recetario ofrece «Editar receta» como botón primario
    // compacto (icono `edit` de 18 dp + etiqueta «Editar»; excepción de diseño
    // aprobada por el usuario el 2026-10-19) y avisa con el id de la Receta.
    @Test
    fun cardEditActionRequestsEditingWithRecipeId() {
        var requestedId: String? = null
        showCards(seededRecipe(photoPath = editPhotoFile("editar-tarjeta.png"))) { requestedId = it }

        composeRule.onNodeWithTag(MyRecipesTestTags.RECIPE_EDIT).assertIsDisplayed()
        composeRule.onNodeWithText(editorText(R.string.my_recipes_action_edit_short)).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(editorText(R.string.my_recipes_action_edit)).assertIsDisplayed()
        composeRule.onNodeWithTag(MyRecipesTestTags.RECIPE_EDIT).performClick()

        assertEquals(SEEDED_ID, requestedId)
    }

    // NFR-1: la acción «Editar receta» de la tarjeta también está en inglés (etiqueta
    // visible «Edit» y nombre accesible «Edit recipe»).
    @Test
    fun cardEditActionIsLocalizedInEnglish() {
        showCards(seededRecipe(photoPath = editPhotoFile("editar-tarjeta-en.png")), locale = Locale.ENGLISH) {}

        assertEquals("Edit recipe", editorText(R.string.my_recipes_action_edit))
        composeRule.onNodeWithContentDescription("Edit recipe").assertIsDisplayed()
        composeRule.onNodeWithText("Edit").assertIsDisplayed()
    }

    // RF-VPI3 · S2 (verify 1): los vínculos cargan al editar («Patata» marcada en el
    // Paso 1, el Paso 2 sin ingredientes) y sobreviven a «Guardar cambios» con ids
    // estables de Ingredientes y Pasos (RF-VPI4).
    @Test
    fun editingKeepsStepIngredientLinks() {
        val linked = seededRecipe(photoPath = editPhotoFile("editar-vinculos.png")).copy(
            ingredients = listOf(
                RecipeIngredient(id = "$SEEDED_ID-i0", name = "Patata", quantity = 700.0, unit = "g"),
                RecipeIngredient(id = "$SEEDED_ID-i1", name = "Huevo", quantity = 2.0, unit = "ud"),
            ),
            steps = listOf(
                RecipeStep(
                    id = "$SEEDED_ID-s0",
                    text = "Pochar la cebolla",
                    timerMinutes = 5,
                    ingredientIds = listOf("$SEEDED_ID-i0"),
                ),
                RecipeStep(id = "$SEEDED_ID-s1", text = "Batir el huevo", ingredientIds = emptyList()),
            ),
        )
        val repository = repositoryWith(linked)
        launchEditor(repository, SEEDED_ID)

        // Precarga: cada Paso conserva su selección en el resumen.
        stepSummaryShouldBe(0, editorQuantity(1))
        stepSummaryShouldBe(1, editorText(R.string.create_recipe_step_ingredients_none))

        // El diálogo refleja la selección guardada; «Aceptar» sin cambios no altera nada.
        openStepIngredientsDialog(0)
        composeRule.onNodeWithTag(CreateRecipeTestTags.stepIngredientOption(0)).assertIsOn()
        composeRule.onNodeWithTag(CreateRecipeTestTags.stepIngredientOption(1)).assertIsOff()
        composeRule.onNodeWithTag(CreateRecipeTestTags.STEP_INGREDIENTS_ACCEPT).performClick()

        composeRule.onNodeWithTag(CreateRecipeTestTags.SAVE).performClick()
        val saved = storedRecipes(repository).single()
        assertEquals(listOf("$SEEDED_ID-i0"), saved.steps[0].ingredientIds)
        assertTrue(saved.steps[1].ingredientIds.isEmpty())
        assertEquals(listOf("$SEEDED_ID-i0", "$SEEDED_ID-i1"), saved.ingredients.map { it.id })
    }

    // RF-VPI5 · S5: una Receta con Pasos sin vínculos (creada por la IA o anterior a la
    // feature) sigue guardable y se puede vincular editando, sin requisitos nuevos (D3).
    @Test
    fun recipeWithoutLinksCanBeLinkedWithoutNewRequirements() {
        val repository = repositoryWith(
            seededRecipe(photoPath = editPhotoFile("editar-sin-vinculos.png")),
        )
        launchEditor(repository, SEEDED_ID)

        stepSummaryShouldBe(0, editorText(R.string.create_recipe_step_ingredients_none))
        composeRule.onNodeWithTag(CreateRecipeTestTags.SAVE).assertIsEnabled()

        linkStepIngredient(stepIndex = 0, ingredientIndex = 0)
        stepSummaryShouldBe(0, editorQuantity(1))

        // Vincular no añade requisitos: «Guardar cambios» sigue habilitado (D3) y el
        // vínculo se guarda al confirmar.
        composeRule.onNodeWithTag(CreateRecipeTestTags.SAVE).assertIsEnabled()
        composeRule.onNodeWithTag(CreateRecipeTestTags.SAVE).performClick()
        val saved = storedRecipes(repository).single()
        assertEquals(listOf(saved.ingredients.single().id), saved.steps.single().ingredientIds)
    }

    private fun editorQuantity(count: Int): String =
        editorContext.resources.getQuantityString(R.plurals.create_recipe_step_ingredients_count, count, count)

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

    private fun showCards(
        recipe: Recipe,
        locale: Locale = Locale.forLanguageTag("es-ES"),
        onEditRecipe: (String) -> Unit,
    ) {
        val baseContext = InstrumentationRegistry.getInstrumentation().targetContext
        val configuration = Configuration(baseContext.resources.configuration).apply { setLocale(locale) }
        editorContext = baseContext.createConfigurationContext(configuration)
        composeRule.setContent {
            CompositionLocalProvider(LocalContext provides editorContext) {
                WhatsForLunchTheme {
                    MyRecipesScreen(
                        uiState = MyRecipesUiState(
                            state = MyRecipesState.WithRecipes,
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
        // Sin foto por defecto (D11: la foto es siempre opcional) y con
        // ingredientes/pasos válidos para que el guardado no quede bloqueado.
        fun seededRecipe(
            origin: RecipeOrigin = RecipeOrigin.USER,
            photoPath: String? = null,
        ): Recipe = Recipe(
            id = SEEDED_ID,
            title = seededTitle,
            story = "Huevos, patata y cebolla",
            mealMoment = MealMoment.DINNER,
            prepMinutes = 10,
            difficulty = Difficulty.EASY,
            baseServings = 2,
            caloriesPerServing = 250,
            photoPath = photoPath,
            favorite = true,
            origin = origin,
            ingredients = listOf(
                RecipeIngredient(id = "$SEEDED_ID-i0", name = "Patata", quantity = 700.0, unit = "g"),
            ),
            steps = listOf(RecipeStep(id = "$SEEDED_ID-s0", text = "Pochar la cebolla", timerMinutes = 5)),
        )

        // Receta que ha dejado de cumplir los obligatorios de guardado (RF-EBF3):
        // sin ingredientes y sin pasos. D11: la foto es siempre opcional.
        fun seededRecipeWithoutDetails(): Recipe = Recipe(
            id = SEEDED_ID,
            title = seededTitle,
            story = "Huevos, patata y cebolla",
            mealMoment = MealMoment.DINNER,
            prepMinutes = 10,
            difficulty = Difficulty.EASY,
            baseServings = 2,
            photoPath = null,
        )
    }
}

// RF-18: si el id no corresponde a ninguna Receta, el formulario no se abre y se vuelve
// a Mi recetario. Se monta la Route real (ViewModel y repositorio de Koin) sobre una base
// de datos vacía.
@RunWith(AndroidJUnit4::class)
class EditRecipeRouteE2ETest {

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
                CreateRecipeRoute(onDone = { closed = true }, recipeId = "receta-que-no-existe")
            }
        }

        composeRule.waitUntil(timeoutMillis = 5_000) { closed }
        assertTrue(closed)
    }
}

// verify 1 · RF-6, RF-17: la app real edita el título de una Receta guardada y Mis
// Recetas muestra el título nuevo.
@RunWith(AndroidJUnit4::class)
class EditRecipeAppE2ETest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun clearDatabase() {
        E2eDatabase.clear()
    }

    @Test
    fun editedTitleAppearsInMyRecipes() {
        val repository: RecipeRepository = GlobalContext.get().get<RecipeRepository>()
        runBlocking {
            repository.saveRecipe(
                Recipe(
                    id = "editar-e2e-1",
                    title = "Tortilla de patatas",
                    story = "Huevos, patata y cebolla",
                    mealMoment = MealMoment.LUNCH,
                    prepMinutes = 10,
                    difficulty = Difficulty.EASY,
                    baseServings = 4,
                    photoPath = editPhotoFile("editar-app-e2e.png"),
                    ingredients = listOf(
                        RecipeIngredient(name = "Patata", quantity = 700.0, unit = "g"),
                    ),
                    steps = listOf(RecipeStep(text = "Pochar la cebolla")),
                ),
            )
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(MyRecipesTestTags.RECIPE_EDIT).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(MyRecipesTestTags.SCREEN).assertIsDisplayed()
        composeRule.onNodeWithTag(MyRecipesTestTags.RECIPE_EDIT).performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Tortilla de patatas").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(CreateRecipeTestTags.TITLE_FIELD).performTextReplacement("Tortilla mejorada")
        composeRule.onNodeWithTag(CreateRecipeTestTags.SAVE).assertIsEnabled()
        composeRule.onNodeWithTag(CreateRecipeTestTags.SAVE).performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Tortilla mejorada").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(MyRecipesTestTags.SCREEN).assertIsDisplayed()
        composeRule.onNodeWithText("Tortilla mejorada").assertIsDisplayed()
        assertEquals("Tortilla mejorada", runBlocking { repository.getRecipe("editar-e2e-1") }?.title)
    }
}
