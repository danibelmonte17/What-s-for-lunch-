package com.whatsforlunch.app.domain.voice

// Comandos exactos del Modo Cocina por voz (RD24, D2): avanzar de Paso, repetir
// la locución del Paso activo y pausar la cuenta del temporizador en marcha.
enum class VoiceCommand {
    NEXT,
    REPEAT,
    PAUSE_TIMER,
}
