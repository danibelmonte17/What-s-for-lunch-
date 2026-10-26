package com.whatsforlunch.app.ui.createrecipe

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.whatsforlunch.app.data.ai.ImageGenerator
import com.whatsforlunch.app.data.photo.SystemPhotoAcquirer
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

// Une el ViewModel del formulario con la pantalla sin estado. Aquí se monta el
// adquirente de foto real (con los lanzadores del sistema), se abre en modo edición
// cuando llega un id de Receta y se navega al guardar.
@Composable
fun CreateRecipeRoute(
    onDone: () -> Unit,
    recipeId: String? = null,
    viewModel: CreateRecipeViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val language = LocalConfiguration.current.locales[0].language
    val activity = context as? ComponentActivity
    val imageGenerator: ImageGenerator = koinInject()
    val photoAcquirer = remember(activity, imageGenerator) {
        activity?.let { SystemPhotoAcquirer(it, it.activityResultRegistry, imageGenerator) }
    }
    LaunchedEffect(photoAcquirer) {
        if (photoAcquirer != null) viewModel.setPhotoAcquirer(photoAcquirer)
    }
    // RF-1, RF-2: precarga la Receta a editar; RF-18: si no existe, se vuelve a Mis
    // Recetas sin abrir el formulario. RF-17: al guardar, se vuelve a Mis Recetas.
    LaunchedEffect(recipeId) { viewModel.loadRecipe(recipeId) }
    LaunchedEffect(uiState.saved, uiState.recipeMissing) {
        if (uiState.saved || uiState.recipeMissing) onDone()
    }

    CreateRecipeScreen(uiState = uiState, actions = viewModel.toActions(onDone, language))
}

// Convierte los métodos del ViewModel en las acciones de la pantalla. Compartido por
// la Route y por las pruebas que montan la pantalla directamente.
// RF-15: cerrar (onBack) no persiste nada: los cambios se descartan al salir.
fun CreateRecipeViewModel.toActions(
    onDone: () -> Unit,
    language: String,
): CreateRecipeActions = CreateRecipeActions(
    onBack = onDone,
    onTitleChange = this::onTitleChange,
    onStoryChange = this::onStoryChange,
    onMealMomentChange = this::onMealMomentChange,
    onPrepMinutesChange = this::onPrepMinutesChange,
    onCookMinutesChange = this::onCookMinutesChange,
    onDifficultyChange = this::onDifficultyChange,
    onCaloriesChange = this::onCaloriesChange,
    onServingsChange = this::onServingsChange,
    onFavoriteToggle = this::onFavoriteToggle,
    onAddIngredient = this::onAddIngredient,
    onIngredientNameChange = this::onIngredientNameChange,
    onIngredientQuantityChange = this::onIngredientQuantityChange,
    onIngredientUnitChange = this::onIngredientUnitChange,
    onIngredientOptionalChange = this::onIngredientOptionalChange,
    onRemoveIngredient = this::onRemoveIngredient,
    onAddStep = this::onAddStep,
    onStepTextChange = this::onStepTextChange,
    onStepTimerChange = this::onStepTimerChange,
    onRemoveStep = this::onRemoveStep,
    onPickPhotoFromGallery = this::onPickPhotoFromGallery,
    onTakePhoto = this::onTakePhoto,
    onGeneratePhoto = { onGeneratePhotoWithAi(language) },
    onRemovePhoto = this::onRemovePhoto,
    onSaveDraft = this::onSaveDraft,
    onPublish = this::onPublish,
    onSaveChanges = this::onSaveChanges,
    onDismissNotice = this::onDismissNotice,
)
