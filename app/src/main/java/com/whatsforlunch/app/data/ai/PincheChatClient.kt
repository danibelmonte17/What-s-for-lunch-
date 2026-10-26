package com.whatsforlunch.app.data.ai

import com.whatsforlunch.app.domain.pinche.PincheReply
import com.whatsforlunch.app.domain.pinche.PincheTurn

// Resultado de una tanda del Pinche: éxito con texto y, si procede, una Propuesta;
// key ausente (⚠ S05) o cualquier fallo de red, proveedor, parseo o tope de tools
// (RF-PCH9, D10).
sealed interface PincheReplyResult {
    data class Success(val reply: PincheReply) : PincheReplyResult
    data object MissingApiKey : PincheReplyResult
    data object Failed : PincheReplyResult
}

// Puerto de conversación del Pinche (RD17). Recibe solo los turnos que el usuario y
// el asistente ya intercambiaron en memoria y el idioma de la app (D12); el cliente
// decide cuándo ejecutar las dos funciones de solo lectura.
interface PincheChatClient {
    suspend fun reply(turns: List<PincheTurn>, language: String): PincheReplyResult
}
