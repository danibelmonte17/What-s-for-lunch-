package com.whatsforlunch.app.domain.pantry

object PantryItemValidation {
    const val MIN_QUANTITY = 0
    const val MAX_QUANTITY = 99

    fun isNameValid(name: String): Boolean = name.isNotBlank()

    fun isQuantityValid(quantity: Int): Boolean = quantity in MIN_QUANTITY..MAX_QUANTITY
}
