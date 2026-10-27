package com.whatsforlunch.app.data.repository

import com.whatsforlunch.app.data.local.IngredientEntity
import com.whatsforlunch.app.data.local.RecipeDao
import com.whatsforlunch.app.data.local.RecipeEntity
import com.whatsforlunch.app.data.local.StepEntity
import com.whatsforlunch.app.data.photo.PhotoStore
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
class RoomRecipeRepository(
    private val dao: RecipeDao,
    private val photoStore: PhotoStore,
) : RecipeRepository {

    override fun observeRecipes(): Flow<List<Recipe>> =
        dao.observeRecipesWithDetails().map { recipes -> recipes.map { it.toDomain() } }

    override suspend fun getRecipe(id: String): Recipe? = dao.getRecipeWithDetails(id)?.toDomain()

    override suspend fun saveRecipe(recipe: Recipe) {
        // D5: los ids de Ingrediente en blanco (recetas de IA) se generan con la
        // convención `{recipeId}-ingredient-{position}` al persistir.
        val stableIds = recipe.ingredients.mapIndexed { index, ingredient ->
            stableIngredientId(recipe.id, ingredient.id, index)
        }
        dao.upsertRecipe(
            recipe = recipe.toEntity(),
            ingredients = recipe.ingredients.mapIndexed { index, ingredient -> ingredient.toEntity(recipe.id, index) },
            steps = recipe.steps.mapIndexed { index, step -> step.toEntity(recipe.id, index, stableIds) },
        )
    }

    override suspend fun deleteRecipe(id: String) {
        val recipe = dao.getRecipeWithDetails(id)?.toDomain() ?: return
        val deletedRows = dao.deleteRecipe(id)
        if (deletedRows > 0 && recipe.photoPath != null) {
            // El borrado del fichero es best-effort: un problema con un fichero ausente
            // o inaccesible no debe revertir ni ocultar el borrado definitivo de Room.
            runCatching { photoStore.delete(recipe.photoPath) }
        }
    }
}

private fun com.whatsforlunch.app.data.local.RecipeWithDetails.toDomain(): Recipe {
    val domainIngredients = ingredients.sortedBy { it.position }.map {
        RecipeIngredient(
            id = it.domainId,
            name = it.name,
            quantity = it.quantity,
            unit = it.unit,
            optional = it.optional,
        )
    }
    // D5: al cargar se descartan, sin error, las referencias huérfanas de los
    // vínculos (ids que no existen entre los Ingredientes de la Receta).
    val validIds = domainIngredients.map { it.id }.toSet()
    return Recipe(
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
        ingredients = domainIngredients,
        steps = steps.sortedBy { it.position }.map {
            RecipeStep(
                id = it.id.toString(),
                text = it.text,
                timerMinutes = it.timerMinutes,
                ingredientIds = it.ingredientIds.split(',').filter { id -> id.isNotBlank() && id in validIds },
            )
        },
    )
}

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

// Id estable de un Ingrediente (RD21): el del dominio o, si llega en blanco
// (recetas de IA), el generado con la convención `{recipeId}-ingredient-{position}`
// (D5). Lo referencian los vínculos de los Pasos, por lo que no cambia entre guardados.
private fun stableIngredientId(recipeId: String, id: String, position: Int): String =
    id.ifBlank { "$recipeId-ingredient-$position" }

private fun RecipeIngredient.toEntity(recipeId: String, position: Int): IngredientEntity = IngredientEntity(
    recipeId = recipeId,
    name = name,
    quantity = quantity,
    unit = unit,
    optional = optional,
    position = position,
    domainId = stableIngredientId(recipeId, id, position),
)

private fun RecipeStep.toEntity(
    recipeId: String,
    position: Int,
    ingredientIds: List<String>,
): StepEntity {
    // D7: los vínculos se persisten en el orden de ingredientes de la Receta y sin
    // referencias huérfanas (D5). Los ids no contienen comas, así que el CSV es fiable.
    val selected = this.ingredientIds.toSet()
    return StepEntity(
        recipeId = recipeId,
        text = text,
        timerMinutes = timerMinutes,
        position = position,
        ingredientIds = ingredientIds.filter { it in selected }.joinToString(","),
    )
}

private inline fun <reified T : Enum<T>> enumOrDefault(value: String, default: T): T =
    runCatching { enumValueOf<T>(value) }.getOrDefault(default)
