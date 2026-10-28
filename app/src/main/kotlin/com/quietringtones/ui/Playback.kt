package com.quietringtones.ui

/** Fracción reproducida (0 a 1), o -1 si no se conoce la duración, para que la onda no se quede muda. */
fun playbackFraction(positionMs: Int, durationMs: Int): Float =
    if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else -1f
