package com.whatsforlunch.app.data.ai

/**
 * Causas de fallo de IA compartidas entre las tres superficies del MVP.
 *
 * Se aplica tras una petición a /chat/completions que no ha completado con éxito.
 * [MissingApiKey] es un caso previo y distinto que se comprueba antes de la red (D3).
 */
enum class AiFailureCause {
    /** Fallo de red: UnknownHostException, ConnectException, SocketTimeoutException u otro IOException. (D3) */
    NO_CONNECTION,

    /** HTTP no-2xx (401/403/429/5xx) o cualquier respuesta del proveedor que no sea exitosa. */
    PROVIDER_ERROR,

    /** Cuerpo nulo, choices/content ausente, JSON ilegible, imagen no decodificable o petición sin alimentos. */
    INVALID_RESPONSE,
}

/**
 * Utilidad interna para clasificar fallos capturados en excepciones.
 *
 * CancellationException se repropaga antes de llegar aquí (RF-DIA2).
 * IOException (incl. timeout) → [NO_CONNECTION].
 * Cualquier otra excepción no-IOException → [INVALID_RESPONSE].
 */
internal object AiFailure {
    fun fromException(exception: Throwable): AiFailureCause =
        if (exception is java.io.IOException) AiFailureCause.NO_CONNECTION else AiFailureCause.INVALID_RESPONSE
}
