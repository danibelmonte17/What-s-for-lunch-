package com.whatsforlunch.app.data.repository

import com.whatsforlunch.app.data.local.IngredientEntity
import com.whatsforlunch.app.data.local.RecipeDao
import com.whatsforlunch.app.data.local.RecipeEntity
import com.whatsforlunch.app.data.local.StepEntity
import com.whatsforlunch.app.domain.model.Difficulty
import com.whatsforlunch.app.domain.model.MealMoment
import com.whatsforlunch.app.domain.model.Recipe
import com.whatsforlunch.app.domain.model.RecipeIngredient
import com.whatsforlunch.app.domain.model.RecipeOrigin
import com.whatsforlunch.app.domain.model.RecipeStatus
import com.whatsforlunch.app.domain.model.RecipeStep
import com.whatsforlunch.app.domain.repository.RecipeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Implementación persistente con Room (Parte B, T2). Sustituye a InMemoryRecipeRepository
// en Koin; mapea entre las entidades y el modelo de dominio.
class RoomRecipeRepository(private val dao: RecipeDao) : RecipeRepository {

    override fun observeRecipes(): Flow<List<Recipe>> =
        dao.observeRecipesWithDetails().map { recipes -> recipes.map { it.toDomain() } }

    override suspend fun saveRecipe(recipe: Recipe) {
        dao.upsertRecipe(
            recipe = recipe.toEntity(),
            ingredients = recipe.ingredients.mapIndexed { index, ingredient -> ingredient.toEntity(recipe.id, index) },
            steps = recipe.steps.mapIndexed { index, step -> step.toEntity(recipe.id, index) },
        )
    }
}

private fun com.whatsforlunch.app.data.local.RecipeWithDetails.toDomain(): Recipe = Recipe(
    id = recipe.id,
    title = recipe.title,
    story = recipe.story,
    mealMoment = enumOrDefault(recipe.mealMoment, MealMoment.LUNCH),
    prepMinutes = recipe.prepMinutes,
    cookMinutes = recipe.cookMinutes,
    difficulty = enumOrDefault(recipe.difficulty, Difficulty.MEDIUM),
    baseServings = recipe.baseServings,
    caloriesPerServing = recipe.caloriesPerServing,
    photoPath = recipe.photoPath,
    favorite = recipe.favorite,
    status = enumOrDefault(recipe.status, RecipeStatus.DRAFT),
    origin = enumOrDefault(recipe.origin, RecipeOrigin.USER),
    ingredients = ingredients.sortedBy { it.position }.map {
        RecipeIngredient(
            id = it.id.toString(),
            name = it.name,
            quantity = it.quantity,
            unit = it.unit,
            optional = it.optional,
        )
    },
    steps = steps.sortedBy { it.position }.map {
        RecipeStep(id = it.id.toString(), text = it.text, timerMinutes = it.timerMinutes)
    },
)

private fun Recipe.toEntity(): RecipeEntity = RecipeEntity(
    id = id,
    title = title,
    story = story,
    mealMoment = mealMoment.name,
    prepMinutes = prepMinutes,
    cookMinutes = cookMinutes,
    difficulty = difficulty.name,
    baseServings = baseServings,
    caloriesPerServing = caloriesPerServing,
    photoPath = photoPath,
    favorite = favorite,
    status = status.name,
    origin = origin.name,
)

private fun RecipeIngredient.toEntity(recipeId: String, position: Int): IngredientEntity = IngredientEntity(
    recipeId = recipeId,
    name = name,
    quantity = quantity,
    unit = unit,
    optional = optional,
    position = position,
)

private fun RecipeStep.toEntity(recipeId: String, position: Int): StepEntity = StepEntity(
    recipeId = recipeId,
    text = text,
    timerMinutes = timerMinutes,
    position = position,
)

private inline fun <reified T : Enum<T>> enumOrDefault(value: String, default: T): T =
    runCatching { enumValueOf<T>(value) }.getOrDefault(default)
