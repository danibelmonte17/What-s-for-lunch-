package com.whatsforlunch.app.data.repository

import com.whatsforlunch.app.domain.model.Aisle
import com.whatsforlunch.app.domain.model.RecipeIngredient
import com.whatsforlunch.app.domain.model.ShoppingListItem
import com.whatsforlunch.app.domain.repository.ShoppingListRepository
import com.whatsforlunch.app.domain.shoppinglist.ShoppingListRules
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

// Implementación en memoria para tests rápidos y como doble de la UI, con las
// mismas reglas de fusión idempotente que la implementación Room (RD18).
class InMemoryShoppingListRepository : ShoppingListRepository {
    private val items = MutableStateFlow<List<ShoppingListItem>>(emptyList())
    private val mutex = Mutex()

    override fun observeItems(): Flow<List<ShoppingListItem>> = items.asStateFlow()

    override suspend fun addMissing(
        sourceKey: String,
        sourceTitle: String,
        ingredients: List<RecipeIngredient>,
    ) {
        mutex.withLock {
            items.value = ShoppingListRules.addContributions(
                existing = items.value,
                sourceKey = sourceKey,
                sourceTitle = sourceTitle,
                ingredients = ingredients,
            )
        }
    }

    override suspend fun saveManualItem(
        id: String?,
        name: String,
        quantity: Double?,
        unit: String,
        aisle: Aisle?,
    ) {
        mutex.withLock {
            items.value = ShoppingListRules.saveManualItem(
                existing = items.value,
                id = id,
                name = name,
                quantity = quantity,
                unit = unit,
                aisle = aisle,
            )
        }
    }

    override suspend fun deleteItem(id: String) {
        mutex.withLock { items.value = items.value.filterNot { it.id == id } }
    }

    override suspend fun restoreItem(item: ShoppingListItem) {
        mutex.withLock {
            items.value = items.value.filterNot { it.id == item.id } + item
        }
    }

    override suspend fun setCompleted(id: String, completed: Boolean) {
        mutex.withLock {
            items.value = items.value.map { item ->
                if (item.id == id) item.copy(completed = completed) else item
            }
        }
    }

    override suspend fun restoreItems(items: List<ShoppingListItem>) {
        if (items.isEmpty()) return
        mutex.withLock {
            val restoredIds = items.mapTo(mutableSetOf()) { it.id }
            // `items` es el parámetro (sombrea la propiedad); la propiedad se
            // referencia con `this.` explícito.
            this.items.value = this.items.value.filterNot { it.id in restoredIds } + items
        }
    }
}
