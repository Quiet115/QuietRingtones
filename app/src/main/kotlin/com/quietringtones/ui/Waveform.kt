package com.quietringtones.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

private const val BARS = 36

/** Cada sonido tiene su silueta: sale de su identificador, así que es siempre la misma. */
private fun barHeights(seed: String): List<Float> {
    var state = seed.hashCode().toLong() and 0xFFFFFFFFL
    fun next(): Float {
        state = (state * 1664525L + 1013904223L) and 0xFFFFFFFFL
        return (state ushr 8).toFloat() / (1 shl 24)
    }
    return List(BARS) { i ->
        val envelope = 0.55f + 0.45f * sin(PI * (i + 0.5) / BARS).toFloat()
        (0.14f + 0.86f * next()) * envelope
    }
}

/**
 * [progress] es la posición real de la reproducción (0 a 1), o null si no suena. Se lee al dibujar,
 * no al componer, para que el barrido siga al audio sin recomponer la pantalla en cada fotograma.
 * Un valor negativo significa "suena, pero no se conoce la duración": se ilumina entera.
 */
@Composable
fun Waveform(
    seed: String,
    lit: Boolean,
    progress: (() -> Float)?,
    modifier: Modifier = Modifier,
) {
    val heights = remember(seed) { barHeights(seed) }
    val accent = MaterialTheme.colorScheme.primary
    val accent2 = MaterialTheme.colorScheme.tertiary
    val off = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)

    Canvas(modifier) {
        val slot = size.width / BARS
        val position = progress?.invoke()
        heights.forEachIndexed { i, h ->
            val t = i / (BARS - 1f)
            val isOn = when {
                position == null -> lit
                position < 0f -> true
                else -> t <= position
            }
            val x = slot * i + slot / 2
            val half = h * size.height / 2
            drawLine(
                color = if (isOn) lerp(accent, accent2, t) else off,
                start = Offset(x, size.height / 2 - half),
                end = Offset(x, size.height / 2 + half),
                strokeWidth = slot * 0.5f,
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
fun PlayStopButton(playing: Boolean, onClick: () -> Unit, onClickLabel: String) {
    val colors = MaterialTheme.colorScheme
    val background = if (playing) colors.primary else colors.surfaceVariant
    val glyph = if (playing) colors.onPrimary else colors.onSurface
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(background)
            .clickable(role = Role.Button, onClickLabel = onClickLabel, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(18.dp)) {
            if (playing) {
                drawRoundRect(glyph, size = Size(size.width * 0.78f, size.height * 0.78f),
                    topLeft = Offset(size.width * 0.11f, size.height * 0.11f), cornerRadius = CornerRadius(3.dp.toPx()))
            } else {
                val path = Path().apply {
                    moveTo(size.width * 0.24f, size.height * 0.1f)
                    lineTo(size.width * 0.92f, size.height * 0.5f)
                    lineTo(size.width * 0.24f, size.height * 0.9f)
                    close()
                }
                drawPath(path, glyph)
            }
        }
    }
}

@Composable
fun CheckMark(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(16.dp)) {
        val path = Path().apply {
            moveTo(size.width * 0.12f, size.height * 0.54f)
            lineTo(size.width * 0.4f, size.height * 0.8f)
            lineTo(size.width * 0.88f, size.height * 0.2f)
        }
        drawPath(path, color, style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round))
    }
}
