package com.whatsforlunch.app.domain.model

import java.time.Instant

/** Temporary scan metadata; image bytes are intentionally not part of this model. */
data class Scan(
    val createdAt: Instant = Instant.now(),
    val detections: List<DetectedFood>,
)
