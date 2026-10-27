package com.whatsforlunch.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface ShoppingListDao {

    @Transaction
    @Query("SELECT * FROM shopping_items ORDER BY normalizedName ASC")
    fun observeAll(): Flow<List<ShoppingItemWithSources>>

    @Transaction
    @Query("SELECT * FROM shopping_items ORDER BY normalizedName ASC")
    suspend fun getAllOnce(): List<ShoppingItemWithSources>

    @Transaction
    @Query("SELECT * FROM shopping_items WHERE normalizedName = :normalizedName LIMIT 1")
    suspend fun findByNormalizedName(normalizedName: String): ShoppingItemWithSources?

    @Transaction
    @Query("SELECT * FROM shopping_items WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): ShoppingItemWithSources?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ShoppingListItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSources(sources: List<ShoppingItemSourceEntity>)

    @Query("DELETE FROM shopping_items WHERE id = :id")
    suspend fun deleteById(id: String): Int

    // RD20: update dirigido de `completed` (RF-AGL5); evita el REPLACE del
    // padre, que cascada a las fuentes.
    @Query("UPDATE shopping_items SET completed = :completed WHERE id = :id")
    suspend fun updateCompleted(id: String, completed: Boolean)
}
