package com.whatsforlunch.app.ui.crearreceta

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whatsforlunch.app.data.photo.NoOpPhotoAcquirer
import com.whatsforlunch.app.data.photo.PhotoAcquirer
import com.whatsforlunch.app.data.photo.PhotoResult
import com.whatsforlunch.app.data.photo.PhotoStore
import com.whatsforlunch.app.domain.crearreceta.RecipeRequirement
import com.whatsforlunch.app.domain.crearreceta.RecipeValidation
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
enum class CrearRecetaNotice {
    AI_KEY_MISSING,
    AI_GENERATION_FAILED,
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

// Estado visible del formulario de alta de Receta (Parte B, T3-T7).
data class CrearRecetaUiState(
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
    val publishErrors: Set<RecipeRequirement> = emptySet(),
    val notice: CrearRecetaNotice? = null,
    val isPhotoLoading: Boolean = false,
    val saved: Boolean = false,
)

// MVVM + StateFlow: el formulario de alta. Las reglas de validación viven en domain/
// (RecipeValidation); aquí solo se orquesta el estado y el guardado.
class CrearRecetaViewModel(
    private val repository: RecipeRepository,
    private val photoStore: PhotoStore,
) : ViewModel() {

    private val recipeId: String = UUID.randomUUID().toString()

    private val _uiState = MutableStateFlow(CrearRecetaUiState())
    val uiState: StateFlow<CrearRecetaUiState> = _uiState.asStateFlow()

    // La Route inyecta la implementación real (con los lanzadores del sistema);
    // los tests inyectan un fake. Por defecto no hay adquisición de foto.
    private var photoAcquirer: PhotoAcquirer = NoOpPhotoAcquirer()

    fun setPhotoAcquirer(acquirer: PhotoAcquirer) {
        photoAcquirer = acquirer
    }

    fun onTitleChange(value: String) = _uiState.update { it.copy(title = value, titleError = false) }

    fun onStoryChange(value: String) = _uiState.update { it.copy(story = value) }

    fun onMealMomentChange(moment: MealMoment) = _uiState.update { it.copy(mealMoment = moment) }

    fun onPrepMinutesChange(value: String) = _uiState.update { it.copy(prepMinutes = value) }

    fun onCookMinutesChange(value: String) = _uiState.update { it.copy(cookMinutes = value) }

    fun onDifficultyChange(difficulty: Difficulty) = _uiState.update { it.copy(difficulty = difficulty) }

    fun onCaloriesChange(value: String) = _uiState.update { it.copy(caloriesPerServing = value) }

    fun onServingsChange(value: Int) = _uiState.update {
        it.copy(
            baseServings = value.coerceIn(
                RecipeValidation.MIN_BASE_SERVINGS,
                RecipeValidation.MAX_BASE_SERVINGS,
            ),
        )
    }

    fun onFavoriteToggle() = _uiState.update { it.copy(favorite = !it.favorite) }

    fun onAddIngredient() = _uiState.update {
        it.copy(ingredients = it.ingredients + IngredientFieldUiState(key = nextKey()))
    }

    fun onIngredientNameChange(key: Long, value: String) = updateIngredient(key) { it.copy(name = value) }

    fun onIngredientQuantityChange(key: Long, value: String) = updateIngredient(key) { it.copy(quantity = value) }

    fun onIngredientUnitChange(key: Long, value: String) = updateIngredient(key) { it.copy(unit = value) }

    fun onIngredientOptionalChange(key: Long, value: Boolean) = updateIngredient(key) { it.copy(optional = value) }

    fun onRemoveIngredient(key: Long) = _uiState.update { state ->
        state.copy(ingredients = state.ingredients.filterNot { it.key == key })
    }

    fun onAddStep() = _uiState.update {
        it.copy(steps = it.steps + StepFieldUiState(key = nextKey()))
    }

    fun onStepTextChange(key: Long, value: String) = updateStep(key) { it.copy(text = value) }

    fun onStepTimerChange(key: Long, value: String) = updateStep(key) { it.copy(timerMinutes = value) }

    fun onRemoveStep(key: Long) = _uiState.update { state ->
        state.copy(steps = state.steps.filterNot { it.key == key })
    }

    fun onPickPhotoFromGallery() = acquirePhoto { photoAcquirer.pickFromGallery() }

    fun onTakePhoto() = acquirePhoto { photoAcquirer.takePhoto() }

    fun onGeneratePhotoWithAi() = acquirePhoto { photoAcquirer.generate(photoPrompt()) }

    fun onRemovePhoto() {
        val current = _uiState.value.photoPath
        _uiState.update { it.copy(photoPath = null) }
        if (current != null) viewModelScope.launch { photoStore.delete(current) }
    }

    fun onDismissNotice() = _uiState.update { it.copy(notice = null) }

    fun onSaveDraft() {
        val recipe = buildRecipe(status = RecipeStatus.DRAFT)
        if (RecipeRequirement.TITLE in RecipeValidation.validateForDraft(recipe)) {
            _uiState.update { it.copy(titleError = true) }
        } else {
            persist(recipe)
        }
    }

    fun onPublish() {
        val recipe = buildRecipe(status = RecipeStatus.PUBLISHED)
        val unmet = RecipeValidation.validateForPublish(recipe)
        if (unmet.isEmpty()) {
            persist(recipe)
        } else {
            _uiState.update {
                it.copy(titleError = RecipeRequirement.TITLE in unmet, publishErrors = unmet)
            }
        }
    }

    private fun acquirePhoto(acquire: suspend () -> PhotoResult) {
        _uiState.update { it.copy(isPhotoLoading = true, notice = null) }
        viewModelScope.launch {
            when (val result = acquire()) {
                is PhotoResult.Success -> {
                    val path = photoStore.save(recipeId, result.bytes)
                    _uiState.update { it.copy(photoPath = path, isPhotoLoading = false) }
                }
                PhotoResult.Cancelled -> _uiState.update { it.copy(isPhotoLoading = false) }
                PhotoResult.CameraPermissionDenied ->
                    _uiState.update { it.copy(isPhotoLoading = false, notice = CrearRecetaNotice.CAMERA_PERMISSION_DENIED) }
                PhotoResult.MissingApiKey ->
                    _uiState.update { it.copy(isPhotoLoading = false, notice = CrearRecetaNotice.AI_KEY_MISSING) }
                PhotoResult.GenerationFailed ->
                    _uiState.update { it.copy(isPhotoLoading = false, notice = CrearRecetaNotice.AI_GENERATION_FAILED) }
            }
        }
    }

    private fun persist(recipe: Recipe) {
        viewModelScope.launch {
            repository.saveRecipe(recipe)
            _uiState.update { it.copy(saved = true) }
        }
    }

    private fun photoPrompt(): String {
        val state = _uiState.value
        val dish = state.title.ifBlank { state.story }.trim()
        return buildString {
            append("Fotografía gastronómica apetitosa del plato \"")
            append(dish)
            append('"')
            if (state.story.isNotBlank()) {
                append(". ")
                append(state.story.trim())
            }
            append('.')
        }
    }

    private fun buildRecipe(status: RecipeStatus): Recipe {
        val state = _uiState.value
        return Recipe(
            id = recipeId,
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
            origin = RecipeOrigin.USER,
            ingredients = state.ingredients.mapIndexed { index, row ->
                RecipeIngredient(
                    id = "$recipeId-ingredient-$index",
                    name = row.name.trim(),
                    quantity = row.quantity.toDoubleOrNull() ?: 0.0,
                    unit = row.unit.trim(),
                    optional = row.optional,
                )
            },
            steps = state.steps.mapIndexed { index, row ->
                RecipeStep(
                    id = "$recipeId-step-$index",
                    text = row.text.trim(),
                    timerMinutes = row.timerMinutes.toIntOrNull()?.takeIf { it >= 0 },
                )
            },
        )
    }

    private fun updateIngredient(key: Long, transform: (IngredientFieldUiState) -> IngredientFieldUiState) =
        _uiState.update { state ->
            state.copy(
                ingredients = state.ingredients.map { if (it.key == key) transform(it) else it },
                publishErrors = emptySet(),
            )
        }

    private fun updateStep(key: Long, transform: (StepFieldUiState) -> StepFieldUiState) =
        _uiState.update { state ->
            state.copy(
                steps = state.steps.map { if (it.key == key) transform(it) else it },
                publishErrors = emptySet(),
            )
        }

    private fun nextKey(): Long = keyCounter++

    private var keyCounter = 1L
}
