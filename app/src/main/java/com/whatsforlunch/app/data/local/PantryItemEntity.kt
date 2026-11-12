package com.whatsforlunch.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "pantry_items",
    indices = [Index(value = ["normalizedName"], unique = true)],
)
data class PantryItemEntity(
    @PrimaryKey val id: String,
    val name: String,
    val normalizedName: String,
    val quantity: Double?,
    // epochDay de la fecha de caducidad opcional; NULL = sin fecha (RD28, v8).
    val expiryDate: Long?,
)
