package com.quietringtones.ui

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quietringtones.R
import com.quietringtones.core.SystemSwitch

/**
 * Sonidos del sistema que no son tonos: Android solo deja activarlos o desactivarlos, no cambiarlos por otros.
 * El teclado y el sonido de carga se gestionan desde los ajustes del propio teléfono.
 */
@Composable
fun OtherSoundsTab(
    states: Map<SystemSwitch, Boolean>,
    canModify: Boolean,
    onSwitch: (SystemSwitch, Boolean) -> Unit,
) {
    val context = LocalContext.current
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(key = "intro") {
            Text(
                stringResource(R.string.other_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        SystemSwitch.entries.forEach { switch ->
            item(key = switch.name) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(switch.titleRes()), style = MaterialTheme.typography.titleMedium)
                        Text(
                            stringResource(switch.helpRes()),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = states[switch] ?: true, enabled = canModify, onCheckedChange = { onSwitch(switch, it) })
                }
            }
        }
        item(key = "divider") { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant) }
        item(key = "keyboard") {
            ShortcutCard(R.string.other_keyboard_title, R.string.other_keyboard_body, R.string.other_keyboard_open) {
                context.open(Settings.ACTION_INPUT_METHOD_SETTINGS)
            }
        }
        item(key = "charging") {
            ShortcutCard(R.string.other_charging_title, R.string.other_charging_body, R.string.other_sound_settings_open) {
                context.open(Settings.ACTION_SOUND_SETTINGS)
            }
        }
    }
}

@Composable
private fun ShortcutCard(title: Int, body: Int, button: Int, onClick: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(onClick = onClick) { Text(stringResource(button)) }
    }
}

private fun Context.open(action: String) {
    runCatching { startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

private fun SystemSwitch.titleRes() = when (this) {
    SystemSwitch.TOUCH -> R.string.other_touch
    SystemSwitch.DIAL_PAD -> R.string.other_dial
    SystemSwitch.LOCK -> R.string.other_lock
}

private fun SystemSwitch.helpRes() = when (this) {
    SystemSwitch.TOUCH -> R.string.other_touch_help
    SystemSwitch.DIAL_PAD -> R.string.other_dial_help
    SystemSwitch.LOCK -> R.string.other_lock_help
}
