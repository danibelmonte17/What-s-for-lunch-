package com.whatsforlunch.app.domain.model

/** Food currently available in the household pantry. */
data class PantryItem(
    val id: String,
    val name: String,
    val quantity: Int? = null,
)
