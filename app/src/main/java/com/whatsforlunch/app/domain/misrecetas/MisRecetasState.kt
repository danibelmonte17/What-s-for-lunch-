package com.whatsforlunch.app.domain.misrecetas

import com.whatsforlunch.app.domain.model.Recipe

// Estado de la pantalla Mis Recetas derivado de las Recetas existentes.
// Regla de negocio sin dependencias de UI: una colección vacía produce el
// estado vacío (docs/domain-model.md, caso «Colección de Recetas vacía»).
enum class MisRecetasState {
    Empty,
    WithRecipes,
}

fun misRecetasStateOf(recipes: List<Recipe>): MisRecetasState =
    if (recipes.isEmpty()) MisRecetasState.Empty else MisRecetasState.WithRecipes
