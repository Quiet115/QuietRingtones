package com.quietringtones.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
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

@Composable
fun Waveform(
    seed: String,
    lit: Boolean,
    playing: Boolean,
    modifier: Modifier = Modifier,
) {
    val heights = remember(seed) { barHeights(seed) }
    val sweep = if (playing) sweepProgress() else 0f
    val on = MaterialTheme.colorScheme.primary
    val off = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)

    Canvas(modifier) {
        val slot = size.width / BARS
        heights.forEachIndexed { i, h ->
            val t = i / (BARS - 1f)
            val isOn = if (playing) t <= sweep else lit
            val x = slot * i + slot / 2
            val half = h * size.height / 2
            drawLine(
                color = if (isOn) on else off,
                start = Offset(x, size.height / 2 - half),
                end = Offset(x, size.height / 2 + half),
                strokeWidth = slot * 0.5f,
                cap = StrokeCap.Round,
            )
        }
    }
}

/** El barrido solo existe mientras suena: es la respuesta al toque en reproducir. */
@Composable
private fun sweepProgress(): Float {
    val transition = rememberInfiniteTransition(label = "sweep")
    val value by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Restart),
        label = "sweep",
    )
    return value
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
