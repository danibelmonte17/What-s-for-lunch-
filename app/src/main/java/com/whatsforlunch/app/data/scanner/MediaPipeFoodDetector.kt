package com.whatsforlunch.app.data.scanner

import android.content.Context
import android.graphics.BitmapFactory
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.objectdetector.ObjectDetector
import com.google.mediapipe.tasks.vision.objectdetector.ObjectDetector.ObjectDetectorOptions
import com.whatsforlunch.app.R
import com.whatsforlunch.app.domain.model.DetectedFood
import com.whatsforlunch.app.domain.model.DetectionBounds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** On-device COCO object detector. Non-food categories are intentionally discarded. */
class MediaPipeFoodDetector(context: Context) : FoodDetector {
    private val appContext = context.applicationContext
    private val detector: ObjectDetector by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        ObjectDetector.createFromOptions(
            appContext,
            ObjectDetectorOptions.builder()
                .setBaseOptions(
                    BaseOptions.builder()
                        .setModelAssetPath(MODEL_ASSET)
                        .build(),
                )
                .setRunningMode(RunningMode.IMAGE)
                .setScoreThreshold(0f)
                .setMaxResults(MAX_RESULTS)
                .build(),
        )
    }

    override suspend fun detect(image: ByteArray): FoodDetectionResult = withContext(Dispatchers.Default) {
        var bitmap: android.graphics.Bitmap? = null
        try {
            bitmap = BitmapFactory.decodeByteArray(image, 0, image.size)
                ?: return@withContext FoodDetectionResult.Failed
            val results = detector.detect(BitmapImageBuilder(bitmap).build())
            val detections = results.detections().mapNotNull { detection ->
                val category = detection.categories().maxByOrNull { it.score() } ?: return@mapNotNull null
                val label = category.categoryName().lowercase()
                val labelId = FOOD_LABELS[label] ?: return@mapNotNull null
                val rect = detection.boundingBox()
                DetectedFood(
                    name = appContext.getString(labelId),
                    confidence = category.score().toDouble(),
                    boundingBox = DetectionBounds(
                        left = (rect.left.toDouble() / bitmap.width).coerceIn(0.0, 1.0),
                        top = (rect.top.toDouble() / bitmap.height).coerceIn(0.0, 1.0),
                        right = (rect.right.toDouble() / bitmap.width).coerceIn(0.0, 1.0),
                        bottom = (rect.bottom.toDouble() / bitmap.height).coerceIn(0.0, 1.0),
                    ),
                )
            }
            FoodDetectionResult.Success(detections)
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            FoodDetectionResult.Failed
        } catch (_: LinkageError) {
            FoodDetectionResult.Failed
        } finally {
            bitmap?.recycle()
        }
    }

    private companion object {
        const val MODEL_ASSET = "efficientdet_lite2_detection_metadata_1.tflite"
        const val MAX_RESULTS = 50

        val FOOD_LABELS = mapOf(
            "apple" to R.string.food_label_apple,
            "banana" to R.string.food_label_banana,
            "broccoli" to R.string.food_label_broccoli,
            "cake" to R.string.food_label_cake,
            "carrot" to R.string.food_label_carrot,
            "donut" to R.string.food_label_donut,
            "hot dog" to R.string.food_label_hot_dog,
            "orange" to R.string.food_label_orange,
            "pizza" to R.string.food_label_pizza,
            "sandwich" to R.string.food_label_sandwich,
        )
    }
}
