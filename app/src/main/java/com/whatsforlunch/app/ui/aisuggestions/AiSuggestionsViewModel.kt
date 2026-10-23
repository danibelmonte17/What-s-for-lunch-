package com.whatsforlunch.app.ui.aisuggestions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whatsforlunch.app.data.ai.RecipeSuggester
import com.whatsforlunch.app.data.ai.RecipeSuggestionsResult
import com.whatsforlunch.app.data.scanner.ScanDraftHolder
import com.whatsforlunch.app.domain.model.PantryItem
import com.whatsforlunch.app.domain.model.ScanDraftItem
import com.whatsforlunch.app.domain.repository.PantryRepository
import com.whatsforlunch.app.domain.suggestion.SuggestionRules
import com.whatsforlunch.app.domain.suggestion.SuggestionWithMatch
import com.whatsforlunch.app.domain.suggestion.SuggestableFood
import com.whatsforlunch.app.domain.text.normalizeName
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

// Avisos de la pantalla: key ausente (S05/RF-SRC7) y fallo mínimo de la IA
// (RF-SRC4/RD05). Se muestran fuera del contenido principal, como en el Escáner.
enum class AiSuggestionsNotice {
    AI_KEY_MISSING,
    FAILED,
}

sealed interface AiSuggestionsPhase {
    data class Selection(
        val foods: List<SuggestableFood> = emptyList(),
        val selected: Set<String> = emptySet(),
        val canRequest: Boolean = false,
    ) : AiSuggestionsPhase

    data object Loading : AiSuggestionsPhase

    data class Results(val items: List<SuggestionWithMatch>) : AiSuggestionsPhase

    data object Empty : AiSuggestionsPhase
}

data class AiSuggestionsUiState(
    val phase: AiSuggestionsPhase = AiSuggestionsPhase.Selection(),
    val notice: AiSuggestionsNotice? = null,
)

/**
 * Estado de la pantalla de Sugerencias (D3-D5, D10).
 *
 * La selección inicial une los chips del Escaneo sin guardar y los Alimentos de la
 * Despensa, un ítem por nombre normalizado y todos preseleccionados. Cada
 * confirmación hace una única llamada al puerto de IA; el fallo vuelve a la
 * selección con un aviso para reintentar sin relanzar la pantalla.
 */
class AiSuggestionsViewModel(
    pantryRepository: PantryRepository,
    private val scanDraftHolder: ScanDraftHolder,
    private val recipeSuggester: RecipeSuggester,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiSuggestionsUiState())
    val uiState: StateFlow<AiSuggestionsUiState> = _uiState.asStateFlow()

    private var foods: List<SuggestableFood> = emptyList()
    private var lastSelection: AiSuggestionsPhase.Selection? = null
    private var lastFoods: List<SuggestableFood> = emptyList()
    private var lastLanguage: String = "es"
    private var isRequesting: Boolean = false

    init {
        viewModelScope.launch {
            combine(
                scanDraftHolder.items,
                pantryRepository.observePantryItems(),
            ) { chips, pantryItems -> mergeFoods(chips, pantryItems) }
                .collect(::onFoodsChanged)
        }
    }

    fun toggleFood(name: String) {
        val phase = _uiState.value.phase as? AiSuggestionsPhase.Selection ?: return
        val selected = if (name in phase.selected) phase.selected - name else phase.selected + name
        _uiState.value = _uiState.value.copy(phase = selectionPhase(phase.foods, selected))
    }

    fun requestSuggestions(language: String) {
        val phase = _uiState.value.phase as? AiSuggestionsPhase.Selection ?: return
        if (!phase.canRequest || isRequesting) return
        val selectedFoods = phase.foods.filter { it.name in phase.selected }
        if (selectedFoods.isEmpty()) return
        lastSelection = phase
        lastFoods = selectedFoods
        lastLanguage = language
        runRequest(selectedFoods, language, phase)
    }

    fun retry() {
        if (isRequesting) return
        val selectedFoods = lastFoods.ifEmpty {
            val phase = _uiState.value.phase as? AiSuggestionsPhase.Selection ?: return
            phase.foods.filter { it.name in phase.selected }
        }
        if (selectedFoods.isEmpty()) return
        val restore = lastSelection ?: AiSuggestionsPhase.Selection(
            foods = foods,
            selected = selectedFoods.mapTo(mutableSetOf()) { it.name },
            canRequest = true,
        )
        runRequest(selectedFoods, lastLanguage, restore)
    }

    fun clearNotice() {
        _uiState.value = _uiState.value.copy(notice = null)
    }

    private fun runRequest(
        selectedFoods: List<SuggestableFood>,
        language: String,
        restore: AiSuggestionsPhase.Selection,
    ) {
        isRequesting = true
        _uiState.value = _uiState.value.copy(phase = AiSuggestionsPhase.Loading, notice = null)
        viewModelScope.launch {
            val result = try {
                recipeSuggester.suggest(selectedFoods, language)
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                RecipeSuggestionsResult.Failed
            }
            isRequesting = false
            when (result) {
                is RecipeSuggestionsResult.Success -> {
                    val items = SuggestionRules.suggestionMatches(result.suggestions, selectedFoods)
                    _uiState.value = _uiState.value.copy(
                        phase = if (items.isEmpty()) AiSuggestionsPhase.Empty
                        else AiSuggestionsPhase.Results(items),
                        notice = null,
                    )
                }

                RecipeSuggestionsResult.MissingApiKey -> {
                    _uiState.value = _uiState.value.copy(
                        phase = restoredSelection(restore),
                        notice = AiSuggestionsNotice.AI_KEY_MISSING,
                    )
                }

                RecipeSuggestionsResult.Failed -> {
                    _uiState.value = _uiState.value.copy(
                        phase = restoredSelection(restore),
                        notice = AiSuggestionsNotice.FAILED,
                    )
                }
            }
        }
    }

    private fun restoredSelection(restore: AiSuggestionsPhase.Selection): AiSuggestionsPhase.Selection {
        val selected = restore.selected.intersect(restore.foods.mapTo(mutableSetOf()) { it.name })
        return selectionPhase(restore.foods, selected)
    }

    private fun onFoodsChanged(merged: List<SuggestableFood>) {
        foods = merged
        val phase = _uiState.value.phase as? AiSuggestionsPhase.Selection ?: return
        val knownNames = phase.foods.mapTo(mutableSetOf()) { it.name }
        // Todos los Alimentos entran preseleccionados (D3); se conservan las
        // deselecciones que el usuario ya hubiera hecho.
        val selected = phase.selected + merged.filter { it.name !in knownNames }.map { it.name }
        _uiState.value = _uiState.value.copy(phase = selectionPhase(merged, selected))
    }

    private fun selectionPhase(
        availableFoods: List<SuggestableFood>,
        selected: Set<String>,
    ): AiSuggestionsPhase.Selection {
        val names = availableFoods.mapTo(mutableSetOf()) { it.name }
        val kept = selected.filterTo(mutableSetOf()) { it in names }
        return AiSuggestionsPhase.Selection(
            foods = availableFoods,
            selected = kept,
            canRequest = kept.isNotEmpty(),
        )
    }

    private fun mergeFoods(
        chips: List<ScanDraftItem>,
        pantryItems: List<PantryItem>,
    ): List<SuggestableFood> {
        val byName = linkedMapOf<String, SuggestableFood>()
        chips.forEach { chip ->
            val name = chip.name.trim()
            if (name.isEmpty()) return@forEach
            byName.putIfAbsent(
                normalizeName(name),
                SuggestableFood(name = name, quantity = chip.quantity.takeIf { it > 0 }),
            )
        }
        pantryItems.forEach { item ->
            val name = item.name.trim()
            if (name.isEmpty()) return@forEach
            byName.putIfAbsent(normalizeName(name), SuggestableFood(name = name, quantity = item.quantity))
        }
        return byName.values.toList()
    }
}
