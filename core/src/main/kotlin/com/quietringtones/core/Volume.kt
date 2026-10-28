package com.quietringtones.core

enum class RingerMode { NORMAL, VIBRATE, SILENT }

enum class DoNotDisturb { OFF, PRIORITY, ALARMS_ONLY, TOTAL_SILENCE }

/** Volumen de un tipo de sonido: nivel actual sobre el máximo del sistema. */
data class Volume(val index: Int, val max: Int) {
    val fraction: Double get() = if (max > 0) index.toDouble() / max else 1.0
}

enum class VolumeAdvice { OK, LOW, MUTED, SILENT_MODE, VIBRATE_MODE, DO_NOT_DISTURB }

/** Decide si un sonido va a oírse, para avisar al usuario antes de que pulse reproducir y no suene. */
object VolumeAdvisor {
    /** Hasta este nivel (un cuarto del máximo) el volumen se considera bajo. */
    const val LOW_FRACTION = 0.25

    fun advise(kind: SoundKind, volume: Volume, ringer: RingerMode, dnd: DoNotDisturb): VolumeAdvice {
        // El modo del timbre silencia llamadas y notificaciones, no las alarmas.
        if (kind != SoundKind.ALARM) {
            when (ringer) {
                RingerMode.SILENT -> return VolumeAdvice.SILENT_MODE
                RingerMode.VIBRATE -> return VolumeAdvice.VIBRATE_MODE
                RingerMode.NORMAL -> Unit
            }
        }
        when (dnd) {
            DoNotDisturb.TOTAL_SILENCE -> return VolumeAdvice.DO_NOT_DISTURB
            DoNotDisturb.ALARMS_ONLY -> if (kind != SoundKind.ALARM) return VolumeAdvice.DO_NOT_DISTURB
            DoNotDisturb.OFF, DoNotDisturb.PRIORITY -> Unit
        }
        return when {
            volume.index <= 0 -> VolumeAdvice.MUTED
            volume.max > 0 && volume.fraction <= LOW_FRACTION -> VolumeAdvice.LOW
            else -> VolumeAdvice.OK
        }
    }
}
