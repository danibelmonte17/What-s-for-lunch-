package com.whatsforlunch.app.ui.shoppinglist

import com.whatsforlunch.app.data.repository.InMemoryShoppingListRepository
import com.whatsforlunch.app.domain.model.RecipeIngredient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

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
        val viewModel = ShoppingListViewModel(repository)
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
        val viewModel = ShoppingListViewModel(repository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }
        advanceUntilIdle()
        val item = viewModel.uiState.value.items.single()

        viewModel.deleteItem(item.id)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.items.isEmpty())
        assertEquals(item, viewModel.uiState.value.pendingUndo)

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
        val viewModel = ShoppingListViewModel(repository)
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
}
