package com.whatsforlunch.app.data.ai

import com.whatsforlunch.app.domain.suggestion.RecipeSuggestion
import com.whatsforlunch.app.domain.suggestion.SuggestableFood

// Resultado de pedir Sugerencias a la IA: separa el éxito (incluida una lista vacía,
// que la UI muestra como estado vacío) de la key ausente (S05) y de cualquier fallo
// de red, proveedor o parseo (RF-SRC4, RD05 mínimo).
sealed interface RecipeSuggestionsResult {
    data class Success(val suggestions: List<RecipeSuggestion>) : RecipeSuggestionsResult
    data object MissingApiKey : RecipeSuggestionsResult
    data object Failed : RecipeSuggestionsResult
}

// Puerto de sugerencias de recetas. Recibe solo los Alimentos seleccionados por el
// usuario (D3) y el idioma de la app (D9); nunca se llama dos veces por confirmación
// (D10).
interface RecipeSuggester {
    suspend fun suggest(foods: List<SuggestableFood>, language: String): RecipeSuggestionsResult
}
