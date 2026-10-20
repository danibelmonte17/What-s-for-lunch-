package com.whatsforlunch.app.domain

import com.whatsforlunch.app.domain.misrecetas.filterRecipes
import com.whatsforlunch.app.domain.model.MealMoment
import com.whatsforlunch.app.domain.model.Recipe
import com.whatsforlunch.app.domain.model.RecipeIngredient
import com.whatsforlunch.app.domain.model.RecipeStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class RecipeSearchFilterTest {

    private val recipes = listOf(
        recipe("1", "Tortilla de patatas", MealMoment.DINNER, "Cebolla", RecipeStatus.PUBLISHED),
        recipe("2", "Crema de calabaza", MealMoment.LUNCH, "Calabaza", RecipeStatus.DRAFT),
        recipe("3", "Ensalada", MealMoment.DINNER, "Tomate", RecipeStatus.PUBLISHED),
    )

    @Test
    fun searchesTitlesAndIngredientNames() {
        assertEquals(listOf("1"), filterRecipes(recipes, "tortilla", null).map { it.id })
        assertEquals(listOf("1"), filterRecipes(recipes, "cebolla", null).map { it.id })
    }

    @Test
    fun ignoresCaseAccentsAndOuterWhitespace() {
        val accentedRecipe = recipe("4", "  Canción de limón  ", MealMoment.DESSERT, "Limón")

        assertEquals(listOf("4"), filterRecipes(listOf(accentedRecipe), "  CANCION ", null).map { it.id })
        assertEquals(listOf("4"), filterRecipes(listOf(accentedRecipe), "  LIMON  ", null).map { it.id })
    }

    @Test
    fun combinesTextAndMomentAndKeepsRepositoryOrderIncludingDrafts() {
        assertEquals(
            listOf("1", "3"),
            filterRecipes(recipes, "", MealMoment.DINNER).map { it.id },
        )
        assertEquals(listOf("2"), filterRecipes(recipes, "calabaza", MealMoment.LUNCH).map { it.id })
        assertEquals(listOf("2"), filterRecipes(recipes, "calabaza", null).map { it.id })
        assertEquals(emptyList<String>(), filterRecipes(recipes, "calabaza", MealMoment.DINNER).map { it.id })
    }

    @Test
    fun blankQueryReturnsAllRecipesInInputOrder() {
        assertEquals(listOf("1", "2", "3"), filterRecipes(recipes, "  ", null).map { it.id })
    }

    private fun recipe(
        id: String,
        title: String,
        moment: MealMoment,
        ingredient: String,
        status: RecipeStatus = RecipeStatus.PUBLISHED,
    ) = Recipe(
        id = id,
        title = title,
        mealMoment = moment,
        status = status,
        ingredients = listOf(RecipeIngredient(id = "ingredient-$id", name = ingredient)),
    )
}
