package com.whatsforlunch.app.ui.shoppinglist

import com.whatsforlunch.app.data.repository.InMemoryPantryRepository
import com.whatsforlunch.app.data.repository.InMemoryShoppingListRepository
import com.whatsforlunch.app.domain.model.Aisle
import com.whatsforlunch.app.domain.model.PantryItem
import com.whatsforlunch.app.domain.model.RecipeIngredient
import com.whatsforlunch.app.domain.shoppinglist.ShoppingGroupMode
import com.whatsforlunch.app.domain.shoppinglist.ShoppingListRules
import com.whatsforlunch.app.domain.shoppinglist.ShoppingProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Locale

// T5/RF-AFL6: la pantalla observa los ítems ordenados y resuelve el borrado con
// «Deshacer» (D7) sin reglas de negocio propias.
@OptIn(ExperimentalCoroutinesApi::class)
class ShoppingListViewModelTest {

    @Before
    fun setMainDispatcher() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    @Test
    fun itemsAreSortedByNormalizedName() = runTest {
        val repository = InMemoryShoppingListRepository()
        repository.addMissing(
            sourceKey = "paella",
            sourceTitle = "Paella",
            ingredients = listOf(
                RecipeIngredient(name = "Mariscos", quantity = 300.0, unit = "g"),
                RecipeIngredient(name = "Azafrán", quantity = 1.0, unit = "g"),
            ),
        )
        val viewModel = ShoppingListViewModel(repository, InMemoryPantryRepository())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }
        advanceUntilIdle()

        assertEquals(listOf("Azafrán", "Mariscos"), viewModel.uiState.value.items.map { it.name })
        assertNull(viewModel.uiState.value.pendingUndo)
    }

    @Test
    fun deletingAnItemExposesItForUndoAndUndoRestoresIt() = runTest {
        val repository = InMemoryShoppingListRepository()
        repository.addMissing(
            sourceKey = "paella",
            sourceTitle = "Paella",
            ingredients = listOf(RecipeIngredient(name = "Azafrán", quantity = 1.0, unit = "g")),
        )
        val viewModel = ShoppingListViewModel(repository, InMemoryPantryRepository())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }
        advanceUntilIdle()
        val item = viewModel.uiState.value.items.single()

        viewModel.deleteItem(item.id)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.items.isEmpty())
        assertEquals(listOf(item), viewModel.uiState.value.pendingUndo?.items)
        assertEquals(false, viewModel.uiState.value.pendingUndo?.bulk)

        viewModel.undoDelete()
        advanceUntilIdle()
        assertEquals(listOf(item), viewModel.uiState.value.items)
        assertNull(viewModel.uiState.value.pendingUndo)
    }

    @Test
    fun dismissingUndoKeepsTheItemDeleted() = runTest {
        val repository = InMemoryShoppingListRepository()
        repository.addMissing(
            sourceKey = "paella",
            sourceTitle = "Paella",
            ingredients = listOf(RecipeIngredient(name = "Azafrán", quantity = 1.0, unit = "g")),
        )
        val viewModel = ShoppingListViewModel(repository, InMemoryPantryRepository())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }
        advanceUntilIdle()

        viewModel.deleteItem(viewModel.uiState.value.items.single().id)
        advanceUntilIdle()
        viewModel.dismissUndo()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.items.isEmpty())
        assertNull(viewModel.uiState.value.pendingUndo)
    }

    @Test
    fun addItemOpensTheEditorAndSavingStoresTheManualItem() = runTest {
        val repository = InMemoryShoppingListRepository()
        val viewModel = ShoppingListViewModel(repository, InMemoryPantryRepository())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }
        advanceUntilIdle()

        viewModel.addItem()
        assertTrue(viewModel.uiState.value.editorVisible)
        assertNull(viewModel.uiState.value.editing)

        viewModel.saveManualItem(name = "Pan", quantity = 2.0, unit = "uds", aisle = Aisle.PANTRY_OILS)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.editorVisible)
        val item = viewModel.uiState.value.items.single()
        assertEquals("Pan", item.name)
        assertEquals(Aisle.PANTRY_OILS, item.aisle)
    }

    @Test
    fun editItemPreloadsTheDialogAndInvalidDataIsIgnored() = runTest {
        val repository = InMemoryShoppingListRepository()
        repository.addMissing(
            sourceKey = "paella",
            sourceTitle = "Paella",
            ingredients = listOf(RecipeIngredient(name = "Azafrán", quantity = 1.0, unit = "g")),
        )
        val viewModel = ShoppingListViewModel(repository, InMemoryPantryRepository())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }
        advanceUntilIdle()

        viewModel.editItem(ShoppingListRules.itemIdFor("Azafrán"))
        assertEquals("Azafrán", viewModel.uiState.value.editing?.name)

        viewModel.saveManualItem(name = "   ", quantity = 2.0, unit = "", aisle = null)
        advanceUntilIdle()

        // Datos inválidos: no se guarda nada y el diálogo sigue abierto (RF-AIL5).
        assertTrue(viewModel.uiState.value.editorVisible)
        assertEquals(1.0, viewModel.uiState.value.items.single().quantity!!, 0.0001)

        viewModel.dismissEditor()
        assertFalse(viewModel.uiState.value.editorVisible)
    }

    // Repair H1 (validación sesión 88): editar con cantidades ≥ 1000 o de más de
    // 3 decimales conserva el valor exacto cuando el texto del diálogo hace el
    // ida-y-vuelta formatEditableQuantity → parseManualQuantity.
    @Test
    fun editingKeepsLargeAndPreciseQuantitiesExactly() = runTest {
        val repository = InMemoryShoppingListRepository()
        repository.addMissing(
            sourceKey = "paella",
            sourceTitle = "Paella",
            ingredients = listOf(RecipeIngredient(name = "Azafrán", quantity = 1.0, unit = "g")),
        )
        val viewModel = ShoppingListViewModel(repository, InMemoryPantryRepository())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }
        advanceUntilIdle()

        for (quantity in listOf(1200.0, 1234.5, 0.1234)) {
            viewModel.editItem(ShoppingListRules.itemIdFor("Azafrán"))
            val quantityText = formatEditableQuantity(quantity, Locale.forLanguageTag("es-ES"))
            viewModel.saveManualItem(
                name = "Azafrán",
                quantity = ShoppingListRules.parseManualQuantity(quantityText),
                unit = "g",
                aisle = null,
            )
            advanceUntilIdle()

            assertEquals(quantity, viewModel.uiState.value.items.single().quantity!!, 0.0)
        }
    }

    // --- agrupar-y-completar-la-lista (RD20) ---------------------------------

    @Test
    fun groupModeDefaultsToAisleAndSwitchesTheGroups() = runTest {
        val repository = InMemoryShoppingListRepository()
        repository.addMissing(
            sourceKey = "paella",
            sourceTitle = "Paella",
            ingredients = listOf(RecipeIngredient(name = "Azafrán", quantity = 1.0, unit = "g")),
        )
        repository.saveManualItem(id = null, name = "Toallitas", quantity = null, unit = "", aisle = null)
        val viewModel = ShoppingListViewModel(repository, InMemoryPantryRepository())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }
        advanceUntilIdle()

        // D1: arranca en «Pasillo de Super» y todo lo sin Pasillo va al final.
        assertEquals(ShoppingGroupMode.BY_AISLE, viewModel.uiState.value.mode)
        assertEquals(listOf(""), viewModel.uiState.value.groups.map { it.key })

        viewModel.setGroupMode(ShoppingGroupMode.BY_RECIPE)
        advanceUntilIdle()

        assertEquals(ShoppingGroupMode.BY_RECIPE, viewModel.uiState.value.mode)
        // D2: «Paella» primero y «Sin receta» al final.
        assertEquals(listOf("paella", ""), viewModel.uiState.value.groups.map { it.key })
        assertEquals(listOf("Azafrán"), viewModel.uiState.value.groups[0].items.map { it.name })
    }

    @Test
    fun toggleCompletedMarksTheItemAndUpdatesTheProgress() = runTest {
        val repository = InMemoryShoppingListRepository()
        repository.addMissing(
            sourceKey = "paella",
            sourceTitle = "Paella",
            ingredients = listOf(
                RecipeIngredient(name = "Azafrán", quantity = 1.0, unit = "g"),
                RecipeIngredient(name = "Mariscos", quantity = 300.0, unit = "g"),
            ),
        )
        val viewModel = ShoppingListViewModel(repository, InMemoryPantryRepository())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }
        advanceUntilIdle()
        val azafran = viewModel.uiState.value.items.first { it.name == "Azafrán" }

        viewModel.toggleCompleted(azafran.id)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.items.first { it.id == azafran.id }.completed)
        assertEquals(
            ShoppingProgress(completed = 1, total = 2, percent = 50),
            viewModel.uiState.value.progress,
        )
        // D4: marcar no lanza aviso de «Deshacer».
        assertNull(viewModel.uiState.value.pendingUndo)

        // Desmarcar revierte el progreso (Scenario 5).
        viewModel.toggleCompleted(azafran.id)
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.items.first { it.id == azafran.id }.completed)
        assertEquals(
            ShoppingProgress(completed = 0, total = 2, percent = 0),
            viewModel.uiState.value.progress,
        )
    }

    @Test
    fun clearCompletedRemovesOnlyCompletedItemsAndUndoRestoresThemAll() = runTest {
        val repository = InMemoryShoppingListRepository()
        repository.addMissing(
            sourceKey = "paella",
            sourceTitle = "Paella",
            ingredients = listOf(
                RecipeIngredient(name = "Azafrán", quantity = 1.0, unit = "g"),
                RecipeIngredient(name = "Harina", quantity = 200.0, unit = "g"),
                RecipeIngredient(name = "Mariscos", quantity = 300.0, unit = "g"),
                RecipeIngredient(name = "Sal", quantity = 1.0, unit = "g"),
            ),
        )
        val viewModel = ShoppingListViewModel(repository, InMemoryPantryRepository())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }
        advanceUntilIdle()
        val completedIds = viewModel.uiState.value.items.take(2).map { it.id }
        completedIds.forEach { viewModel.toggleCompleted(it) }
        advanceUntilIdle()
        // Estado real de los completados en el momento de «Limpiar comprados».
        val completedItems = viewModel.uiState.value.items.filter { it.id in completedIds }

        viewModel.clearCompleted()
        advanceUntilIdle()
        assertEquals(
            listOf("Mariscos", "Sal"),
            viewModel.uiState.value.items.map { it.name },
        )
        assertEquals(completedItems, viewModel.uiState.value.pendingUndo?.items)
        assertEquals(true, viewModel.uiState.value.pendingUndo?.bulk)

        viewModel.undoDelete()
        advanceUntilIdle()
        // D7/D10: los ítems vuelven con su marca, cantidades y fuentes.
        assertEquals(4, viewModel.uiState.value.items.size)
        completedItems.forEach { restored ->
            val item = viewModel.uiState.value.items.first { it.id == restored.id }
            assertEquals(restored, item)
            assertTrue(item.completed)
        }
    }

    @Test
    fun editingAnItemKeepsItsCompletedState() = runTest {
        val repository = InMemoryShoppingListRepository()
        repository.addMissing(
            sourceKey = "paella",
            sourceTitle = "Paella",
            ingredients = listOf(RecipeIngredient(name = "Azafrán", quantity = 1.0, unit = "g")),
        )
        val viewModel = ShoppingListViewModel(repository, InMemoryPantryRepository())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }
        advanceUntilIdle()
        val id = ShoppingListRules.itemIdFor("Azafrán")
        viewModel.toggleCompleted(id)
        advanceUntilIdle()

        viewModel.editItem(id)
        viewModel.saveManualItem(name = "Azafrán", quantity = 2.0, unit = "g", aisle = Aisle.DAIRY)
        advanceUntilIdle()

        val item = viewModel.uiState.value.items.single()
        assertTrue(item.completed)
        assertEquals(2.0, item.quantity!!, 0.0001)
    }

    // --- pasar marcados a la Despensa (RD29) ---------------------------------

    @Test
    fun addCompletedToPantrySavesOnlyTheMarkedItemsAndRemovesThem() = runTest {
        val repository = InMemoryShoppingListRepository()
        repository.saveManualItem(id = null, name = "Pan", quantity = 2.0, unit = "ud", aisle = null)
        repository.saveManualItem(id = null, name = "Leche", quantity = 1.0, unit = "l", aisle = null)
        repository.saveManualItem(id = null, name = "Arroz", quantity = 1.0, unit = "kg", aisle = null)
        val pantry = InMemoryPantryRepository()
        val viewModel = ShoppingListViewModel(repository, pantry)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }
        advanceUntilIdle()

        viewModel.toggleCompleted(ShoppingListRules.itemIdFor("Pan"))
        advanceUntilIdle()
        viewModel.toggleCompleted(ShoppingListRules.itemIdFor("Leche"))
        advanceUntilIdle()

        viewModel.addCompletedToPantry()
        advanceUntilIdle()

        // Solo los marcados salen de la Lista y llegan a la Despensa.
        assertEquals(listOf("Arroz"), viewModel.uiState.value.items.map { it.name })
        assertEquals(
            listOf("Leche", "Pan"),
            pantry.observePantryItems().first().map { it.name },
        )
        assertEquals(2, viewModel.uiState.value.pantryAddedCount)
        // Sin «Deshacer»: el destino ya es la Despensa (RD29).
        assertNull(viewModel.uiState.value.pendingUndo)

        viewModel.dismissPantryNotice()
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.pantryAddedCount)
    }

    @Test
    fun addCompletedToPantrySumsIntoAnExistingPantryItem() = runTest {
        val repository = InMemoryShoppingListRepository()
        repository.saveManualItem(id = null, name = "Tomate", quantity = 2.0, unit = "ud", aisle = null)
        val pantry = InMemoryPantryRepository()
        pantry.savePantryItem(PantryItem(id = "tomate", name = "Tomate", quantity = 3))
        val viewModel = ShoppingListViewModel(repository, pantry)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }
        advanceUntilIdle()
        viewModel.toggleCompleted(ShoppingListRules.itemIdFor("Tomate"))
        advanceUntilIdle()

        viewModel.addCompletedToPantry()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.items.isEmpty())
        // RD13 sobre el Alimento existente: id y nombre intactos, 3 + 2 = 5 y
        // una sola fila.
        val saved = pantry.observePantryItems().first()
        assertEquals(1, saved.size)
        assertEquals("tomate", saved.single().id)
        assertEquals("Tomate", saved.single().name)
        assertEquals(5, saved.single().quantity)
    }

    @Test
    fun addCompletedToPantryRoundsAndCapsTheQuantity() = runTest {
        val repository = InMemoryShoppingListRepository()
        repository.saveManualItem(id = null, name = "Harina", quantity = 50.0, unit = "g", aisle = null)
        repository.saveManualItem(id = null, name = "Sal", quantity = 0.4, unit = "kg", aisle = null)
        val pantry = InMemoryPantryRepository()
        pantry.savePantryItem(PantryItem(id = "harina", name = "Harina", quantity = 60))
        val viewModel = ShoppingListViewModel(repository, pantry)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }
        advanceUntilIdle()
        viewModel.toggleCompleted(ShoppingListRules.itemIdFor("Harina"))
        advanceUntilIdle()
        viewModel.toggleCompleted(ShoppingListRules.itemIdFor("Sal"))
        advanceUntilIdle()

        viewModel.addCompletedToPantry()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.items.isEmpty())
        val saved = pantry.observePantryItems().first().associateBy { it.name }
        // Recorte al máximo 99 (60 + 50 → 99) y «0,4» → sin cantidad.
        assertEquals(99, saved.getValue("Harina").quantity)
        assertNull(saved.getValue("Sal").quantity)
    }
}
