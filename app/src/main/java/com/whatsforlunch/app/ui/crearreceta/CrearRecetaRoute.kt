package com.whatsforlunch.app.ui.crearreceta

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.whatsforlunch.app.data.ai.ImageGenerator
import com.whatsforlunch.app.data.photo.SystemPhotoAcquirer
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

// Une el ViewModel del formulario con la pantalla sin estado. Aquí se monta el
// adquirente de foto real (con los lanzadores del sistema) y se navega al guardar.
@Composable
fun CrearRecetaRoute(
    onDone: () -> Unit,
    viewModel: CrearRecetaViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val imageGenerator: ImageGenerator = koinInject()
    val photoAcquirer = remember(activity, imageGenerator) {
        activity?.let { SystemPhotoAcquirer(it, it.activityResultRegistry, imageGenerator) }
    }
    LaunchedEffect(photoAcquirer) {
        if (photoAcquirer != null) viewModel.setPhotoAcquirer(photoAcquirer)
    }
    LaunchedEffect(uiState.saved) {
        if (uiState.saved) onDone()
    }

    CrearRecetaScreen(uiState = uiState, actions = viewModel.toActions(onDone))
}

// Convierte los métodos del ViewModel en las acciones de la pantalla. Compartido por
// la Route y por las pruebas que montan la pantalla directamente.
fun CrearRecetaViewModel.toActions(onDone: () -> Unit): CrearRecetaActions = CrearRecetaActions(
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
    onGeneratePhoto = this::onGeneratePhotoWithAi,
    onRemovePhoto = this::onRemovePhoto,
    onSaveDraft = this::onSaveDraft,
    onPublish = this::onPublish,
    onDismissNotice = this::onDismissNotice,
)
