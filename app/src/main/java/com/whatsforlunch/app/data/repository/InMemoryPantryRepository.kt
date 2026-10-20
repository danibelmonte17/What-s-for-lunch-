package com.whatsforlunch.app.data.repository

import com.whatsforlunch.app.domain.model.PantryItem
import com.whatsforlunch.app.domain.pantry.PantryItemValidation
import com.whatsforlunch.app.domain.repository.PantryRepository
import com.whatsforlunch.app.domain.repository.PantrySaveResult
import com.whatsforlunch.app.domain.text.normalizeName
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class InMemoryPantryRepository : PantryRepository {
    private val items = MutableStateFlow<List<PantryItem>>(emptyList())
    private val mutex = Mutex()

    override fun observePantryItems(): Flow<List<PantryItem>> = items.asStateFlow()

    override suspend fun savePantryItem(item: PantryItem): PantrySaveResult = mutex.withLock {
        require(PantryItemValidation.isNameValid(item.name)) { "Pantry item name must not be blank" }
        require(item.quantity == null || PantryItemValidation.isQuantityValid(item.quantity)) {
            "Pantry item quantity must be between ${PantryItemValidation.MIN_QUANTITY} and ${PantryItemValidation.MAX_QUANTITY}"
        }
        val normalizedName = normalizeName(item.name)
        if (items.value.any { it.id != item.id && normalizeName(it.name) == normalizedName }) {
            return@withLock PantrySaveResult.DuplicateName
        }
        val saved = item.copy(name = item.name.trim())
        items.value = (items.value.filterNot { it.id == item.id } + saved)
            .sortedBy { it.name.lowercase() }
        PantrySaveResult.Saved
    }

    override suspend fun deletePantryItem(id: String) {
        mutex.withLock { items.value = items.value.filterNot { it.id == id } }
    }
}
