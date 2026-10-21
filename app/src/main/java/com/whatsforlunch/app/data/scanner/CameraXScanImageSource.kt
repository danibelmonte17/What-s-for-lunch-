package com.whatsforlunch.app.data.scanner

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.nio.ByteBuffer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/** CameraX capture adapter. Captured JPEG bytes are copied to memory and never written to disk. */
class CameraXScanImageSource(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
) : ScanImageSource, CameraPreviewController {
    @Volatile private var cameraProvider: ProcessCameraProvider? = null
    @Volatile private var imageCapture: ImageCapture? = null
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
                    .build()
                provider.unbindAll()
                provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, capture)
                cameraProvider = provider
                imageCapture = capture
                onReady()
            } catch (_: Exception) {
                onFailure()
            } catch (_: LinkageError) {
                onFailure()
            }
        }, ContextCompat.getMainExecutor(context))
    }

    override fun unbindPreview() {
        unbound = true
        cameraProvider?.unbindAll()
        cameraProvider = null
        imageCapture = null
    }

    override suspend fun capture(): ScanCaptureResult = withContext(Dispatchers.Main.immediate) {
        if (!hasCameraPermission()) return@withContext ScanCaptureResult.CameraPermissionDenied
        val capture = imageCapture ?: return@withContext ScanCaptureResult.Failed
        suspendCancellableCoroutine { continuation ->
            try {
                capture.takePicture(
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageCapturedCallback() {
                        override fun onCaptureSuccess(image: ImageProxy) {
                            val result = try {
                                val bytes = image.planes.firstOrNull()?.buffer?.let(::copyBytes)
                                if (bytes == null || bytes.isEmpty()) ScanCaptureResult.Failed else ScanCaptureResult.Success(bytes)
                            } catch (_: Exception) {
                                ScanCaptureResult.Failed
                            } finally {
                                image.close()
                            }
                            if (continuation.isActive) continuation.resume(result)
                        }

                        override fun onError(exception: ImageCaptureException) {
                            if (continuation.isActive) continuation.resume(ScanCaptureResult.Failed)
                        }
                    },
                )
            } catch (_: Exception) {
                if (continuation.isActive) continuation.resume(ScanCaptureResult.Failed)
            } catch (_: LinkageError) {
                if (continuation.isActive) continuation.resume(ScanCaptureResult.Failed)
            }
        }
    }

    private fun hasCameraPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    private fun copyBytes(source: ByteBuffer): ByteArray = source.duplicate().let { buffer ->
        ByteArray(buffer.remaining()).also(buffer::get)
    }
}
