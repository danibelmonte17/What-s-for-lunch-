package com.whatsforlunch.app.data.scanner

import androidx.camera.view.PreviewView

/**
 * Port for a single in-memory scan capture. Implementations must never persist the
 * image: it is analyzed and discarded (RD06).
 */
interface ScanImageSource {
    suspend fun capture(): ByteArray?
}

/** Camera preview lifecycle kept at the Android boundary, outside the ViewModel. */
interface CameraPreviewController {
    fun bindPreview(previewView: PreviewView, onReady: () -> Unit, onFailure: () -> Unit)
    fun unbindPreview()
}
