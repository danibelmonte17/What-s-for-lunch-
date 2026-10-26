package com.whatsforlunch.app.data.repository

import androidx.room.withTransaction
import com.whatsforlunch.app.data.local.RecipeDatabase
import com.whatsforlunch.app.data.local.ShoppingItemSourceEntity
import com.whatsforlunch.app.data.local.ShoppingItemWithSources
import com.whatsforlunch.app.data.local.ShoppingListDao
import com.whatsforlunch.app.data.local.ShoppingListItemEntity
import com.whatsforlunch.app.domain.model.Aisle
import com.whatsforlunch.app.domain.model.RecipeIngredient
import com.whatsforlunch.app.domain.model.ShoppingItemSource
import com.whatsforlunch.app.domain.model.ShoppingListItem
import com.whatsforlunch.app.domain.repository.ShoppingListRepository
import com.whatsforlunch.app.domain.shoppinglist.ShoppingListRules
import com.whatsforlunch.app.domain.text.normalizeName
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class RoomShoppingListRepository(
    private val dao: ShoppingListDao,
    private val database: RecipeDatabase,
) : ShoppingListRepository {
    private val mutex = Mutex()

    override fun observeItems(): Flow<List<ShoppingListItem>> =
        dao.observeAll().map { items -> items.map(ShoppingItemWithSources::toDomain) }

    override suspend fun addMissing(
        sourceKey: String,
        sourceTitle: String,
        ingredients: List<RecipeIngredient>,
    ) {
        if (ingredients.isEmpty()) return
        mutex.withLock {
            database.withTransaction {
                val current = dao.getAllOnce().map(ShoppingItemWithSources::toDomain)
                val merged = ShoppingListRules.addContributions(
                    existing = current,
                    sourceKey = sourceKey,
                    sourceTitle = sourceTitle,
                    ingredients = ingredients,
                )
                merged.forEach { item -> dao.upsertItemWithSources(item) }
            }
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
            database.withTransaction {
                val current = dao.getAllOnce().map(ShoppingItemWithSources::toDomain)
                val updated = ShoppingListRules.saveManualItem(
                    existing = current,
                    id = id,
                    name = name,
                    quantity = quantity,
                    unit = unit,
                    aisle = aisle,
                )
                // Solo se reescribe lo que cambió: `upsertItemWithSources` reinserta
                // las fuentes que el REPLACE del padre borra en cascada.
                val before = current.associateBy(ShoppingListItem::id)
                updated.forEach { item ->
                    if (before[item.id] != item) dao.upsertItemWithSources(item)
                }
            }
        }
    }

    override suspend fun deleteItem(id: String) {
        mutex.withLock { dao.deleteById(id) }
    }

    override suspend fun restoreItem(item: ShoppingListItem) {
        mutex.withLock {
            database.withTransaction { dao.upsertItemWithSources(item) }
        }
    }
}

// `INSERT OR REPLACE` sobre la fila padre dispara el ON DELETE CASCADE de SQLite; se
// reinsertan todas las fuentes dentro de la misma transacción para no perderlas.
private suspend fun ShoppingListDao.upsertItemWithSources(item: ShoppingListItem) {
    insertItem(item.toEntity())
    if (item.sources.isNotEmpty()) insertSources(item.sources.map { it.toEntity(item.id) })
}

private fun ShoppingItemWithSources.toDomain(): ShoppingListItem = ShoppingListItem(
    id = item.id,
    name = item.name,
    quantity = item.quantity,
    unit = item.unit,
    aisle = item.aisle.toAisle(),
    completed = item.completed,
    sources = sources
        .sortedWith(compareBy({ normalizeName(it.sourceTitle) }, { it.sourceKey }))
        .map { ShoppingItemSource(sourceKey = it.sourceKey, sourceTitle = it.sourceTitle) },
)

private fun ShoppingListItem.toEntity(): ShoppingListItemEntity = ShoppingListItemEntity(
    id = id,
    name = name,
    normalizedName = normalizeName(name),
    quantity = quantity,
    unit = unit,
    aisle = aisle?.name,
    completed = completed,
)

// El Pasillo se persiste como el nombre del enum (`PRODUCE`, …) o null
// («Sin clasificar»); un valor legado desconocido degrada a null sin romper la
// lectura.
private fun String?.toAisle(): Aisle? =
    this?.let { raw -> Aisle.entries.firstOrNull { it.name == raw } }

private fun ShoppingItemSource.toEntity(itemId: String): ShoppingItemSourceEntity =
    ShoppingItemSourceEntity(
        itemId = itemId,
        sourceKey = sourceKey,
        sourceTitle = sourceTitle,
    )
