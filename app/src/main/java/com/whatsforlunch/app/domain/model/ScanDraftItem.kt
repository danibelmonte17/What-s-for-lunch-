package com.whatsforlunch.app.domain.model

import java.util.UUID

/** Editable, temporary food proposal. Quantity zero represents no quantity. */
data class ScanDraftItem(
    val name: String,
    val confidence: Double?,
    val quantity: Int = 0,
    val id: String = UUID.randomUUID().toString(),
)
