package com.whatsforlunch.app

import com.whatsforlunch.app.domain.model.PantryItem
import com.whatsforlunch.app.domain.model.RecipeIngredient
import com.whatsforlunch.app.domain.shoppinglist.ShoppingListRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// RD18: Faltantes por nombre normalizado y fusión idempotente por fuente.
class ShoppingListRulesTest {

    @Test
    fun missingRequiredKeepsOnlyAbsentRequiredIngredientsInRecipeOrder() {
        val ingredients = listOf(
            RecipeIngredient(id = "1", name = "Arroz", quantity = 200.0, unit = "g"),
            RecipeIngredient(id = "2", name = "Azafrán", quantity = 1.0, unit = "g"),
            RecipeIngredient(id = "3", name = "Mariscos", quantity = 300.0, unit = "g"),
            RecipeIngredient(id = "4", name = "Limón", optional = true),
        )
        val pantry = listOf(PantryItem("p1", "arroz"))

        assertEquals(
            listOf("Azafrán", "Mariscos"),
            ShoppingListRules.missingRequiredIngredients(ingredients, pantry).map { it.name },
        )
    }

    @Test
    fun missingRequiredNormalizesNamesAndAvoidsDuplicates() {
        val ingredients = listOf(
            RecipeIngredient(name = "Tomate"),
            RecipeIngredient(name = "TOMATE"),
            RecipeIngredient(name = "Harina"),
            RecipeIngredient(name = "Harina"),
        )
        val pantry = listOf(PantryItem("1", "  Tomaté  "))

        assertEquals(
            listOf("Harina"),
            ShoppingListRules.missingRequiredIngredients(ingredients, pantry).map { it.name },
        )
    }

    @Test
    fun itemIdIsStableForTheNormalizedName() {
        assertEquals(
            ShoppingListRules.itemIdFor("Azafrán"),
            ShoppingListRules.itemIdFor("  AZAFRAN "),
        )
        assertNotEquals(
            ShoppingListRules.itemIdFor("Azafrán"),
            ShoppingListRules.itemIdFor("Mariscos"),
        )
    }

    @Test
    fun contributionCreatesANewItemWithoutAisleAndNotCompleted() {
        val result = ShoppingListRules.addContributions(
            existing = emptyList(),
            sourceKey = "paella",
            sourceTitle = "Paella",
            ingredients = listOf(RecipeIngredient(name = "Azafrán", quantity = 1.0, unit = "g")),
        )

        val item = result.single()
        assertEquals(ShoppingListRules.itemIdFor("Azafrán"), item.id)
        assertEquals("Azafrán", item.name)
        assertEquals(1.0, item.quantity!!, 0.0001)
        assertEquals("g", item.unit)
        assertNull(item.aisle)
        assertFalse(item.completed)
        assertEquals(listOf("paella"), item.sources.map { it.sourceKey })
        assertEquals(listOf("Paella"), item.sources.map { it.sourceTitle })
    }

    @Test
    fun repeatingTheSameSourceIsANoOpAndDoesNotDoubleTheQuantity() {
        val first = ShoppingListRules.addContributions(
            existing = emptyList(),
            sourceKey = "paella",
            sourceTitle = "Paella",
            ingredients = listOf(RecipeIngredient(name = "Mariscos", quantity = 300.0, unit = "g")),
        )

        val second = ShoppingListRules.addContributions(
            existing = first,
            sourceKey = "paella",
            sourceTitle = "Paella",
            ingredients = listOf(RecipeIngredient(name = "Mariscos", quantity = 300.0, unit = "g")),
        )

        assertEquals(first, second)
        assertEquals(300.0, second.single().quantity!!, 0.0001)
        assertEquals(1, second.single().sources.size)
    }

    @Test
    fun differentSourcesWithTheSameUnitSumTheirQuantities() {
        val tortilla = ShoppingListRules.addContributions(
            existing = emptyList(),
            sourceKey = "tortilla",
            sourceTitle = "Tortilla",
            ingredients = listOf(RecipeIngredient(name = "Harina", quantity = 200.0, unit = "g")),
        )

        val galletas = ShoppingListRules.addContributions(
            existing = tortilla,
            sourceKey = "galletas",
            sourceTitle = "Galletas",
            ingredients = listOf(RecipeIngredient(name = "harina", quantity = 500.0, unit = "g")),
        )

        val item = galletas.single()
        assertEquals(700.0, item.quantity!!, 0.0001)
        assertEquals("g", item.unit)
        assertEquals(setOf("tortilla", "galletas"), item.sources.map { it.sourceKey }.toSet())
    }

    @Test
    fun differentUnitsKeepTheItemQuantityButStillRegisterTheSource() {
        val first = ShoppingListRules.addContributions(
            existing = emptyList(),
            sourceKey = "receta-a",
            sourceTitle = "Receta A",
            ingredients = listOf(RecipeIngredient(name = "Harina", quantity = 200.0, unit = "g")),
        )

        val second = ShoppingListRules.addContributions(
            existing = first,
            sourceKey = "receta-b",
            sourceTitle = "Receta B",
            ingredients = listOf(RecipeIngredient(name = "Harina", quantity = 2.0, unit = "uds")),
        )

        val item = second.single()
        assertEquals(200.0, item.quantity!!, 0.0001)
        assertEquals("g", item.unit)
        assertEquals(2, item.sources.size)
    }

    @Test
    fun zeroQuantityCreatesAnItemWithoutVisibleQuantity() {
        val result = ShoppingListRules.addContributions(
            existing = emptyList(),
            sourceKey = "receta",
            sourceTitle = "Receta",
            ingredients = listOf(RecipeIngredient(name = "Sal", quantity = 0.0, unit = "g")),
        )

        assertNull(result.single().quantity)
    }

    @Test
    fun duplicateNamesInsideOneContributionUseTheFirstOccurrence() {
        val result = ShoppingListRules.addContributions(
            existing = emptyList(),
            sourceKey = "receta",
            sourceTitle = "Receta",
            ingredients = listOf(
                RecipeIngredient(name = "Harina", quantity = 200.0, unit = "g"),
                RecipeIngredient(name = "harina", quantity = 500.0, unit = "g"),
            ),
        )

        assertEquals(1, result.size)
        assertEquals(200.0, result.single().quantity!!, 0.0001)
    }

    @Test
    fun itemsAreMatchedByNormalizedNameAcrossSources() {
        val result = ShoppingListRules.addContributions(
            existing = emptyList(),
            sourceKey = "fuente-1",
            sourceTitle = "Fuente 1",
            ingredients = listOf(RecipeIngredient(name = "Tomate", quantity = 1.0, unit = "ud")),
        )
        val updated = ShoppingListRules.addContributions(
            existing = result,
            sourceKey = "fuente-2",
            sourceTitle = "Fuente 2",
            ingredients = listOf(RecipeIngredient(name = "  TOMATÉ ", quantity = 2.0, unit = "ud")),
        )

        assertEquals(1, updated.size)
        assertEquals(3.0, updated.single().quantity!!, 0.0001)
        assertTrue(updated.single().sources.any { it.sourceKey == "fuente-2" })
    }
}
