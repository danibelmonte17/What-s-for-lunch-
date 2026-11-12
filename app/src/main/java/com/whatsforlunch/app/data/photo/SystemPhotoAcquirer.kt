package com.whatsforlunch.app.data.photo

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.whatsforlunch.app.data.ai.ImageGenerationResult
import com.whatsforlunch.app.data.ai.ImageGenerator
import java.io.File
import java.io.IOException
import kotlin.coroutines.resume
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine

// Implementación real del puerto de foto (Parte B, T6): galería con PickVisualMedia,
// captura con TakePicture (permiso CAMERA en manifest y en tiempo de ejecución) y
// «Generar con IA» delegando en ImageGenerator. Se construye en la Route, que es
// quien dispone del ActivityResultRegistry; los tests inyectan un fake.
class SystemPhotoAcquirer(
    private val context: Context,
    registry: ActivityResultRegistry,
    private val imageGenerator: ImageGenerator,
) : PhotoAcquirer {

    private var pendingGallery: CancellableContinuation<PhotoResult>? = null
    private var pendingCapture: CancellableContinuation<PhotoResult>? = null
    private var pendingPermission: CancellableContinuation<Boolean>? = null
    private var captureUri: Uri? = null

    private val galleryLauncher = registry.register(GALLERY_KEY, ActivityResultContracts.PickVisualMedia()) { uri ->
        val continuation = pendingGallery
        pendingGallery = null
        continuation?.resume(uri?.let(::readBytes)?.let { PhotoResult.Success(it) } ?: PhotoResult.Cancelled)
    }

    private val permissionLauncher =
        registry.register(PERMISSION_KEY, ActivityResultContracts.RequestPermission()) { granted ->
            val continuation = pendingPermission
            pendingPermission = null
            continuation?.resume(granted)
        }

    private val cameraLauncher = registry.register(CAMERA_KEY, ActivityResultContracts.TakePicture()) { success ->
        val continuation = pendingCapture
        pendingCapture = null
        val uri = captureUri
        captureUri = null
        val bytes = if (success && uri != null) readBytes(uri) else null
        continuation?.resume(bytes?.let { PhotoResult.Success(it) } ?: PhotoResult.Cancelled)
    }

    override suspend fun pickFromGallery(): PhotoResult = suspendCancellableCoroutine { continuation ->
        pendingGallery = continuation
        galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    override suspend fun takePhoto(): PhotoResult {
        if (!hasCameraPermission()) {
            val granted = suspendCancellableCoroutine { continuation ->
                pendingPermission = continuation
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }
            if (!granted) return PhotoResult.CameraPermissionDenied
        }
        val uri = createCaptureUri() ?: return PhotoResult.Cancelled
        captureUri = uri
        return suspendCancellableCoroutine { continuation ->
            pendingCapture = continuation
            cameraLauncher.launch(uri)
        }
    }

    override suspend fun generate(prompt: String): PhotoResult = when (val result: ImageGenerationResult = imageGenerator.generate(prompt)) {
        is ImageGenerationResult.Success -> PhotoResult.Success(result.bytes)
        is ImageGenerationResult.MissingApiKey -> PhotoResult.MissingApiKey
        is ImageGenerationResult.Failed -> PhotoResult.GenerationFailed(cause = result.cause)
    }

    private fun hasCameraPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    private fun createCaptureUri(): Uri? {
        val dir = File(context.cacheDir, CAPTURE_DIR)
        if (!dir.exists() && !dir.mkdirs()) return null
        val file = File(dir, "capture_${System.currentTimeMillis()}.jpg")
        return try {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    private fun readBytes(uri: Uri): ByteArray? = try {
        context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
    } catch (_: IOException) {
        null
    } catch (_: SecurityException) {
        null
    }

    private companion object {
        const val GALLERY_KEY = "create_recipe_pick_gallery"
        const val PERMISSION_KEY = "create_recipe_request_camera"
        const val CAMERA_KEY = "create_recipe_take_photo"
        const val CAPTURE_DIR = "captures"
    }
}
