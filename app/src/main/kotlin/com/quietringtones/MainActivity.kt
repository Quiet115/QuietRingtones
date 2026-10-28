package com.quietringtones

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quietringtones.core.Profile
import com.quietringtones.core.Selection
import com.quietringtones.core.Sound
import com.quietringtones.core.SoundKind

class MainActivity : ComponentActivity() {
    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme { MainScreen(viewModel) }
        }
    }

    override fun onStop() {
        super.onStop()
        viewModel.stopPreview()
    }
}

@Composable
private fun MainScreen(viewModel: AppViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    var tab by remember { mutableIntStateOf(0) }
    var importKind by rememberSaveable { mutableStateOf(SoundKind.RINGTONE) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val name = displayName(context.contentResolver, uri)
            viewModel.importAndUse(importKind, uri, name)
        }
    }

    val message = state.message
    val text = message?.let { stringResource(it.res, *listOfNotNull(it.arg).toTypedArray()) }
    LaunchedEffect(text) {
        if (text != null) {
            snackbar.showSnackbar(text)
            viewModel.dismissMessage()
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text(stringResource(R.string.tab_sounds)) })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text(stringResource(R.string.tab_profiles)) })
            }
            if (!state.canModify) PermissionBanner()
            when (tab) {
                0 -> SoundsTab(
                    state = state,
                    onUse = viewModel::use,
                    onPreview = viewModel::preview,
                    onImport = { kind ->
                        importKind = kind
                        picker.launch(arrayOf("audio/*"))
                    },
                )
                else -> ProfilesTab(
                    profiles = state.profiles,
                    onSave = viewModel::saveProfile,
                    onApply = viewModel::applyProfile,
                    onDelete = viewModel::deleteProfile,
                    onRestore = viewModel::restore,
                )
            }
        }
    }
}

@Composable
private fun PermissionBanner() {
    val context = LocalContext.current
    Card(Modifier.fillMaxWidth().padding(16.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.permission_needed))
            Button(onClick = {
                context.startActivity(
                    Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${context.packageName}")),
                )
            }) { Text(stringResource(R.string.permission_grant)) }
        }
    }
}

@Composable
private fun SoundsTab(
    state: UiState,
    onUse: (SoundKind, Selection) -> Unit,
    onPreview: (Sound) -> Unit,
    onImport: (SoundKind) -> Unit,
) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)) {
        SoundKind.entries.forEach { kind ->
            val current = state.current[kind]
            val sounds = state.available[kind].orEmpty()
            item(key = "header-${kind.name}") {
                Column(Modifier.padding(top = 16.dp)) {
                    Text(stringResource(kind.label()), style = MaterialTheme.typography.titleLarge)
                    Text(
                        stringResource(R.string.active_now, current.displayName()),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    OutlinedButton(onClick = { onImport(kind) }) { Text(stringResource(R.string.import_audio)) }
                }
            }
            if (sounds.isEmpty()) {
                item(key = "empty-${kind.name}") { Text(stringResource(R.string.no_sounds)) }
            }
            items(sounds, key = { "${kind.name}-${it.id}" }) { sound ->
                val isActive = (current as? Selection.Chosen)?.sound == sound
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(sound.name, Modifier.weight(1f))
                    TextButton(onClick = { onPreview(sound) }) { Text(stringResource(R.string.preview)) }
                    if (isActive) {
                        Text(stringResource(R.string.active), style = MaterialTheme.typography.labelLarge)
                    } else {
                        Button(onClick = { onUse(kind, Selection.Chosen(sound)) }) {
                            Text(stringResource(R.string.use))
                        }
                    }
                }
            }
            item(key = "divider-${kind.name}") { HorizontalDivider(Modifier.padding(top = 8.dp)) }
        }
    }
}

@Composable
private fun ProfilesTab(
    profiles: List<Profile>,
    onSave: (String) -> Unit,
    onApply: (String) -> Unit,
    onDelete: (String) -> Unit,
    onRestore: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(stringResource(R.string.profile_name_hint)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            enabled = name.isNotBlank(),
            onClick = {
                onSave(name)
                name = ""
            },
        ) { Text(stringResource(R.string.save_current_profile)) }

        if (profiles.isEmpty()) Text(stringResource(R.string.no_profiles))
        LazyColumn(Modifier.weight(1f)) {
            items(profiles, key = { it.id }) { profile ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(profile.name, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                    Button(onClick = { onApply(profile.id) }) { Text(stringResource(R.string.apply)) }
                    TextButton(onClick = { onDelete(profile.id) }) { Text(stringResource(R.string.delete)) }
                }
            }
        }
        OutlinedButton(onClick = onRestore, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.restore_originals))
        }
    }
}

private fun SoundKind.label() = when (this) {
    SoundKind.RINGTONE -> R.string.kind_ringtone
    SoundKind.NOTIFICATION -> R.string.kind_notification
    SoundKind.ALARM -> R.string.kind_alarm
}

@Composable
private fun Selection?.displayName(): String = when (this) {
    is Selection.Chosen -> sound.name
    else -> stringResource(R.string.silent)
}

private fun displayName(resolver: android.content.ContentResolver, uri: Uri): String =
    resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) cursor.getString(0) else null
    } ?: uri.lastPathSegment ?: "audio"
