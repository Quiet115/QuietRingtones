package com.quietringtones.core

/**
 * La parte de un audio que el usuario conserva: dónde empieza y cuánto dura, en milisegundos.
 * El usuario decide ambos; los métodos solo impiden que la selección se salga del audio.
 */
data class TrimSelection(val durationMs: Long, val startMs: Long, val lengthMs: Long) {
    val endMs: Long get() = startMs + lengthMs
    val isWholeFile: Boolean get() = startMs == 0L && lengthMs >= durationMs

    /** Mueve el inicio sin cambiar la duración; no deja que el final pase del audio. */
    fun withStart(start: Long): TrimSelection =
        copy(startMs = start.coerceIn(0L, (durationMs - lengthMs).coerceAtLeast(0L)))

    /** Cambia la duración; si ya no cabe desde el inicio actual, el inicio retrocede. */
    fun withLength(length: Long): TrimSelection {
        val len = length.coerceIn(minLength(durationMs), durationMs.coerceAtLeast(0L))
        return copy(lengthMs = len, startMs = startMs.coerceAtMost((durationMs - len).coerceAtLeast(0L)))
    }

    fun whole(): TrimSelection = copy(startMs = 0L, lengthMs = durationMs)

    companion object {
        const val DEFAULT_LENGTH_MS = 30_000L
        const val MIN_LENGTH_MS = 1_000L

        fun minLength(durationMs: Long) = minOf(MIN_LENGTH_MS, durationMs.coerceAtLeast(0L))

        /** Al abrir: desde el principio y 30 s, o el audio entero si es más corto. */
        fun initial(durationMs: Long) =
            TrimSelection(durationMs, 0L, minOf(DEFAULT_LENGTH_MS, durationMs.coerceAtLeast(0L)))
    }
}