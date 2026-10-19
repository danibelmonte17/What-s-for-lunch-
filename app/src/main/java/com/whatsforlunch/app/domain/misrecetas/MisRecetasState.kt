package com.whatsforlunch.app.domain.misrecetas

import com.whatsforlunch.app.domain.model.Recipe

// Estado de la pantalla Mis Recetas derivado de las Recetas existentes.
// Regla de negocio sin dependencias de UI: una colección vacía produce el
// estado vacío (docs/domain-model.md, caso «Colección de Recetas vacía»).
enum class MisRecetasState {
    Empty,
    WithRecipes,
    NoResults,
}

fun misRecetasStateOf(recipes: List<Recipe>, visibleRecipes: List<Recipe>): MisRecetasState = when {
    recipes.isEmpty() -> MisRecetasState.Empty
    visibleRecipes.isEmpty() -> MisRecetasState.NoResults
    else -> MisRecetasState.WithRecipes
}

fun misRecetasStateOf(recipes: List<Recipe>): MisRecetasState =
    misRecetasStateOf(recipes, recipes)
