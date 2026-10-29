package com.whatsforlunch.app.data.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager

// Alarma del temporizador con el tono de alarma del sistema (D4): `Ringtone` en
// bucle (D8) hasta `stop()`. Sin permisos, sin assets ni dependencias nuevas; si el
// dispositivo no tiene tono de alarma configurado cae al tono de notificación.
class SystemRingtoneTimerAlarm(context: Context) : TimerAlarm {

    private val appContext = context.applicationContext

    private var ringtone: Ringtone? = null

    override fun start() {
        stop()
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val tone = RingtoneManager.getRingtone(appContext, uri) ?: return
        // Suena por el stream de alarma del dispositivo (USAGE_ALARM), no por el general.
        tone.audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        tone.isLooping = true
        tone.play()
        ringtone = tone
    }

    override fun stop() {
        ringtone?.let { tone ->
            if (tone.isPlaying) tone.stop()
        }
        ringtone = null
    }
}
