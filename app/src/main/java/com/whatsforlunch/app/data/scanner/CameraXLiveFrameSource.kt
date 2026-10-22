package com.whatsforlunch.app.data.scanner

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
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
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * CameraX live-frame adapter. Every analyzed frame is copied to memory as JPEG bytes and
 * never written to disk (RD06). `STRATEGY_KEEP_ONLY_LATEST` conflates at the camera level:
 * while the consumer is busy, only the newest frame survives.
 */
class CameraXLiveFrameSource(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
) : LiveFrameSource, CameraPreviewController {
    @Volatile private var cameraProvider: ProcessCameraProvider? = null
    @Volatile private var analysisExecutor: ExecutorService? = null
    @Volatile private var frameEmitter: ((ByteArray) -> Unit)? = null
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
                val executor = Executors.newSingleThreadExecutor()
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setResolutionSelector(
                        ResolutionSelector.Builder()
                            .setResolutionStrategy(
                                ResolutionStrategy(
                                    Size(ANALYSIS_WIDTH, ANALYSIS_HEIGHT),
                                    ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER,
                                ),
                            )
                            .build(),
                    )
                    .build()
                    .also { it.setAnalyzer(executor, ::emitFrame) }
                provider.unbindAll()
                provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
                analysisExecutor?.shutdown()
                cameraProvider = provider
                analysisExecutor = executor
                onReady()
            } catch (_: Exception) {
                onFailure()
            } catch (_: LinkageError) {
                onFailure()
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun emitFrame(image: ImageProxy) {
        try {
            val bytes = image.toJpegBytes()
            if (bytes != null && bytes.isNotEmpty()) frameEmitter?.invoke(bytes)
        } catch (_: Exception) {
            // Frame skipped: a single unreadable frame must not stop the live loop.
        } catch (_: LinkageError) {
            // Frame skipped: same rationale as above.
        } finally {
            image.close()
        }
    }

    private fun ImageProxy.toJpegBytes(): ByteArray {
        val bitmap = toBitmap()
        return try {
            ByteArrayOutputStream().use { output ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)
                output.toByteArray()
            }
        } finally {
            bitmap.recycle()
        }
    }

    override fun frames(): Flow<ByteArray> = callbackFlow {
        frameEmitter = { bytes -> trySend(bytes) }
        awaitClose { frameEmitter = null }
    }

    override fun unbindPreview() {
        unbound = true
        frameEmitter = null
        cameraProvider?.unbindAll()
        cameraProvider = null
        analysisExecutor?.shutdown()
        analysisExecutor = null
    }

    private fun hasCameraPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    private companion object {
        const val ANALYSIS_WIDTH = 640
        const val ANALYSIS_HEIGHT = 480
        const val JPEG_QUALITY = 85
    }
}
