package com.whatsforlunch.app

import com.whatsforlunch.app.domain.model.Aisle
import com.whatsforlunch.app.domain.model.PantryItem
import com.whatsforlunch.app.domain.model.RecipeIngredient
import com.whatsforlunch.app.domain.model.ShoppingItemSource
import com.whatsforlunch.app.domain.model.ShoppingListItem
import com.whatsforlunch.app.domain.shoppinglist.ShoppingGroupMode
import com.whatsforlunch.app.domain.shoppinglist.ShoppingListRules
import com.whatsforlunch.app.domain.shoppinglist.ShoppingProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// RD18: Faltantes por nombre normalizado y fusión idempotente por fuente.
class ShoppingListRulesTest {

    @Test
    fun missingRequiredKeepsOnlyAbsentIngredientsInRecipeOrder() {
        // D5: todos los Ingredientes cuentan (sin distinción obligatorio/opcional).
        val ingredients = listOf(
            RecipeIngredient(id = "1", name = "Arroz", quantity = 200.0, unit = "g"),
            RecipeIngredient(id = "2", name = "Azafrán", quantity = 1.0, unit = "g"),
            RecipeIngredient(id = "3", name = "Mariscos", quantity = 300.0, unit = "g"),
            RecipeIngredient(id = "4", name = "Limón"),
        )
        val pantry = listOf(PantryItem("p1", "arroz"))

        assertEquals(
            listOf("Azafrán", "Mariscos", "Limón"),
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

    // --- Agrupación y progreso (RD20) ----------------------------------------

    @Test
    fun groupItemsByAislePutsTheAislesInEnumOrderAndUncategorizedLast() {
        val groups = ShoppingListRules.groupItems(
            items = listOf(
                item("Toallitas"),
                item("Manzana", aisle = Aisle.PRODUCE),
                item("Pollo", aisle = Aisle.MEAT_FISH),
            ),
            mode = ShoppingGroupMode.BY_AISLE,
        )

        assertEquals(listOf("PRODUCE", "MEAT_FISH", ""), groups.map { it.key })
        assertEquals(listOf("Manzana"), groups[0].items.map { it.name })
        assertEquals(listOf("Pollo"), groups[1].items.map { it.name })
        assertEquals(listOf("Toallitas"), groups[2].items.map { it.name })
    }

    @Test
    fun groupItemsByAisleSkipsEmptyAisles() {
        val groups = ShoppingListRules.groupItems(
            items = listOf(item("Leche", aisle = Aisle.DAIRY), item("Toallitas")),
            mode = ShoppingGroupMode.BY_AISLE,
        )

        assertEquals(listOf("DAIRY", ""), groups.map { it.key })
    }

    @Test
    fun groupItemsByRecipeReplicatesMultiSourceItemsAndPutsNoSourceLast() {
        val harina = item(
            "Harina",
            sources = listOf(
                ShoppingItemSource("gazpacho", "Gazpacho"),
                ShoppingItemSource("paella", "Paella"),
            ),
        )
        val groups = ShoppingListRules.groupItems(
            items = listOf(
                harina,
                item("Azafrán", sources = listOf(ShoppingItemSource("paella", "Paella"))),
                item("Toallitas"),
            ),
            mode = ShoppingGroupMode.BY_RECIPE,
        )

        assertEquals(listOf("gazpacho", "paella", ""), groups.map { it.key })
        assertEquals(listOf("Gazpacho", "Paella", ""), groups.map { it.title })
        assertEquals(listOf("Harina"), groups[0].items.map { it.name })
        // Dentro del grupo el orden es el de nombre normalizado: Azafrán antes
        // que Harina, aunque la aportación llegara después.
        assertEquals(listOf("Azafrán", "Harina"), groups[1].items.map { it.name })
        assertEquals(listOf("Toallitas"), groups[2].items.map { it.name })
        // El multi-fuente se replica, pero sigue siendo una sola fila lógica.
        assertEquals(harina, groups[0].items.single())
        assertEquals(harina, groups[1].items.single { it.name == "Harina" })
    }

    @Test
    fun groupItemsOrdersRecipeGroupsByNormalizedTitleWithSourceKeyAsTieBreak() {
        val groups = ShoppingListRules.groupItems(
            items = listOf(
                item("Harina", sources = listOf(ShoppingItemSource("b", "  ARROZ "))),
                item("Sal", sources = listOf(ShoppingItemSource("a", "Arroz"))),
            ),
            mode = ShoppingGroupMode.BY_RECIPE,
        )

        assertEquals(listOf("a", "b"), groups.map { it.key })
    }

    @Test
    fun groupItemsKeepsCompletedRowsInTheirSortPosition() {
        val groups = ShoppingListRules.groupItems(
            items = listOf(
                item("Zanahoria", completed = true),
                item("Manzana", completed = true),
                item("Azafrán"),
            ),
            mode = ShoppingGroupMode.BY_AISLE,
        )

        assertEquals(listOf("Azafrán", "Manzana", "Zanahoria"), groups.single().items.map { it.name })
    }

    @Test
    fun progressCountsUniqueItemsOnceAndRoundsToTheNearestInteger() {
        val fourItems = listOf(
            item("Azafrán"),
            item("Harina", completed = true),
            item("Leche", completed = true),
            item("Pan"),
        )
        assertEquals(
            ShoppingProgress(completed = 2, total = 4, percent = 50),
            ShoppingListRules.progress(fourItems),
        )

        val fourteenItems = (1..14).map { item("Ingrediente $it", completed = it <= 6) }
        assertEquals(
            ShoppingProgress(completed = 6, total = 14, percent = 43),
            ShoppingListRules.progress(fourteenItems),
        )

        val threeItems = (1..3).map { item("Ingrediente $it", completed = it <= 2) }
        assertEquals(
            ShoppingProgress(completed = 2, total = 3, percent = 67),
            ShoppingListRules.progress(threeItems),
        )

        assertEquals(ShoppingProgress(0, 0, 0), ShoppingListRules.progress(emptyList()))
        assertEquals(
            ShoppingProgress(completed = 3, total = 3, percent = 100),
            ShoppingListRules.progress(threeItems.map { it.copy(completed = true) }),
        )
    }

    @Test
    fun contributionsKeepTheCompletedStateOfMergedItems() {
        val completed = ShoppingListRules.addContributions(
            existing = emptyList(),
            sourceKey = "paella",
            sourceTitle = "Paella",
            ingredients = listOf(RecipeIngredient(name = "Harina", quantity = 200.0, unit = "g")),
        ).map { it.copy(completed = true) }

        val merged = ShoppingListRules.addContributions(
            existing = completed,
            sourceKey = "gazpacho",
            sourceTitle = "Gazpacho",
            ingredients = listOf(RecipeIngredient(name = "Harina", quantity = 500.0, unit = "g")),
        )

        assertTrue(merged.single().completed)
        assertEquals(700.0, merged.single().quantity!!, 0.0001)
    }

    private fun item(
        name: String,
        aisle: Aisle? = null,
        completed: Boolean = false,
        sources: List<ShoppingItemSource> = emptyList(),
    ) = ShoppingListItem(
        id = ShoppingListRules.itemIdFor(name),
        name = name,
        aisle = aisle,
        completed = completed,
        sources = sources,
    )
}
