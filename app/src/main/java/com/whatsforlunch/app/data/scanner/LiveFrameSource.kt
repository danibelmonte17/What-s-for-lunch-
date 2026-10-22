package com.whatsforlunch.app.data.scanner

import androidx.camera.view.PreviewView
import kotlinx.coroutines.flow.Flow

/**
 * Port for a continuous stream of in-memory scan frames. Implementations must never
 * persist the frames: they are processed and discarded (RD06).
 */
interface LiveFrameSource {
    fun frames(): Flow<ByteArray>
}

/** Camera preview lifecycle kept at the Android boundary, outside the ViewModel. */
interface CameraPreviewController {
    fun bindPreview(previewView: PreviewView, onReady: () -> Unit, onFailure: () -> Unit)
    fun unbindPreview()
}
