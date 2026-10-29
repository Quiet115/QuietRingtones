package com.quietringtones.trim

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.quietringtones.R
import com.quietringtones.TrimUi
import com.quietringtones.core.TrimSelection
import com.quietringtones.ui.PlayStopButton

/** El usuario elige dónde empieza el tono y cuánto dura: lo que sirva a su teléfono o a su uso. */
@Composable
fun TrimDialog(
    trim: TrimUi,
    previewing: Boolean,
    progress: () -> Float,
    onStart: (Long) -> Unit,
    onLength: (Long) -> Unit,
    onWhole: () -> Unit,
    onPreview: () -> Unit,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    val s = trim.selection
    val minLength = TrimSelection.minLength(s.durationMs)
    val maxStart = (s.durationMs - s.lengthMs).coerceAtLeast(0L)
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.trim_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    trim.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    stringResource(R.string.trim_summary, clock(s.startMs), clock(s.endMs), clock(s.lengthMs)),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(stringResource(R.string.trim_start), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Slider(
                    value = s.startMs.toFloat(),
                    onValueChange = { onStart(it.toLong()) },
                    valueRange = 0f..maxStart.toFloat().coerceAtLeast(1f),
                    enabled = maxStart > 0 && !trim.busy,
                )
                Text(stringResource(R.string.trim_length), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Slider(
                    value = s.lengthMs.toFloat(),
                    onValueChange = { onLength(it.toLong()) },
                    valueRange = minLength.toFloat()..s.durationMs.toFloat().coerceAtLeast(minLength + 1f),
                    enabled = s.durationMs > minLength && !trim.busy,
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PlayStopButton(
                        playing = previewing,
                        onClick = onPreview,
                        onClickLabel = stringResource(if (previewing) R.string.stop else R.string.preview),
                    )
                    if (previewing) {
                        LinearProgressIndicator(progress = { progress().coerceIn(0f, 1f) }, modifier = Modifier.weight(1f))
                    } else {
                        TextButton(onClick = onWhole, enabled = !s.isWholeFile && !trim.busy) {
                            Text(stringResource(R.string.trim_whole))
                        }
                    }
                }
                if (trim.busy) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        CircularProgressIndicator(strokeWidth = 3.dp)
                        Text(stringResource(R.string.trim_busy), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
        confirmButton = { Button(onClick = onConfirm, enabled = !trim.busy) { Text(stringResource(R.string.use)) } },
        dismissButton = { TextButton(onClick = onCancel, enabled = !trim.busy) { Text(stringResource(R.string.cancel)) } },
    )
}

internal fun clock(ms: Long): String {
    val total = ms / 1000
    return "%d:%02d".format(total / 60, total % 60)
}