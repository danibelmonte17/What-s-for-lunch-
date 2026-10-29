package com.whatsforlunch.app.data.audio

// Puerto de la alarma del temporizador del Paso (RF-GTP4, D4): reproduce el aviso
// sonoro al llegar a cero y lo silencia. La implementación Android usa el tono de
// alarma del sistema (sin permisos ni assets); los tests usan un doble que cuenta
// las llamadas (patrón puerto/impl de data/, igual que PhotoStore).
interface TimerAlarm {
    fun start()

    fun stop()
}
