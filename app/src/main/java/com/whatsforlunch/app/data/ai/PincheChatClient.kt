package com.whatsforlunch.app.data.ai

import com.whatsforlunch.app.domain.model.UserProfile
import com.whatsforlunch.app.domain.pinche.PincheReply
import com.whatsforlunch.app.domain.pinche.PincheTurn
import com.whatsforlunch.app.domain.profile.SavedProfilePart

// Resultado de una tanda del Pinche: éxito con texto y, si procede, una Propuesta;
// key ausente (⚠ S05) o cualquier fallo de red, proveedor, parseo o tope de tools
// (RF-PCH9, D10). En el éxito, `profileSaved` son las partes del perfil que
// `save_profile` persistió en este turno (RD27/D2, resumen fiel del chat) y `null`
// cuando no se guardó nada.
sealed interface PincheReplyResult {
    data class Success(
        val reply: PincheReply,
        val profileSaved: List<SavedProfilePart>? = null,
    ) : PincheReplyResult

    data object MissingApiKey : PincheReplyResult
    data object Failed : PincheReplyResult
}

// Puerto de conversación del Pinche (RD17). Recibe solo los turnos que el usuario y
// el asistente ya intercambiaron en memoria, el idioma de la app (D12) y el perfil
// del usuario (`crear-mi-perfil`, D6: viaja en el system prompt); el cliente decide
// cuándo ejecutar las dos funciones de solo lectura y `save_profile`
// (`rellenar-perfil-con-el-pinche`, RD27).
interface PincheChatClient {
    suspend fun reply(turns: List<PincheTurn>, language: String, profile: UserProfile): PincheReplyResult
}
