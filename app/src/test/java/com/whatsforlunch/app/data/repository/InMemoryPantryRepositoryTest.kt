package com.whatsforlunch.app.data.repository

import com.whatsforlunch.app.domain.model.PantryItem
import com.whatsforlunch.app.domain.repository.PantrySaveResult
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class InMemoryPantryRepositoryTest {
    @Test
    fun savesAndObservesItems() = runBlocking {
        val repository = InMemoryPantryRepository()
        val item = PantryItem("1", "Tomate fresco", 2)

        assertEquals(PantrySaveResult.Saved, repository.savePantryItem(item))
        assertEquals(listOf(item), repository.observePantryItems().first())
    }

    @Test
    fun updatesExistingItemWithoutAddingAnotherRow() = runBlocking {
        val repository = InMemoryPantryRepository()
        repository.savePantryItem(PantryItem("1", "Tomate fresco"))

        val updated = PantryItem("1", "Tomate maduro", 3)
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

    @Test
    fun keepsExpiryDateOnSaveRenameAndClear() = runBlocking {
        val repository = InMemoryPantryRepository()
        val expiry = LocalDate.of(2026, 11, 20)
        val item = PantryItem("1", "Aguacate", 2, expiry)

        assertEquals(PantrySaveResult.Saved, repository.savePantryItem(item))
        assertEquals(expiry, repository.observePantryItems().first().single().expiryDate)

        val renamed = item.copy(name = "Aguacate maduro")
        assertEquals(PantrySaveResult.Saved, repository.savePantryItem(renamed))
        val saved = repository.observePantryItems().first().single()
        assertEquals("Aguacate maduro", saved.name)
        assertEquals(expiry, saved.expiryDate)

        assertEquals(PantrySaveResult.Saved, repository.savePantryItem(renamed.copy(expiryDate = null)))
        assertNull(repository.observePantryItems().first().single().expiryDate)
    }

    @Test
    fun acceptsZeroAndMaximumIntegerQuantitiesAndRejectsValuesOutsideRange() = runBlocking {
        val repository = InMemoryPantryRepository()
        val withoutQuantity = PantryItem("0", "Harina", null)
        val zeroQuantity = PantryItem("1", "Leche", 0)
        val maximumQuantity = PantryItem("2", "Tomate", 99)

        assertEquals(PantrySaveResult.Saved, repository.savePantryItem(withoutQuantity))
        assertEquals(PantrySaveResult.Saved, repository.savePantryItem(zeroQuantity))
        assertEquals(PantrySaveResult.Saved, repository.savePantryItem(maximumQuantity))
        assertEquals(listOf(withoutQuantity, maximumQuantity, zeroQuantity).sortedBy { it.name.lowercase() }, repository.observePantryItems().first())
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { repository.savePantryItem(PantryItem("3", "Too low", -1)) }
        }
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { repository.savePantryItem(PantryItem("4", "Too high", 100)) }
        }
        Unit
    }
}
