package com.whatsforlunch.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PantryItemDao {
    @Query("SELECT * FROM pantry_items ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<PantryItemEntity>>

    @Query("SELECT * FROM pantry_items WHERE normalizedName = :normalizedName LIMIT 1")
    suspend fun findByNormalizedName(normalizedName: String): PantryItemEntity?

    @Query("SELECT * FROM pantry_items WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): PantryItemEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(item: PantryItemEntity)

    @Query("UPDATE pantry_items SET name = :name, normalizedName = :normalizedName, quantity = :quantity, expiryDate = :expiryDate WHERE id = :id")
    suspend fun update(id: String, name: String, normalizedName: String, quantity: Double?, expiryDate: Long?): Int

    @Query("DELETE FROM pantry_items WHERE id = :id")
    suspend fun deleteById(id: String): Int
}
