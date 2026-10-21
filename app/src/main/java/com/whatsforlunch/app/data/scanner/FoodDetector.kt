package com.whatsforlunch.app.data.scanner

import com.whatsforlunch.app.domain.model.DetectedFood

sealed interface FoodDetectionResult {
    data class Success(val detections: List<DetectedFood>) : FoodDetectionResult
    data object Failed : FoodDetectionResult
}

fun interface FoodDetector {
    suspend fun detect(image: ByteArray): FoodDetectionResult
}

object NoOpFoodDetector : FoodDetector {
    override suspend fun detect(image: ByteArray): FoodDetectionResult = FoodDetectionResult.Failed
}
