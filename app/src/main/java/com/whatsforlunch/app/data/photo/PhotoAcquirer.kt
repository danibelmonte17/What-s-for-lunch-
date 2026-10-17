package com.whatsforlunch.app.data.photo

// Resultado de una operación de foto del formulario de alta.
// Distingue el éxito de los avisos que la UI debe mostrar (RF-21, RF-22, RF-23).
sealed interface PhotoResult {
    class Success(val bytes: ByteArray) : PhotoResult
    data object Cancelled : PhotoResult
    data object CameraPermissionDenied : PhotoResult
    data object MissingApiKey : PhotoResult
    data object GenerationFailed : PhotoResult
}

// Puerto de foto (Parte B, T6): la galería, la cámara y «Generar con IA».
// La UI depende de esta abstracción; la implementación real usa el sistema y los
// tests la sustituyen por un fake que inyecta una imagen de prueba.
interface PhotoAcquirer {
    suspend fun pickFromGallery(): PhotoResult

    suspend fun takePhoto(): PhotoResult

    suspend fun generate(prompt: String): PhotoResult
}

// Implementación por defecto: sin efecto. La Route la sustituye por la real
// (SystemPhotoAcquirer) al montar la pantalla de alta.
class NoOpPhotoAcquirer : PhotoAcquirer {
    override suspend fun pickFromGallery(): PhotoResult = PhotoResult.Cancelled

    override suspend fun takePhoto(): PhotoResult = PhotoResult.Cancelled

    override suspend fun generate(prompt: String): PhotoResult = PhotoResult.GenerationFailed
}
