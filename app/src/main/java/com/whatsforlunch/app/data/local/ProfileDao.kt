package com.whatsforlunch.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profile WHERE id = :id LIMIT 1")
    fun observeRow(id: String): Flow<ProfileEntity?>

    @Query("SELECT * FROM profile_restrictions ORDER BY id ASC")
    fun observeRestrictions(): Flow<List<ProfileRestrictionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRow(row: ProfileEntity)

    @Query("DELETE FROM profile_restrictions")
    suspend fun clearRestrictions()

    @Insert
    suspend fun insertRestrictions(restrictions: List<ProfileRestrictionEntity>)

    /**
     * Guardado completo del perfil en una sola transacción: la fila única, el
     * vaciado de las chips y su re-inserción en el orden dado (RD25), de modo que
     * el orden de la lista es el orden de alta.
     */
    @Transaction
    suspend fun replaceProfile(row: ProfileEntity, restrictions: List<ProfileRestrictionEntity>) {
        upsertRow(row)
        clearRestrictions()
        insertRestrictions(restrictions)
    }
}
