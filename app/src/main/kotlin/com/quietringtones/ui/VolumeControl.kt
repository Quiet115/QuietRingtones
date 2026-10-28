package com.quietringtones.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.quietringtones.R
import com.quietringtones.VolumeUi
import com.quietringtones.core.SoundKind
import com.quietringtones.core.VolumeAdvice
import kotlin.math.roundToInt

/** Barra de volumen del tipo de sonido, con un aviso cuando no se va a oír. */
@Composable
fun VolumeControl(kind: SoundKind, volume: VolumeUi, onChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    val label = stringResource(kind.volumeLabel())
    val percent = if (volume.max > 0) (volume.index * 100f / volume.max).roundToInt() else 0
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(stringResource(R.string.volume_percent, percent), style = MaterialTheme.typography.bodyMedium)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SpeakerIcon(
                level = if (volume.max > 0) volume.index.toFloat() / volume.max else 0f,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (volume.max > volume.min) {
                Slider(
                    value = volume.index.coerceIn(volume.min, volume.max).toFloat(),
                    onValueChange = { onChange(it.roundToInt()) },
                    valueRange = volume.min.toFloat()..volume.max.toFloat(),
                    modifier = Modifier.weight(1f).semantics { contentDescription = label },
                )
            }
        }
        if (volume.advice != VolumeAdvice.OK) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            ) {
                Text(
                    stringResource(volume.advice.messageRes()),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        }
    }
}

/** Altavoz con tantas ondas como volumen; en cero lleva una cruz. */
@Composable
private fun SpeakerIcon(level: Float, color: Color) {
    Canvas(Modifier.size(22.dp)) {
        val w = size.width
        val h = size.height
        val body = Path().apply {
            moveTo(w * 0.08f, h * 0.38f)
            lineTo(w * 0.28f, h * 0.38f)
            lineTo(w * 0.5f, h * 0.18f)
            lineTo(w * 0.5f, h * 0.82f)
            lineTo(w * 0.28f, h * 0.62f)
            lineTo(w * 0.08f, h * 0.62f)
            close()
        }
        drawPath(body, color)
        val stroke = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
        if (level <= 0f) {
            drawLine(color, Offset(w * 0.64f, h * 0.38f), Offset(w * 0.92f, h * 0.62f), strokeWidth = stroke.width, cap = StrokeCap.Round)
            drawLine(color, Offset(w * 0.92f, h * 0.38f), Offset(w * 0.64f, h * 0.62f), strokeWidth = stroke.width, cap = StrokeCap.Round)
        } else {
            val waves = if (level < 0.34f) 1 else if (level < 0.67f) 2 else 3
            for (i in 1..waves) {
                val r = h * (0.12f + 0.12f * i)
                drawArc(
                    color = color,
                    startAngle = -45f,
                    sweepAngle = 90f,
                    useCenter = false,
                    topLeft = Offset(w * 0.52f - r, h * 0.5f - r),
                    size = Size(r * 2, r * 2),
                    style = stroke,
                )
            }
        }
    }
}

private fun SoundKind.volumeLabel() = when (this) {
    SoundKind.RINGTONE -> R.string.volume_ringtone
    SoundKind.NOTIFICATION -> R.string.volume_notification
    SoundKind.ALARM -> R.string.volume_alarm
}

fun VolumeAdvice.messageRes(): Int = when (this) {
    VolumeAdvice.MUTED, VolumeAdvice.OK -> R.string.advice_muted
    VolumeAdvice.LOW -> R.string.advice_low
    VolumeAdvice.SILENT_MODE -> R.string.advice_silent
    VolumeAdvice.VIBRATE_MODE -> R.string.advice_vibrate
    VolumeAdvice.DO_NOT_DISTURB -> R.string.advice_dnd
}
