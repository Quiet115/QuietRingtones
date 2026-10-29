package com.quietringtones.contacts

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.quietringtones.ContactsUi
import com.quietringtones.R
import com.quietringtones.core.Sound
import com.quietringtones.ui.PlayStopButton

/** Sonido por contacto: desactivado de serie; al activarlo se explica qué permisos hacen falta y qué no se guarda. */
@Composable
fun ContactsSettingsTab(
    ui: ContactsUi,
    onDisable: () -> Unit,
    onPermissionResult: (Boolean) -> Unit,
    onPickContact: (android.net.Uri) -> Unit,
    onRemove: (ContactSound) -> Unit,
) {
    var explain by rememberSaveable { mutableStateOf(false) }
    val permissions = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        onPermissionResult(ContactSounds.PERMISSIONS.all { result[it] == true })
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickContact()) { uri ->
        if (uri != null) onPickContact(uri)
    }
    val on = ui.enabled && ui.permitted

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(key = "switch") {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.contacts_switch), style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(R.string.contacts_switch_help),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = on, onCheckedChange = { if (it) explain = true else onDisable() })
            }
        }
        if (ui.enabled && !ui.permitted) {
            item(key = "needs-permission") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.contacts_permission_off), style = MaterialTheme.typography.bodyLarge)
                    OutlinedButton(onClick = { explain = true }) { Text(stringResource(R.string.contacts_allow)) }
                }
            }
        }
        if (on) {
            item(key = "add") {
                Button(onClick = { picker.launch(null) }) { Text(stringResource(R.string.contacts_add)) }
            }
            if (ui.assigned.isEmpty()) {
                item(key = "none") {
                    Text(
                        stringResource(R.string.contacts_none),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(ui.assigned, key = { it.id }) { contact ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(contact.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            contact.soundName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    TextButton(onClick = { onRemove(contact) }) { Text(stringResource(R.string.contacts_remove)) }
                }
            }
        }
    }

    if (explain) {
        AlertDialog(
            onDismissRequest = { explain = false },
            title = { Text(stringResource(R.string.contacts_explain_title)) },
            text = { Text(stringResource(R.string.contacts_explain_body)) },
            confirmButton = {
                Button(onClick = {
                    explain = false
                    permissions.launch(ContactSounds.PERMISSIONS)
                }) { Text(stringResource(R.string.contacts_allow)) }
            },
            dismissButton = { TextButton(onClick = { explain = false }) { Text(stringResource(R.string.not_now)) } },
        )
    }
}

/** Elegir con qué sonido suena el contacto: la lista de tonos de llamada, con vista previa. */
@Composable
fun ContactSoundDialog(
    contactName: String,
    sounds: List<Sound>,
    playing: String?,
    onTogglePreview: (Sound) -> Unit,
    onPick: (Sound) -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.contacts_pick_title, contactName)) },
        text = {
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 380.dp)) {
                items(sounds, key = { it.id }) { sound ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                        PlayStopButton(
                            playing = playing == sound.id,
                            onClick = { onTogglePreview(sound) },
                            onClickLabel = stringResource(if (playing == sound.id) R.string.stop else R.string.preview),
                        )
                        Text(sound.name, Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                        FilledTonalButton(onClick = { onPick(sound) }) { Text(stringResource(R.string.use)) }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel)) } },
    )
}