package com.whatsforlunch.app

import com.whatsforlunch.app.data.ai.TestPromptSource
import com.whatsforlunch.app.data.photo.PhotoAcquirer
import com.whatsforlunch.app.data.photo.PhotoResult
import com.whatsforlunch.app.data.photo.PhotoStore
import com.whatsforlunch.app.data.repository.InMemoryRecipeRepository
import com.whatsforlunch.app.domain.createrecipe.RecipeRequirement
import com.whatsforlunch.app.domain.createrecipe.RecipeValidation
import com.whatsforlunch.app.domain.model.Recipe
import com.whatsforlunch.app.domain.model.RecipeIngredient
import com.whatsforlunch.app.domain.model.RecipeOrigin
import com.whatsforlunch.app.domain.model.RecipeStep
import com.whatsforlunch.app.ui.createrecipe.CreateRecipeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private class FakePhotoStore : PhotoStore {
    override suspend fun save(recipeId: String, bytes: ByteArray): String = "/tmp/$recipeId.jpg"

    override suspend fun delete(path: String) = Unit
}

private class FakePhotoAcquirer : PhotoAcquirer {
    override suspend fun pickFromGallery(): PhotoResult = PhotoResult.Success(ByteArray(1))

    override suspend fun takePhoto(): PhotoResult = PhotoResult.Success(ByteArray(1))

    override suspend fun generate(prompt: String): PhotoResult = PhotoResult.Success(ByteArray(1))
}

// Vínculo Paso↔Ingrediente (RD21, RF-VPI1 a RF-VPI5): asignación por Paso (N:M y
// opcional), limpieza al borrar filas, orden de ingredientes (D7) y round-trip de
// los ids estables (D5) al guardar y reabrir.
@OptIn(ExperimentalCoroutinesApi::class)
class CreateRecipeViewModelTest {

    @Before
    fun setMainDispatcher() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    // RF-VPI2 (D2): el toggle añade y quita un Ingrediente de un Paso y un Ingrediente
    // puede usarse en varios Pasos; D7: la selección se guarda en orden de ingredientes.
    @Test
    fun togglingStepIngredientsAssignsRemovesAndKeepsIngredientOrder() = runTest {
        val viewModel = viewModel()
        addIngredient(viewModel, index = 0, name = "Patatas")
        addIngredient(viewModel, index = 1, name = "Huevo")
        addIngredient(viewModel, index = 2, name = "Cebolla")
        viewModel.onAddStep()
        viewModel.onAddStep()
        val ingredients = viewModel.uiState.value.ingredients
        val firstStep = viewModel.uiState.value.steps[0]
        val secondStep = viewModel.uiState.value.steps[1]

        // Se marcan en orden inverso: se guardan en el orden de ingredientes (D7).
        viewModel.onStepIngredientToggle(firstStep.key, ingredients[2].id)
        viewModel.onStepIngredientToggle(firstStep.key, ingredients[0].id)
        assertEquals(
            listOf(ingredients[0].id, ingredients[2].id),
            stepLinks(viewModel)[0],
        )

        // N:M: el mismo Ingrediente puede usarse en otro Paso.
        viewModel.onStepIngredientToggle(secondStep.key, ingredients[0].id)
        assertEquals(listOf(ingredients[0].id), stepLinks(viewModel)[1])

        // Segundo toggle sobre el mismo Ingrediente lo quita del Paso.
        viewModel.onStepIngredientToggle(firstStep.key, ingredients[0].id)
        assertEquals(listOf(ingredients[2].id), stepLinks(viewModel)[0])
    }

    // D6 (Scenario 3): quitar un Ingrediente elimina sus vínculos de todos los Pasos,
    // conserva los de los demás y borrar un Paso solo borra ese Paso.
    @Test
    fun removingIngredientClearsItsLinksFromEveryStep() = runTest {
        val viewModel = viewModel()
        addIngredient(viewModel, index = 0, name = "Patatas")
        addIngredient(viewModel, index = 1, name = "Huevo")
        repeat(3) { viewModel.onAddStep() }
        val ingredients = viewModel.uiState.value.ingredients
        val steps = viewModel.uiState.value.steps
        viewModel.onStepIngredientToggle(steps[0].key, ingredients[0].id)
        viewModel.onStepIngredientToggle(steps[1].key, ingredients[1].id)
        viewModel.onStepIngredientToggle(steps[2].key, ingredients[0].id)

        viewModel.onRemoveIngredient(ingredients[0].key)

        assertEquals(listOf(emptyList<String>(), listOf(ingredients[1].id), emptyList()), stepLinks(viewModel))

        // Quitar un Paso solo borra ese Paso y sus vínculos (D6).
        viewModel.onRemoveStep(viewModel.uiState.value.steps[1].key)
        assertEquals(listOf(emptyList<String>(), emptyList()), stepLinks(viewModel))
    }

    // D6: renombrar un Ingrediente o cambiar su cantidad/unidad conserva
    // sus vínculos (son por id estable).
    @Test
    fun editingIngredientKeepsItsStepLinks() = runTest {
        val viewModel = viewModel()
        addIngredient(viewModel, index = 0, name = "Patatas")
        viewModel.onAddStep()
        val ingredient = viewModel.uiState.value.ingredients.single()
        val step = viewModel.uiState.value.steps.single()
        viewModel.onStepIngredientToggle(step.key, ingredient.id)

        viewModel.onIngredientNameChange(ingredient.key, "Patatas viejas")
        viewModel.onIngredientQuantityChange(ingredient.key, "300")
        viewModel.onIngredientUnitChange(ingredient.key, "g")

        assertEquals(listOf(ingredient.id), stepLinks(viewModel).single())
    }

    // RF-VPI3/RF-VPI4 (verify 1 = Scenario 2): los vínculos y los ids estables
    // sobreviven al guardar y a reabrir la Receta en edición.
    @Test
    fun savedRecipeKeepsStableIdsAndStepLinksWhenReopened() = runTest {
        val repository = InMemoryRecipeRepository()
        val viewModel = viewModel(repository)
        viewModel.onTitleChange("Tortilla de patatas")
        viewModel.setPhotoAcquirer(FakePhotoAcquirer())
        viewModel.onPickPhotoFromGallery()
        addIngredient(viewModel, index = 0, name = "Patatas")
        addIngredient(viewModel, index = 1, name = "Huevo")
        viewModel.onAddStep()
        viewModel.onAddStep()
        viewModel.onStepTextChange(viewModel.uiState.value.steps[0].key, "Pochar las patatas")
        viewModel.onStepTextChange(viewModel.uiState.value.steps[1].key, "Añadir el huevo")
        val ingredients = viewModel.uiState.value.ingredients
        viewModel.onStepIngredientToggle(viewModel.uiState.value.steps[0].key, ingredients[0].id)
        viewModel.onSave()
        assertTrue(viewModel.uiState.value.saved)

        val saved = repository.observeRecipes().first().single()
        assertTrue(saved.ingredients.all { it.id.isNotBlank() })
        assertEquals(2, saved.ingredients.map { it.id }.distinct().size)
        assertEquals(listOf(saved.ingredients[0].id), saved.steps[0].ingredientIds)
        assertTrue(saved.steps[1].ingredientIds.isEmpty())

        val reopened = viewModel(repository, recipeId = saved.id)
        val state = reopened.uiState.value
        assertEquals(saved.ingredients.map { it.id }, state.ingredients.map { it.id })
        assertEquals(listOf(saved.ingredients[0].id), state.steps[0].ingredientIds)
        assertTrue(state.steps[1].ingredientIds.isEmpty())
    }

    // RF-VPI5 (D4, Scenario 5): una Receta sin vínculos (creada por IA o legada) es
    // válida, se puede vincular editando y guardar no exige vínculos (D3).
    @Test
    fun recipesWithoutStepLinksAreValidAndCanGainLinks() = runTest {
        val saveableWithoutLinks = Recipe(
            id = "ia-1",
            title = "Arroz con tomate",
            story = "Una receta sencilla",
            photoPath = "/tmp/ia-1.jpg",
            origin = RecipeOrigin.SUGGESTION,
            ingredients = listOf(
                RecipeIngredient(id = "ia-1-ingredient-0", name = "Tomate", quantity = 1.0, unit = "ud"),
            ),
            steps = listOf(RecipeStep(text = "Cocinar el arroz", ingredientIds = emptyList())),
        )
        assertTrue(RecipeValidation.validateForSave(saveableWithoutLinks).isEmpty())

        val repository = InMemoryRecipeRepository()
        repository.saveRecipe(saveableWithoutLinks)
        val viewModel = viewModel(repository, recipeId = "ia-1")
        val step = viewModel.uiState.value.steps.single()
        val ingredient = viewModel.uiState.value.ingredients.single()
        assertTrue(step.ingredientIds.isEmpty())

        viewModel.onStepIngredientToggle(step.key, ingredient.id)
        viewModel.onSave()
        assertTrue(viewModel.uiState.value.saved)

        val saved = repository.getRecipe("ia-1")!!
        assertEquals(listOf(saved.ingredients.single().id), saved.steps.single().ingredientIds)
    }

    // D11: al editar una Receta sin foto (Sugerencia/Pinche) el guardado no exige
    // foto — la foto es siempre opcional.
    @Test
    fun editingARecipeWithoutPhotoSavesWithoutPhoto() = runTest {
        val photoless = Recipe(
            id = "pinche-1",
            title = "Crema de calabaza",
            photoPath = null,
            origin = RecipeOrigin.SUGGESTION,
            ingredients = listOf(
                RecipeIngredient(id = "pinche-1-ingredient-0", name = "Calabaza", quantity = 500.0, unit = "g"),
            ),
            steps = listOf(RecipeStep(text = "Triturar", ingredientIds = emptyList())),
        )
        val repository = InMemoryRecipeRepository()
        repository.saveRecipe(photoless)
        val viewModel = viewModel(repository, recipeId = "pinche-1")
        assertTrue(viewModel.uiState.value.canSave)

        viewModel.onTitleChange("Crema de calabaza tostada")
        viewModel.onSave()
        assertTrue(viewModel.uiState.value.saved)
        assertEquals("Crema de calabaza tostada", repository.getRecipe("pinche-1")!!.title)
    }

    // RF-EBF2/RF-EBF3 + D11: la foto es siempre opcional; el alta guarda sin foto
    // cuando el resto de obligatorios están completos.
    @Test
    fun createSavesWithoutPhoto() = runTest {
        val repository = InMemoryRecipeRepository()
        val viewModel = viewModel(repository)
        viewModel.onTitleChange("Tortilla de patatas")
        addIngredient(viewModel, index = 0, name = "Patatas")
        viewModel.onAddStep()
        viewModel.onStepTextChange(viewModel.uiState.value.steps[0].key, "Pochar")
        assertTrue(viewModel.uiState.value.canSave)
        assertEquals(emptySet<RecipeRequirement>(), viewModel.uiState.value.saveErrors)

        viewModel.onSave()
        assertTrue(viewModel.uiState.value.saved)
        assertEquals(null, repository.observeRecipes().first().single().photoPath)
    }

    // D3: guardar no exige vínculos (RecipeValidation sin cambios) — una Receta
    // completa con Pasos sin vincular guarda sin requisitos nuevos.
    @Test
    fun saveValidationDoesNotRequireStepLinks() {
        val unmet = RecipeValidation.validateForSave(
            Recipe(
                id = "sin-vinculos",
                title = "Tarta de manzana",
                photoPath = "/tmp/tarta.jpg",
                ingredients = listOf(
                    RecipeIngredient(name = "Manzana", quantity = 3.0, unit = "ud"),
                ),
                steps = listOf(
                    RecipeStep(text = "Hornear 40 min", timerMinutes = 40, ingredientIds = emptyList()),
                ),
            ),
        )
        assertEquals(emptySet<RecipeRequirement>(), unmet)
    }

    // RF-VPI4: las filas nuevas de Ingrediente reciben un id estable no vacío y único,
    // del que dependen los vínculos de los Pasos.
    @Test
    fun newIngredientRowsGetUniqueStableIds() = runTest {
        val viewModel = viewModel()
        addIngredient(viewModel, index = 0, name = "Patatas")
        addIngredient(viewModel, index = 1, name = "Huevo")
        val ids = viewModel.uiState.value.ingredients.map { it.id }
        assertTrue(ids.all { it.isNotBlank() })
        assertEquals(2, ids.distinct().size)
    }

    private fun viewModel(
        repository: InMemoryRecipeRepository = InMemoryRecipeRepository(),
        recipeId: String? = null,
    ) = CreateRecipeViewModel(repository, FakePhotoStore(), TestPromptSource(), recipeId)

    private fun addIngredient(viewModel: CreateRecipeViewModel, index: Int, name: String) {
        viewModel.onAddIngredient()
        val row = viewModel.uiState.value.ingredients[index]
        viewModel.onIngredientNameChange(row.key, name)
        viewModel.onIngredientQuantityChange(row.key, "1")
    }

    private fun stepLinks(viewModel: CreateRecipeViewModel): List<List<String>> =
        viewModel.uiState.value.steps.map { it.ingredientIds }
}
