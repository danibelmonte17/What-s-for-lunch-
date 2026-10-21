package com.whatsforlunch.app.data.scanner

import androidx.camera.view.PreviewView

sealed interface ScanCaptureResult {
    data class Success(val bytes: ByteArray) : ScanCaptureResult
    data object CameraPermissionDenied : ScanCaptureResult
    data object Cancelled : ScanCaptureResult
    data object Failed : ScanCaptureResult
}

/** Port for capturing a single image. Implementations must not persist the image. */
fun interface ScanImageSource {
    suspend fun capture(): ScanCaptureResult
}

/** Camera preview lifecycle kept at the Android boundary, outside the ViewModel. */
interface CameraPreviewController {
    fun bindPreview(previewView: PreviewView, onReady: () -> Unit, onFailure: () -> Unit)
    fun unbindPreview()
}
