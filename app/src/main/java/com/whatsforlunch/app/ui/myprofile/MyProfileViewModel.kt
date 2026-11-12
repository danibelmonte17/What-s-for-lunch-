package com.whatsforlunch.app.ui.myprofile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whatsforlunch.app.domain.model.DietType
import com.whatsforlunch.app.domain.model.ProfileSection
import com.whatsforlunch.app.domain.model.UserProfile
import com.whatsforlunch.app.domain.profile.UserProfileRules
import com.whatsforlunch.app.domain.repository.UserProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MyProfileUiState(
    val profile: UserProfile = UserProfile.empty,
    val allergyInput: String = "",
    val intoleranceInput: String = "",
)

/**
 * Estado de «Mi perfil» (`crear-mi-perfil`): el perfil observable desde Room y el
 * texto de cada campo de alta. Todas las mutaciones pasan por `UserProfileRules`
 * (RD25) antes de persistir; la UI no contiene reglas de negocio (P25).
 */
class MyProfileViewModel(
    private val userProfileRepository: UserProfileRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyProfileUiState())
    val uiState: StateFlow<MyProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            userProfileRepository.observeProfile().collect { profile ->
                _uiState.update { it.copy(profile = profile) }
            }
        }
    }

    fun onAllergyInputChange(text: String) {
        _uiState.update { it.copy(allergyInput = text) }
    }

    fun onIntoleranceInputChange(text: String) {
        _uiState.update { it.copy(intoleranceInput = text) }
    }

    /** Añade la chip de Alergias (RD25): vacías o duplicadas se rechazan sin cambios. */
    fun addAllergy() {
        addChip(ProfileSection.ALLERGY, _uiState.value.allergyInput) { added ->
            if (added) _uiState.update { it.copy(allergyInput = "") }
        }
    }

    /** Añade la chip de Intolerancias (RD25): vacías o duplicadas se rechazan sin cambios. */
    fun addIntolerance() {
        addChip(ProfileSection.INTOLERANCE, _uiState.value.intoleranceInput) { added ->
            if (added) _uiState.update { it.copy(intoleranceInput = "") }
        }
    }

    fun removeAllergy(value: String) = removeChip(ProfileSection.ALLERGY, value)

    fun removeIntolerance(value: String) = removeChip(ProfileSection.INTOLERANCE, value)

    /** Marca o desmarca «No tengo ninguna» (RD25/D3): marcarla limpia la sección. */
    fun setAllergyNone(none: Boolean) = setNone(ProfileSection.ALLERGY, none)

    fun setIntoleranceNone(none: Boolean) = setNone(ProfileSection.INTOLERANCE, none)

    /** Selección única de tipo de alimentación (D2): sin preselección inicial. */
    fun selectDietType(dietType: DietType) = mutate { UserProfileRules.setDietType(it, dietType) }

    private fun addChip(section: ProfileSection, value: String, onResult: (Boolean) -> Unit) {
        val updated = UserProfileRules.addChip(_uiState.value.profile, section, value)
        if (updated == null) {
            onResult(false)
            return
        }
        onResult(true)
        mutate { updated }
    }

    private fun removeChip(section: ProfileSection, value: String) =
        mutate { UserProfileRules.removeChip(it, section, value) }

    private fun setNone(section: ProfileSection, none: Boolean) =
        mutate { UserProfileRules.setNone(it, section, none) }

    private fun mutate(change: (UserProfile) -> UserProfile) {
        val updated = change(_uiState.value.profile)
        _uiState.update { it.copy(profile = updated) }
        viewModelScope.launch { userProfileRepository.save(updated) }
    }
}
