package com.whatsforlunch.app.domain.repository

import com.whatsforlunch.app.domain.model.PantryItem
import kotlinx.coroutines.flow.Flow

interface PantryRepository {
    fun observePantryItems(): Flow<List<PantryItem>>

    suspend fun savePantryItem(item: PantryItem): PantrySaveResult

    suspend fun deletePantryItem(id: String)
}

sealed interface PantrySaveResult {
    data object Saved : PantrySaveResult
    data object DuplicateName : PantrySaveResult
}
