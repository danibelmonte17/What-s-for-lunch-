package com.whatsforlunch.app.domain.myrecipes

import com.whatsforlunch.app.domain.model.Recipe

// Estado de la pantalla Mi recetario derivado de las Recetas existentes.
// Regla de negocio sin dependencias de UI: una colección vacía produce el
// estado vacío (docs/domain-model.md, caso «Colección de Recetas vacía»).
enum class MyRecipesState {
    Empty,
    WithRecipes,
    NoResults,
}

fun myRecipesStateOf(recipes: List<Recipe>, visibleRecipes: List<Recipe>): MyRecipesState = when {
    recipes.isEmpty() -> MyRecipesState.Empty
    visibleRecipes.isEmpty() -> MyRecipesState.NoResults
    else -> MyRecipesState.WithRecipes
}

fun myRecipesStateOf(recipes: List<Recipe>): MyRecipesState =
    myRecipesStateOf(recipes, recipes)
