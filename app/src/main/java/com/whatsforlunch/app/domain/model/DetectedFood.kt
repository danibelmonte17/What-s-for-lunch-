package com.whatsforlunch.app.domain.model

/** One object-detector result. Bounds are normalized to the captured image. */
data class DetectedFood(
    val name: String,
    val confidence: Double,
    val boundingBox: DetectionBounds? = null,
)

data class DetectionBounds(
    val left: Double,
    val top: Double,
    val right: Double,
    val bottom: Double,
)
