package com.whatsforlunch.app.domain

import com.whatsforlunch.app.domain.myrecipes.filterRecipes
import com.whatsforlunch.app.domain.model.MealMoment
import com.whatsforlunch.app.domain.model.Recipe
import com.whatsforlunch.app.domain.model.RecipeIngredient
import org.junit.Assert.assertEquals
import org.junit.Test

class RecipeSearchFilterTest {

    private val recipes = listOf(
        recipe("1", "Tortilla de patatas", MealMoment.DINNER, "Cebolla"),
        recipe("2", "Crema de calabaza", MealMoment.LUNCH, "Calabaza", favorite = true),
        recipe("3", "Ensalada", MealMoment.DINNER, "Tomate"),
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
    fun combinesTextAndMomentAndKeepsRepositoryOrder() {
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

    // RF-EBF7: el chip «Favoritas» solo deja las favoritas…
    @Test
    fun favoritesOnlyKeepsOnlyFavoriteRecipes() {
        assertEquals(listOf("2"), filterRecipes(recipes, "", null, favoritesOnly = true).map { it.id })
    }

    // …y se combina por AND con la búsqueda y el momento (D3).
    @Test
    fun favoritesCombineWithTextAndMoment() {
        assertEquals(
            listOf("2"),
            filterRecipes(recipes, "calabaza", MealMoment.LUNCH, favoritesOnly = true).map { it.id },
        )
        assertEquals(
            emptyList<String>(),
            filterRecipes(recipes, "calabaza", MealMoment.DINNER, favoritesOnly = true).map { it.id },
        )
        assertEquals(
            emptyList<String>(),
            filterRecipes(recipes, "tortilla", null, favoritesOnly = true).map { it.id },
        )
    }

    @Test
    fun favoritesOnlyWithoutFavoritesShowsNoResults() {
        val noFavorites = recipes.map { it.copy(favorite = false) }
        assertEquals(emptyList<String>(), filterRecipes(noFavorites, "", null, favoritesOnly = true).map { it.id })
    }

    private fun recipe(
        id: String,
        title: String,
        moment: MealMoment,
        ingredient: String,
        favorite: Boolean = false,
    ) = Recipe(
        id = id,
        title = title,
        mealMoment = moment,
        favorite = favorite,
        ingredients = listOf(RecipeIngredient(id = "ingredient-$id", name = ingredient)),
    )
}
