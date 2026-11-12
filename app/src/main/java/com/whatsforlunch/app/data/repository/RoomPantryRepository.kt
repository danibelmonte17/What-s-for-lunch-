package com.whatsforlunch.app.data.repository

import android.database.sqlite.SQLiteConstraintException
import com.whatsforlunch.app.data.local.PantryItemDao
import com.whatsforlunch.app.data.local.PantryItemEntity
import com.whatsforlunch.app.domain.model.PantryItem
import com.whatsforlunch.app.domain.pantry.PantryItemValidation
import com.whatsforlunch.app.domain.repository.PantryRepository
import com.whatsforlunch.app.domain.repository.PantrySaveResult
import com.whatsforlunch.app.domain.text.normalizeName
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.roundToInt

class RoomPantryRepository(private val dao: PantryItemDao) : PantryRepository {
    private val saveMutex = Mutex()

    override fun observePantryItems(): Flow<List<PantryItem>> =
        dao.observeAll().map { items -> items.map(PantryItemEntity::toDomain) }

    override suspend fun savePantryItem(item: PantryItem): PantrySaveResult = saveMutex.withLock {
        require(PantryItemValidation.isNameValid(item.name)) { "Pantry item name must not be blank" }
        require(item.quantity == null || PantryItemValidation.isQuantityValid(item.quantity)) {
            "Pantry item quantity must be between ${PantryItemValidation.MIN_QUANTITY} and ${PantryItemValidation.MAX_QUANTITY}"
        }
        val normalizedName = normalizeName(item.name)
        val matchingItem = dao.findByNormalizedName(normalizedName)
        if (matchingItem != null && matchingItem.id != item.id) return@withLock PantrySaveResult.DuplicateName
        val existingItem = dao.findById(item.id)

        val entity = PantryItemEntity(
            id = item.id,
            name = item.name.trim(),
            normalizedName = normalizedName,
            quantity = item.quantity?.toDouble(),
            expiryDate = item.expiryDate?.toEpochDay(),
        )
        try {
            if (existingItem != null) {
                dao.update(entity.id, entity.name, entity.normalizedName, entity.quantity, entity.expiryDate)
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
    quantity = quantity?.roundToInt()?.coerceIn(PantryItemValidation.MIN_QUANTITY, PantryItemValidation.MAX_QUANTITY),
    expiryDate = expiryDate?.let(LocalDate::ofEpochDay),
)
