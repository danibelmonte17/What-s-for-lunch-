package com.whatsforlunch.app

import com.whatsforlunch.app.data.repository.InMemoryShoppingListRepository
import com.whatsforlunch.app.domain.model.Aisle
import com.whatsforlunch.app.domain.model.RecipeIngredient
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// RD18: persistencia en memoria con las mismas reglas que Room, incluido «Deshacer».
class InMemoryShoppingListRepositoryTest {

    @Test
    fun addMissingIsIdempotentForTheSameSource() = runTest {
        val repository = InMemoryShoppingListRepository()
        val ingredients = listOf(RecipeIngredient(name = "Mariscos", quantity = 300.0, unit = "g"))

        repository.addMissing("paella", "Paella", ingredients)
        repository.addMissing("paella", "Paella", ingredients)

        val items = repository.observeItems().first()
        assertEquals(1, items.size)
        assertEquals(300.0, items.single().quantity!!, 0.0001)
        assertEquals(1, items.single().sources.size)
    }

    @Test
    fun differentSourcesAccumulateAndKeepEveryOrigin() = runTest {
        val repository = InMemoryShoppingListRepository()

        repository.addMissing(
            "tortilla",
            "Tortilla",
            listOf(RecipeIngredient(name = "Harina", quantity = 200.0, unit = "g")),
        )
        repository.addMissing(
            "galletas",
            "Galletas",
            listOf(RecipeIngredient(name = "Harina", quantity = 500.0, unit = "g")),
        )

        val item = repository.observeItems().first().single()
        assertEquals(700.0, item.quantity!!, 0.0001)
        assertEquals(setOf("Tortilla", "Galletas"), item.sources.map { it.sourceTitle }.toSet())
    }

    @Test
    fun deletedItemCanBeRestoredWithItsQuantityUnitAndOrigins() = runTest {
        val repository = InMemoryShoppingListRepository()
        repository.addMissing(
            "paella",
            "Paella",
            listOf(RecipeIngredient(name = "Azafrán", quantity = 1.0, unit = "g")),
        )
        val item = repository.observeItems().first().single()

        repository.deleteItem(item.id)
        assertTrue(repository.observeItems().first().isEmpty())

        repository.restoreItem(item)
        val restored = repository.observeItems().first().single()
        assertEquals(item, restored)
    }

    @Test
    fun saveManualItemCreatesItemsAndMergesDuplicateNames() = runTest {
        val repository = InMemoryShoppingListRepository()

        repository.saveManualItem(id = null, name = "Pan", quantity = 2.0, unit = "uds", aisle = Aisle.PANTRY_OILS)
        repository.saveManualItem(id = null, name = "pan", quantity = 3.0, unit = "uds", aisle = Aisle.DAIRY)

        val item = repository.observeItems().first().single()
        assertEquals("Pan", item.name)
        assertEquals(3.0, item.quantity!!, 0.0001)
        assertEquals(Aisle.DAIRY, item.aisle)
        assertTrue(item.sources.isEmpty())
    }

    @Test
    fun manualEditKeepsTheOriginsOfContributedItems() = runTest {
        val repository = InMemoryShoppingListRepository()
        repository.addMissing(
            "tortilla",
            "Tortilla",
            listOf(RecipeIngredient(name = "Harina", quantity = 200.0, unit = "g")),
        )

        repository.saveManualItem(
            id = null,
            name = "Harina",
            quantity = 500.0,
            unit = "g",
            aisle = Aisle.PANTRY_OILS,
        )

        val item = repository.observeItems().first().single()
        assertEquals(500.0, item.quantity!!, 0.0001)
        assertEquals(listOf("Tortilla"), item.sources.map { it.sourceTitle })
    }
}
