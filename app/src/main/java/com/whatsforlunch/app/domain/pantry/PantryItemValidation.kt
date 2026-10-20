package com.whatsforlunch.app.domain.pantry

object PantryItemValidation {
    fun isNameValid(name: String): Boolean = name.isNotBlank()

    fun parseQuantity(value: String): Double? {
        if (value.isBlank()) return null
        return value.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0.0 }
    }

    fun isQuantityValid(value: String): Boolean = value.isBlank() || parseQuantity(value) != null
}
