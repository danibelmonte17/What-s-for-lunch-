package com.whatsforlunch.app.ui.myrecipes

import androidx.lifecycle.ViewModelStore
import com.whatsforlunch.app.data.repository.InMemoryPantryRepository
import com.whatsforlunch.app.data.repository.InMemoryRecipeRepository
import com.whatsforlunch.app.domain.model.PantryItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// RF-MOB3 (enmienda 2026-11-11): `hasFoods` refleja si la Despensa tiene datos,
// que es lo que decide el FAB «Recetas con mi Despensa» en Mi recetario.
@OptIn(ExperimentalCoroutinesApi::class)
class MyRecipesHasFoodsTest {

    @Test
    fun hasFoodsIsFalseWhileThePantryIsEmpty() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModelStore = ViewModelStore()
        try {
            val viewModel = MyRecipesViewModel(InMemoryRecipeRepository(), InMemoryPantryRepository())
            viewModelStore.put("hasFoods", viewModel)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect { }
            }
            runCurrent()

            assertFalse(viewModel.uiState.value.hasFoods)
        } finally {
            viewModelStore.clear()
            runCurrent()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun hasFoodsTurnsTrueAsSoonAsThePantryHasAnItem() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModelStore = ViewModelStore()
        try {
            val pantry = InMemoryPantryRepository()
            val viewModel = MyRecipesViewModel(InMemoryRecipeRepository(), pantry)
            viewModelStore.put("hasFoods", viewModel)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect { }
            }
            runCurrent()
            assertFalse(viewModel.uiState.value.hasFoods)

            pantry.savePantryItem(PantryItem(id = "pantry-tomate", name = "Tomate fresco"))
            runCurrent()

            assertTrue(viewModel.uiState.value.hasFoods)
        } finally {
            viewModelStore.clear()
            runCurrent()
            Dispatchers.resetMain()
        }
    }
}
