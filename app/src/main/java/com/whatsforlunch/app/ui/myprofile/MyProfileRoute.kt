package com.whatsforlunch.app.ui.myprofile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel

// Une el ViewModel (MVVM + StateFlow) con la pantalla sin estado (`crear-mi-perfil`,
// T3). El idioma no interviene: los textos salen de resources con paridad es/en.
@Composable
fun MyProfileRoute(
    modifier: Modifier = Modifier,
    scrollToTopTick: Int = 0,
    viewModel: MyProfileViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    MyProfileScreen(
        uiState = uiState,
        onAllergyInputChange = viewModel::onAllergyInputChange,
        onAddAllergy = viewModel::addAllergy,
        onRemoveAllergy = viewModel::removeAllergy,
        onAllergyNoneChange = viewModel::setAllergyNone,
        onIntoleranceInputChange = viewModel::onIntoleranceInputChange,
        onAddIntolerance = viewModel::addIntolerance,
        onRemoveIntolerance = viewModel::removeIntolerance,
        onIntoleranceNoneChange = viewModel::setIntoleranceNone,
        onDietTypeSelected = viewModel::selectDietType,
        scrollToTopTick = scrollToTopTick,
        modifier = modifier,
    )
}
