package com.whatsforlunch.app

import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.whatsforlunch.app.data.scanner.FoodDetectionResult
import com.whatsforlunch.app.data.scanner.MediaPipeFoodDetector
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MediaPipeFoodDetectorTest {
    @Test
    fun lite2DetectsFoodAboveAcceptedThresholdAndFiltersNonFoodLabels() = runBlocking {
        assertTrue("MediaPipe detector requires API 30+", Build.VERSION.SDK_INT >= 30)
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val image = instrumentation.context.assets.open("scanner/banana.jpg").use { it.readBytes() }
        val detector = MediaPipeFoodDetector(instrumentation.targetContext)

        val coldStart = System.nanoTime()
        val result = detector.detect(image)
        val coldStartMs = (System.nanoTime() - coldStart) / 1_000_000

        assertTrue("Lite2 should return a successful detection result", result is FoodDetectionResult.Success)
        // D2 latency evidence: the cold call includes the one-time model load; the following
        // calls are the steady-state per-frame inference of the live scanner. Reported in
        // docs/technical-discovery.md; no assertion on timing (the AVD is shared).
        val samplesMs = (1..5).map {
            val start = System.nanoTime()
            val sample = detector.detect(image)
            assertTrue("Timed sample $it failed", sample is FoodDetectionResult.Success)
            (System.nanoTime() - start) / 1_000_000
        }
        println(
            "D2 latency (ms) — cold start (incl. model load): $coldStartMs, steady samples: $samplesMs, " +
                "avg=${samplesMs.average()}, max=${samplesMs.maxOrNull()}",
        )
        val detections = (result as FoodDetectionResult.Success).detections
        val banana = instrumentation.targetContext.getString(R.string.food_label_banana)
        assertTrue(
            "Expected banana with confidence >= 0.60, got $detections",
            detections.any { it.name == banana && it.confidence >= 0.60 },
        )
        val foodLabels = setOf(
            R.string.food_label_apple,
            R.string.food_label_banana,
            R.string.food_label_broccoli,
            R.string.food_label_cake,
            R.string.food_label_carrot,
            R.string.food_label_donut,
            R.string.food_label_hot_dog,
            R.string.food_label_orange,
            R.string.food_label_pizza,
            R.string.food_label_sandwich,
        ).map(instrumentation.targetContext::getString).toSet()
        assertTrue("Detector must filter non-food COCO classes: $detections", detections.all { it.name in foodLabels })
    }
}
