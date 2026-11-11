package com.whatsforlunch.app.domain.model

import java.time.LocalDate

/** Food currently available in the household pantry. */
data class PantryItem(
    val id: String,
    val name: String,
    val quantity: Int? = null,
    // Fecha de caducidad opcional (RD28, `redisenar-vista-de-la-despensa`): null =
    // sin fecha, que se muestra como «Larga duración».
    val expiryDate: LocalDate? = null,
)
