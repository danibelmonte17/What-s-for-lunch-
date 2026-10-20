package com.whatsforlunch.app.data.repository

import com.whatsforlunch.app.domain.model.PantryItem
import com.whatsforlunch.app.domain.repository.PantrySaveResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class InMemoryPantryRepositoryTest {
    @Test
    fun savesAndObservesItems() = runBlocking {
        val repository = InMemoryPantryRepository()
        val item = PantryItem("1", "Tomate fresco", 2.5)

        assertEquals(PantrySaveResult.Saved, repository.savePantryItem(item))
        assertEquals(listOf(item), repository.observePantryItems().first())
    }

    @Test
    fun updatesExistingItemWithoutAddingAnotherRow() = runBlocking {
        val repository = InMemoryPantryRepository()
        repository.savePantryItem(PantryItem("1", "Tomate fresco"))

        val updated = PantryItem("1", "Tomate maduro", 3.0)
        assertEquals(PantrySaveResult.Saved, repository.savePantryItem(updated))
        assertEquals(listOf(updated), repository.observePantryItems().first())
    }

    @Test
    fun rejectsNormalizedDuplicateOnCreateAndRename() = runBlocking {
        val repository = InMemoryPantryRepository()
        repository.savePantryItem(PantryItem("1", "Tomate fresco"))

        assertEquals(
            PantrySaveResult.DuplicateName,
            repository.savePantryItem(PantryItem("2", "  TOMÁTE  fresco ")),
        )
        assertEquals(
            PantrySaveResult.DuplicateName,
            repository.savePantryItem(PantryItem("2", "TOMATE fresco")),
        )
        assertEquals(listOf(PantryItem("1", "Tomate fresco")), repository.observePantryItems().first())
    }

    @Test
    fun deleteRemovesItemAndMissingIdIsNoOp() = runBlocking {
        val repository = InMemoryPantryRepository()
        repository.savePantryItem(PantryItem("1", "Tomate fresco"))

        repository.deletePantryItem("missing")
        assertEquals(1, repository.observePantryItems().first().size)
        repository.deletePantryItem("1")
        assertEquals(emptyList<PantryItem>(), repository.observePantryItems().first())
    }
}
