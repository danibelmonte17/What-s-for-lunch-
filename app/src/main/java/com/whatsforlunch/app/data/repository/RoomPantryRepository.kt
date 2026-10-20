package com.whatsforlunch.app.data.repository

import android.database.sqlite.SQLiteConstraintException
import com.whatsforlunch.app.data.local.PantryItemDao
import com.whatsforlunch.app.data.local.PantryItemEntity
import com.whatsforlunch.app.domain.model.PantryItem
import com.whatsforlunch.app.domain.pantry.PantryItemValidation
import com.whatsforlunch.app.domain.repository.PantryRepository
import com.whatsforlunch.app.domain.repository.PantrySaveResult
import com.whatsforlunch.app.domain.text.normalizeName
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class RoomPantryRepository(private val dao: PantryItemDao) : PantryRepository {
    private val saveMutex = Mutex()

    override fun observePantryItems(): Flow<List<PantryItem>> =
        dao.observeAll().map { items -> items.map(PantryItemEntity::toDomain) }

    override suspend fun savePantryItem(item: PantryItem): PantrySaveResult = saveMutex.withLock {
        require(PantryItemValidation.isNameValid(item.name)) { "Pantry item name must not be blank" }
        require(item.quantity == null || item.quantity.isFinite() && item.quantity >= 0.0) {
            "Pantry item quantity must be a finite non-negative number"
        }
        val normalizedName = normalizeName(item.name)
        val matchingItem = dao.findByNormalizedName(normalizedName)
        if (matchingItem != null && matchingItem.id != item.id) return@withLock PantrySaveResult.DuplicateName
        val existingItem = dao.findById(item.id)

        val entity = PantryItemEntity(
            id = item.id,
            name = item.name.trim(),
            normalizedName = normalizedName,
            quantity = item.quantity,
        )
        try {
            if (existingItem != null) {
                dao.update(entity.id, entity.name, entity.normalizedName, entity.quantity)
            } else {
                dao.insert(entity)
            }
            PantrySaveResult.Saved
        } catch (_: SQLiteConstraintException) {
            PantrySaveResult.DuplicateName
        }
    }

    override suspend fun deletePantryItem(id: String) {
        dao.deleteById(id)
    }
}

private fun PantryItemEntity.toDomain() = PantryItem(
    id = id,
    name = name,
    quantity = quantity,
)
