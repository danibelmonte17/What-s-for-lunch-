package com.whatsforlunch.app.data.ai

// Puerto de escritura de la API key del dispositivo: Ajustes guarda, sustituye y
// borra la credencial; el resto de la app la consume como AiKeyProvider (D2/D4).
interface AiKeyStore : AiKeyProvider {
    fun save(key: String)

    fun clear()
}
