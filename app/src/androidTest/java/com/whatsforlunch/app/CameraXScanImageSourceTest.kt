package com.whatsforlunch.app

import android.Manifest
import android.view.ViewGroup
import androidx.camera.view.PreviewView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.whatsforlunch.app.data.scanner.CameraXScanImageSource
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CameraXScanImageSourceTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun grantCameraPermission() {
        InstrumentationRegistry.getInstrumentation().uiAutomation.grantRuntimePermission(
            InstrumentationRegistry.getInstrumentation().targetContext.packageName,
            Manifest.permission.CAMERA,
        )
    }

    @Test
    fun capturesJpegInMemoryWithoutCreatingFiles() {
        val activity = composeRule.activity
        val cacheBefore = imageFiles(activity.cacheDir)
        val filesBefore = imageFiles(activity.filesDir)
        lateinit var previewView: PreviewView
        val ready = CountDownLatch(1)
        val failed = AtomicBoolean(false)
        val source = CameraXScanImageSource(activity, activity)

        composeRule.runOnUiThread {
            previewView = PreviewView(activity)
            activity.addContentView(previewView, ViewGroup.LayoutParams(640, 480))
            source.bindPreview(previewView, { ready.countDown() }, { failed.set(true); ready.countDown() })
        }
        try {
            assertTrue("CameraX preview did not bind", ready.await(30, TimeUnit.SECONDS))
            assertFalse("CameraX preview failed", failed.get())
            val bytes = runBlocking {
                withTimeout(60_000) { source.capture() }
            }
            assertNotNull("CameraX capture returned no bytes", bytes)
            assertTrue("JPEG SOI marker missing", bytes!!.size > 3 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte())
            assertEquals(cacheBefore, imageFiles(activity.cacheDir))
            assertEquals(filesBefore, imageFiles(activity.filesDir))
        } finally {
            composeRule.runOnUiThread {
                source.unbindPreview()
                (previewView.parent as? ViewGroup)?.removeView(previewView)
            }
        }
    }

    private fun imageFiles(directory: java.io.File): Set<String> = directory.listFiles().orEmpty()
        .filter { it.extension.lowercase() in setOf("jpg", "jpeg", "png", "webp") }
        .map { it.name }
        .toSet()
}
