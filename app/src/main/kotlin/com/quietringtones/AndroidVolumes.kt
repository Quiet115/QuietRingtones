package com.quietringtones

import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import com.quietringtones.core.DoNotDisturb
import com.quietringtones.core.RingerMode
import com.quietringtones.core.SoundKind
import com.quietringtones.core.Volume

/** Volúmenes del sistema de cada tipo de sonido, el modo del timbre y No molestar. */
class AndroidVolumes(context: Context) {
    private val audio = context.getSystemService(AudioManager::class.java)
    private val notifications = context.getSystemService(NotificationManager::class.java)

    private fun stream(kind: SoundKind) = when (kind) {
        SoundKind.RINGTONE -> AudioManager.STREAM_RING
        SoundKind.NOTIFICATION -> AudioManager.STREAM_NOTIFICATION
        SoundKind.ALARM -> AudioManager.STREAM_ALARM
    }

    fun volume(kind: SoundKind) = Volume(audio.getStreamVolume(stream(kind)), audio.getStreamMaxVolume(stream(kind)))

    /**
     * Mínimo que ofrece la barra. Llamadas y notificaciones no bajan de 1: ponerlas en 0 activa el modo
     * silencio y Android lo exige acceso a No molestar, que esta app no pide.
     */
    fun minimum(kind: SoundKind): Int =
        if (kind == SoundKind.ALARM) audio.getStreamMinVolume(stream(kind)) else 1

    /** Puede lanzar SecurityException si No molestar impide cambiar ese volumen. */
    fun set(kind: SoundKind, index: Int) {
        val max = audio.getStreamMaxVolume(stream(kind))
        audio.setStreamVolume(stream(kind), index.coerceIn(minimum(kind), max), 0)
    }

    fun ringerMode() = when (audio.ringerMode) {
        AudioManager.RINGER_MODE_SILENT -> RingerMode.SILENT
        AudioManager.RINGER_MODE_VIBRATE -> RingerMode.VIBRATE
        else -> RingerMode.NORMAL
    }

    fun doNotDisturb() = when (notifications.currentInterruptionFilter) {
        NotificationManager.INTERRUPTION_FILTER_NONE -> DoNotDisturb.TOTAL_SILENCE
        NotificationManager.INTERRUPTION_FILTER_ALARMS -> DoNotDisturb.ALARMS_ONLY
        NotificationManager.INTERRUPTION_FILTER_PRIORITY -> DoNotDisturb.PRIORITY
        else -> DoNotDisturb.OFF
    }
}
