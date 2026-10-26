package com.whatsforlunch.app.ui.createrecipe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whatsforlunch.app.data.ai.AiFailureCause
import com.whatsforlunch.app.data.ai.PromptSource
import com.whatsforlunch.app.data.ai.renderDishPhotoPrompt
import com.whatsforlunch.app.data.photo.NoOpPhotoAcquirer
import com.whatsforlunch.app.data.photo.PhotoAcquirer
import com.whatsforlunch.app.data.photo.PhotoResult
import com.whatsforlunch.app.data.photo.PhotoStore
import com.whatsforlunch.app.domain.createrecipe.RecipeRequirement
import com.whatsforlunch.app.domain.createrecipe.RecipeValidation
import com.whatsforlunch.app.domain.model.Difficulty
import com.whatsforlunch.app.domain.model.MealMoment
import com.whatsforlunch.app.domain.model.Recipe
import com.whatsforlunch.app.domain.model.RecipeIngredient
import com.whatsforlunch.app.domain.model.RecipeOrigin
import com.whatsforlunch.app.domain.model.RecipeStatus
import com.whatsforlunch.app.domain.model.RecipeStep
import com.whatsforlunch.app.domain.repository.RecipeRepository
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Aviso no bloqueante del formulario (RF-21, RF-22, RF-23).
enum class CreateRecipeNotice {
    AI_KEY_MISSING,
    AI_GENERATION_FAILED,
    AI_GENERATION_FAILED_NO_CONNECTION,
    AI_GENERATION_FAILED_PROVIDER_ERROR,
    AI_GENERATION_FAILED_INVALID_RESPONSE,
    CAMERA_PERMISSION_DENIED,
}

// Fila editable de Ingrediente en el formulario (RF-5, RF-6, RF-8).
data class IngredientFieldUiState(
    val key: Long,
    val name: String = "",
    val quantity: String = "",
    val unit: String = "",
    val optional: Boolean = false,
) {
    val isValid: Boolean
        get() = name.isNotBlank() && (quantity.toDoubleOrNull() ?: 0.0) > 0.0
}

// Fila editable de Paso en el formulario (RF-9, RF-10, RF-12).
data class StepFieldUiState(
    val key: Long,
    val text: String = "",
    val timerMinutes: String = "",
) {
    val isValid: Boolean
        get() = text.isNotBlank()
}

// Estado visible del formulario de Receta: alta (recipeId null) o edición de una
// Receta existente (Parte B, T2-T7).
data class CreateRecipeUiState(
    val title: String = "",
    val story: String = "",
    val mealMoment: MealMoment = MealMoment.LUNCH,
    val prepMinutes: String = "0",
    val cookMinutes: String = "0",
    val difficulty: Difficulty = Difficulty.MEDIUM,
    val baseServings: Int = 4,
    val caloriesPerServing: String = "",
    val photoPath: String? = null,
    val favorite: Boolean = false,
    val ingredients: List<IngredientFieldUiState> = emptyList(),
    val steps: List<StepFieldUiState> = emptyList(),
    val titleError: Boolean = false,
    // Requisitos incumplidos de Publicar, en vivo (fuente única:
    // RecipeValidation.validateForPublish) y si el formulario ya los cumple.
    val publishErrors: Set<RecipeRequirement> = emptySet(),
    val canPublish: Boolean = false,
    val notice: CreateRecipeNotice? = null,
    val isPhotoLoading: Boolean = false,
    val saved: Boolean = false,
    // Modo edición: id de la Receta abierta (null = alta) y el estado con el que se
    // guardará si no se publica (RF-5, RF-6, RF-9, RF-10, RF-16).
    val recipeId: String? = null,
    val editingStatus: RecipeStatus? = null,
    // RF-18: el id recibido no corresponde a ninguna Receta; se vuelve a Mis Recetas.
    val recipeMissing: Boolean = false,
)

// MVVM + StateFlow: el formulario de alta y de edición de Receta. Las reglas de
// validación viven en domain/ (RecipeValidation); aquí solo se orquesta el estado y
// el guardado.
class CreateRecipeViewModel(
    private val repository: RecipeRepository,
    private val photoStore: PhotoStore,
    private val promptSource: PromptSource,
    // Id de la Receta a editar; null = alta de una Receta nueva (RF-1).
    initialRecipeId: String? = null,
) : ViewModel() {

    // Id reservado para la Receta nueva: en alta no hay Receta previa (RF-2).
    private val newRecipeId: String = UUID.randomUUID().toString()

    // Origen de la Receta guardada; la edición lo conserva (RF-16).
    private var originalOrigin: RecipeOrigin = RecipeOrigin.USER

    private val _uiState = MutableStateFlow(withPublishState(CreateRecipeUiState()))
    val uiState: StateFlow<CreateRecipeUiState> = _uiState.asStateFlow()

    init {
        loadRecipe(initialRecipeId)
    }

    // La Route inyecta la implementación real (con los lanzadores del sistema);
    // los tests inyectan un fake. Por defecto no hay adquisición de foto.
    private var photoAcquirer: PhotoAcquirer = NoOpPhotoAcquirer()

    fun setPhotoAcquirer(acquirer: PhotoAcquirer) {
        photoAcquirer = acquirer
    }

    // RF-1, RF-2: abre el formulario en modo edición precargando los datos de la
    // Receta; RF-18: si el id no corresponde a ninguna Receta, avisa para volver a
    // Mis Recetas sin abrir el formulario. Idempotente: no recarga la misma Receta.
    fun loadRecipe(id: String?) {
        if (id == null || id == _uiState.value.recipeId) return
        viewModelScope.launch {
            val recipe = repository.getRecipe(id)
            if (recipe == null) {
                mutate { it.copy(recipeMissing = true) }
            } else {
                originalOrigin = recipe.origin
                mutate { preloadedState(recipe) }
            }
        }
    }

    fun onTitleChange(value: String) = mutate { it.copy(title = value, titleError = false) }

    fun onStoryChange(value: String) = mutate { it.copy(story = value) }

    fun onMealMomentChange(moment: MealMoment) = mutate { it.copy(mealMoment = moment) }

    fun onPrepMinutesChange(value: String) = mutate { it.copy(prepMinutes = value) }

    fun onCookMinutesChange(value: String) = mutate { it.copy(cookMinutes = value) }

    fun onDifficultyChange(difficulty: Difficulty) = mutate { it.copy(difficulty = difficulty) }

    fun onCaloriesChange(value: String) = mutate { it.copy(caloriesPerServing = value) }

    fun onServingsChange(value: Int) = mutate {
        it.copy(
            baseServings = value.coerceIn(
                RecipeValidation.MIN_BASE_SERVINGS,
                RecipeValidation.MAX_BASE_SERVINGS,
            ),
        )
    }

    fun onFavoriteToggle() = mutate { it.copy(favorite = !it.favorite) }

    fun onAddIngredient() = mutate {
        it.copy(ingredients = it.ingredients + IngredientFieldUiState(key = nextKey()))
    }

    fun onIngredientNameChange(key: Long, value: String) = updateIngredient(key) { it.copy(name = value) }

    fun onIngredientQuantityChange(key: Long, value: String) = updateIngredient(key) { it.copy(quantity = value) }

    fun onIngredientUnitChange(key: Long, value: String) = updateIngredient(key) { it.copy(unit = value) }

    fun onIngredientOptionalChange(key: Long, value: Boolean) = updateIngredient(key) { it.copy(optional = value) }

    fun onRemoveIngredient(key: Long) = mutate { state ->
        state.copy(ingredients = state.ingredients.filterNot { it.key == key })
    }

    fun onAddStep() = mutate {
        it.copy(steps = it.steps + StepFieldUiState(key = nextKey()))
    }

    fun onStepTextChange(key: Long, value: String) = updateStep(key) { it.copy(text = value) }

    fun onStepTimerChange(key: Long, value: String) = updateStep(key) { it.copy(timerMinutes = value) }

    fun onRemoveStep(key: Long) = mutate { state ->
        state.copy(steps = state.steps.filterNot { it.key == key })
    }

    fun onPickPhotoFromGallery() = acquirePhoto { photoAcquirer.pickFromGallery() }

    fun onTakePhoto() = acquirePhoto { photoAcquirer.takePhoto() }

    fun onGeneratePhotoWithAi(language: String) = acquirePhoto {
        photoAcquirer.generate(
            renderDishPhotoPrompt(
                title = _uiState.value.title,
                story = _uiState.value.story,
                language = language,
                source = promptSource,
            ),
        )
    }

    fun onRemovePhoto() {
        val current = _uiState.value.photoPath
        mutate { it.copy(photoPath = null) }
        if (current != null) viewModelScope.launch { photoStore.delete(current) }
    }

    fun onDismissNotice() = mutate { it.copy(notice = null) }

    // RF-10: guarda manteniendo el estado Borrador (también en modo edición).
    fun onSaveDraft() {
        val recipe = buildRecipe(_uiState.value, RecipeStatus.DRAFT)
        if (RecipeRequirement.TITLE in RecipeValidation.validateForDraft(recipe)) {
            mutate { it.copy(titleError = true) }
        } else {
            persist(recipe)
        }
    }

    // RF-11: publica la Receta (alta o transición Borrador -> Publicada al editar).
    fun onPublish() {
        val recipe = buildRecipe(_uiState.value, RecipeStatus.PUBLISHED)
        val unmet = RecipeValidation.validateForPublish(recipe)
        if (unmet.isEmpty()) {
            persist(recipe)
        } else {
            // Defensivo: el botón llega deshabilitado mientras haya requisitos incumplidos.
            mutate { it.copy(titleError = RecipeRequirement.TITLE in unmet) }
        }
    }

    // RF-6: guarda los cambios de una Receta Publicada manteniendo su estado, solo si
    // sigue cumpliendo los requisitos de Publicar (RF-7).
    fun onSaveChanges() {
        val recipe = buildRecipe(_uiState.value, _uiState.value.editingStatus ?: RecipeStatus.PUBLISHED)
        val unmet = RecipeValidation.validateForPublish(recipe)
        if (unmet.isEmpty()) {
            persist(recipe)
        } else {
            mutate { it.copy(titleError = RecipeRequirement.TITLE in unmet) }
        }
    }

    private fun acquirePhoto(acquire: suspend () -> PhotoResult) {
        mutate { it.copy(isPhotoLoading = true, notice = null) }
        viewModelScope.launch {
            when (val result = acquire()) {
                is PhotoResult.Success -> {
                    val path = photoStore.save(currentRecipeId(), result.bytes)
                    mutate { it.copy(photoPath = path, isPhotoLoading = false) }
                }
                PhotoResult.Cancelled -> mutate { it.copy(isPhotoLoading = false) }
                PhotoResult.CameraPermissionDenied ->
                    mutate { it.copy(isPhotoLoading = false, notice = CreateRecipeNotice.CAMERA_PERMISSION_DENIED) }
                PhotoResult.MissingApiKey ->
                    mutate { it.copy(isPhotoLoading = false, notice = CreateRecipeNotice.AI_KEY_MISSING) }
                is PhotoResult.GenerationFailed ->
                    mutate {
                        it.copy(
                            isPhotoLoading = false,
                            notice = result.cause.toNotice(),
                        )
                    }
            }
        }
    }

    private fun AiFailureCause.toNotice(): CreateRecipeNotice = when (this) {
        AiFailureCause.NO_CONNECTION -> CreateRecipeNotice.AI_GENERATION_FAILED_NO_CONNECTION
        AiFailureCause.PROVIDER_ERROR -> CreateRecipeNotice.AI_GENERATION_FAILED_PROVIDER_ERROR
        AiFailureCause.INVALID_RESPONSE -> CreateRecipeNotice.AI_GENERATION_FAILED_INVALID_RESPONSE
    }

    private fun persist(recipe: Recipe) {
        viewModelScope.launch {
            repository.saveRecipe(recipe)
            mutate { it.copy(saved = true) }
        }
    }

    private fun buildRecipe(state: CreateRecipeUiState, status: RecipeStatus): Recipe {
        // En modo edición se conservan el id y el origen de la Receta (RF-2, RF-16).
        val id = state.recipeId ?: newRecipeId
        return Recipe(
            id = id,
            title = state.title.trim(),
            story = state.story.trim(),
            mealMoment = state.mealMoment,
            prepMinutes = state.prepMinutes.toIntOrNull()?.coerceAtLeast(0) ?: 0,
            cookMinutes = state.cookMinutes.toIntOrNull()?.coerceAtLeast(0) ?: 0,
            difficulty = state.difficulty,
            baseServings = state.baseServings.coerceIn(
                RecipeValidation.MIN_BASE_SERVINGS,
                RecipeValidation.MAX_BASE_SERVINGS,
            ),
            caloriesPerServing = state.caloriesPerServing.toIntOrNull()?.takeIf { it >= 0 },
            photoPath = state.photoPath,
            favorite = state.favorite,
            status = status,
            origin = originalOrigin,
            ingredients = state.ingredients.mapIndexed { index, row ->
                RecipeIngredient(
                    id = "$id-ingredient-$index",
                    name = row.name.trim(),
                    quantity = row.quantity.toDoubleOrNull() ?: 0.0,
                    unit = row.unit.trim(),
                    optional = row.optional,
                )
            },
            steps = state.steps.mapIndexed { index, row ->
                RecipeStep(
                    id = "$id-step-$index",
                    text = row.text.trim(),
                    timerMinutes = row.timerMinutes.toIntOrNull()?.takeIf { it >= 0 },
                )
            },
        )
    }

    // Id con el que se guarda la Receta: el de la Receta en edición o el reservado
    // para el alta (RF-2).
    private fun currentRecipeId(): String = _uiState.value.recipeId ?: newRecipeId

    // RF-2: precarga el formulario con todos los datos de la Receta editada,
    // incluidas sus filas de Ingrediente y Paso y su foto y favorito.
    private fun preloadedState(recipe: Recipe): CreateRecipeUiState = CreateRecipeUiState(
        title = recipe.title,
        story = recipe.story,
        mealMoment = recipe.mealMoment,
        prepMinutes = recipe.prepMinutes.toString(),
        cookMinutes = recipe.cookMinutes.toString(),
        difficulty = recipe.difficulty,
        baseServings = recipe.baseServings.coerceIn(
            RecipeValidation.MIN_BASE_SERVINGS,
            RecipeValidation.MAX_BASE_SERVINGS,
        ),
        caloriesPerServing = recipe.caloriesPerServing?.toString() ?: "",
        photoPath = recipe.photoPath,
        favorite = recipe.favorite,
        ingredients = recipe.ingredients.map { ingredient ->
            IngredientFieldUiState(
                key = nextKey(),
                name = ingredient.name,
                quantity = formatQuantity(ingredient.quantity),
                unit = ingredient.unit,
                optional = ingredient.optional,
            )
        },
        steps = recipe.steps.map { step ->
            StepFieldUiState(
                key = nextKey(),
                text = step.text,
                timerMinutes = step.timerMinutes?.toString() ?: "",
            )
        },
        recipeId = recipe.id,
        editingStatus = recipe.status,
    )

    // Cualquier cambio recalcula los requisitos incumplidos de Publicar y si el
    // formulario puede publicar (RF-14 a RF-16); fuente única de verdad: validateForPublish.
    private fun mutate(transform: (CreateRecipeUiState) -> CreateRecipeUiState) {
        _uiState.update { withPublishState(transform(it)) }
    }

    private fun withPublishState(state: CreateRecipeUiState): CreateRecipeUiState {
        val unmet = RecipeValidation.validateForPublish(buildRecipe(state, RecipeStatus.PUBLISHED))
        return state.copy(publishErrors = unmet, canPublish = unmet.isEmpty())
    }

    private fun updateIngredient(key: Long, transform: (IngredientFieldUiState) -> IngredientFieldUiState) =
        mutate { state ->
            state.copy(
                ingredients = state.ingredients.map { if (it.key == key) transform(it) else it },
            )
        }

    private fun updateStep(key: Long, transform: (StepFieldUiState) -> StepFieldUiState) =
        mutate { state ->
            state.copy(
                steps = state.steps.map { if (it.key == key) transform(it) else it },
            )
        }

    private fun nextKey(): Long = keyCounter++

    private var keyCounter = 1L
}

// Cantidad de una fila de Ingrediente sin decimales innecesarios (700.0 -> "700"),
// para que la precarga del modo edición muestre el mismo valor que el alta (RF-2).
private fun formatQuantity(quantity: Double): String =
    if (quantity % 1.0 == 0.0) quantity.toLong().toString() else quantity.toString()
