package com.whatsforlunch.app.data.scanner

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.core.graphics.scale
import android.util.Size
import android.view.Surface
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.io.ByteArrayOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.math.roundToInt
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * CameraX adapter for the scanner: keeps the live preview on screen and takes one
 * in-memory JPEG per user action, downscaled to at most 1024 px on the longest side
 * and quality 85 (D6). Nothing is ever written to disk (RD06).
 */
class CameraXScanImageSource(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
) : ScanImageSource, CameraPreviewController {
    @Volatile private var cameraProvider: ProcessCameraProvider? = null
    @Volatile private var imageCapture: ImageCapture? = null
    @Volatile private var captureExecutor: ExecutorService? = null
    @Volatile private var unbound = false

    override fun bindPreview(previewView: PreviewView, onReady: () -> Unit, onFailure: () -> Unit) {
        unbound = false
        if (!hasCameraPermission()) {
            onFailure()
            return
        }
        val providerFuture = ProcessCameraProvider.getInstance(context.applicationContext)
        providerFuture.addListener({
            if (unbound) return@addListener
            try {
                val provider = providerFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val capture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .setResolutionSelector(
                        ResolutionSelector.Builder()
                            .setResolutionStrategy(
                                ResolutionStrategy(
                                    Size(CAPTURE_WIDTH, CAPTURE_HEIGHT),
                                    ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER,
                                ),
                            )
                            .build(),
                    )
                    .build()
                capture.targetRotation = previewView.display?.rotation ?: Surface.ROTATION_0
                provider.unbindAll()
                provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, capture)
                captureExecutor?.shutdown()
                cameraProvider = provider
                imageCapture = capture
                captureExecutor = Executors.newSingleThreadExecutor()
                onReady()
            } catch (_: Exception) {
                onFailure()
            } catch (_: LinkageError) {
                onFailure()
            }
        }, ContextCompat.getMainExecutor(context))
    }

    override suspend fun capture(): ByteArray? {
        val capture = imageCapture ?: return null
        val executor = captureExecutor ?: return null
        return suspendCancellableCoroutine { continuation ->
            capture.takePicture(executor, object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val bytes = try {
                        image.toJpegBytes()
                    } catch (_: Exception) {
                        null
                    } catch (_: LinkageError) {
                        null
                    } finally {
                        image.close()
                    }
                    if (continuation.isActive) continuation.resume(bytes)
                }

                override fun onError(exception: ImageCaptureException) {
                    if (continuation.isActive) continuation.resume(null)
                }
            })
        }
    }

    private fun ImageProxy.toJpegBytes(): ByteArray {
        val source = toBitmap()
        val longestSide = maxOf(source.width, source.height)
        val scaled = if (longestSide > MAX_SIDE) {
            val factor = MAX_SIDE.toFloat() / longestSide
            source.scale(
                (source.width * factor).roundToInt().coerceAtLeast(1),
                (source.height * factor).roundToInt().coerceAtLeast(1),
                filter = true,
            )
        } else {
            source
        }
        return try {
            ByteArrayOutputStream().use { output ->
                scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)
                output.toByteArray()
            }
        } finally {
            if (scaled !== source) scaled.recycle()
            source.recycle()
        }
    }

    override fun unbindPreview() {
        unbound = true
        imageCapture = null
        cameraProvider?.unbindAll()
        cameraProvider = null
        captureExecutor?.shutdown()
        captureExecutor = null
    }

    private fun hasCameraPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    private companion object {
        const val CAPTURE_WIDTH = 1280
        const val CAPTURE_HEIGHT = 960
        const val MAX_SIDE = 1024
        const val JPEG_QUALITY = 85
    }
}
