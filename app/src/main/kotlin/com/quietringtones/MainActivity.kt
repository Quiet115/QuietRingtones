package com.quietringtones

import android.app.StatusBarManager
import android.content.ComponentName
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quietringtones.core.Profile
import com.quietringtones.core.QuietRingtones
import com.quietringtones.core.Selection
import com.quietringtones.core.OnlineSound
import com.quietringtones.core.Sound
import com.quietringtones.core.SoundCatalog
import com.quietringtones.core.SoundKind
import com.quietringtones.core.forKind
import com.quietringtones.online.CatalogSettingsTab
import com.quietringtones.ui.CheckMark
import com.quietringtones.ui.Language
import com.quietringtones.ui.LanguageStore
import com.quietringtones.ui.SettingsTab
import com.quietringtones.ui.withLanguage
import com.quietringtones.updates.UpdatesTab
import com.quietringtones.ui.PlayStopButton
import com.quietringtones.ui.QuietTheme
import com.quietringtones.ui.VolumeControl
import com.quietringtones.ui.Waveform
import com.quietringtones.ui.donateColors
import android.graphics.Color

private const val DONATION_URL = "https://ko-fi.com/quiet115"

class MainActivity : ComponentActivity() {
    private val viewModel: AppViewModel by viewModels()

    /** Idioma con el que se creó esta pantalla; si el elegido cambia, se recrea. */
    private var activeLanguage = Language.ENGLISH

    override fun attachBaseContext(newBase: Context) {
        activeLanguage = LanguageStore(newBase).load()
        super.attachBaseContext(newBase.withLanguage(activeLanguage))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val appearance by viewModel.appearance.collectAsStateWithLifecycle()
            val language by viewModel.language.collectAsStateWithLifecycle()
            LaunchedEffect(language) { if (language != activeLanguage) recreate() }
            val dark = appearance.isDark(isSystemInDarkTheme())
            SideEffect {
                val bars = if (dark) SystemBarStyle.dark(Color.TRANSPARENT) else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                enableEdgeToEdge(statusBarStyle = bars, navigationBarStyle = bars)
            }
            QuietTheme(appearance) { MainScreen(viewModel) }
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.startVolumeWatch()
    }

    override fun onStop() {
        super.onStop()
        viewModel.stopPreview()
        viewModel.stopVolumeWatch()
    }
}

@Composable
private fun MainScreen(viewModel: AppViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val playing by viewModel.playing.collectAsStateWithLifecycle()
    // Se lee al dibujar la onda, no al componer: el barrido no recompone la pantalla.
    val progress = viewModel.progress.collectAsStateWithLifecycle()
    val volumes by viewModel.volume.collectAsStateWithLifecycle()
    val onlineState by viewModel.online.collectAsStateWithLifecycle()
    val downloading by viewModel.downloading.collectAsStateWithLifecycle()
    val showOnline by viewModel.showOnline.collectAsStateWithLifecycle()
    val cacheSize by viewModel.cacheSize.collectAsStateWithLifecycle()
    val appearance by viewModel.appearance.collectAsStateWithLifecycle()
    val language by viewModel.language.collectAsStateWithLifecycle()
    val updates by viewModel.updates.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var importKind by rememberSaveable { mutableStateOf(SoundKind.RINGTONE) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }
    LaunchedEffect(Unit) { viewModel.autoCheckOnStart() }
    // El catálogo online se pide la primera vez que se abre una pestaña de sonidos, no al arrancar.
    LaunchedEffect(tab) { if (tab < SoundKind.entries.size) viewModel.loadOnline() }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            viewModel.importAndUse(importKind, uri, displayName(context.contentResolver, uri))
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

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Header()
            QuietTabs(selected = tab, onSelect = { tab = it })
            if (!state.canModify) PermissionStrip()
            val kind = SoundKind.entries.getOrNull(tab)
            if (kind != null) {
                SoundsTab(
                    kind = kind,
                    state = state,
                    playing = playing,
                    progress = { progress.value },
                    volume = volumes[kind],
                    onVolume = { viewModel.setVolume(kind, it) },
                    onUse = viewModel::use,
                    onTogglePreview = { sound -> viewModel.togglePreview(kind, sound) },
                    onImport = {
                        importKind = kind
                        picker.launch(arrayOf("audio/*"))
                    },
                    online = OnlineUi(showOnline, onlineState, downloading),
                    onPreviewOnline = { sound -> viewModel.previewOnline(kind, sound) },
                    onUseOnline = { sound -> viewModel.useOnline(kind, sound) },
                    onRetryOnline = viewModel::retryOnline,
                )
            } else if (tab == SoundKind.entries.size) {
                ProfilesTab(
                    profiles = state.profiles,
                    onSave = viewModel::saveProfile,
                    onApply = viewModel::applyProfile,
                    onDelete = viewModel::deleteProfile,
                )
            } else {
                SettingsTab(
                    appearance = appearance,
                    onAppearance = viewModel::setAppearance,
                    language = language,
                    onLanguage = viewModel::setLanguage,
                    catalogPage = {
                        CatalogSettingsTab(
                            showOnline = showOnline,
                            onShowOnline = viewModel::setShowOnline,
                            cacheBytes = cacheSize,
                            onClearCache = viewModel::clearPreviewCache,
                        )
                    },
                    updatesPage = {
                        UpdatesTab(
                            ui = updates,
                            onAutomatic = viewModel::setAutomaticUpdates,
                            onCheck = viewModel::checkNow,
                            onDownload = viewModel::download,
                            onSkip = viewModel::skip,
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun Header() {
    val context = LocalContext.current
    val donate = donateColors()
    Row(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge)
        Surface(
            onClick = { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(DONATION_URL))) } },
            shape = CircleShape,
            color = donate.container,
            contentColor = donate.content,
            modifier = Modifier.minimumInteractiveComponentSize(),
        ) {
            Text(
                stringResource(R.string.donate),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun QuietTabs(selected: Int, onSelect: (Int) -> Unit) {
    val labels = SoundKind.entries.map { it.tabLabel() } + R.string.tab_profiles + R.string.tab_settings
    ScrollableTabRow(
        selectedTabIndex = selected,
        edgePadding = 8.dp,
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.primary,
    ) {
        labels.forEachIndexed { index, label ->
            Tab(
                selected = selected == index,
                onClick = { onSelect(index) },
                selectedContentColor = MaterialTheme.colorScheme.primary,
                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                text = { Text(stringResource(label)) },
            )
        }
    }
}

@Composable
private fun PermissionStrip() {
    val context = LocalContext.current
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(
            Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.permission_needed),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
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
    kind: SoundKind,
    state: UiState,
    playing: String?,
    progress: () -> Float,
    volume: VolumeUi?,
    onVolume: (Int) -> Unit,
    onUse: (SoundKind, Selection) -> Unit,
    onTogglePreview: (Sound) -> Unit,
    onImport: () -> Unit,
    online: OnlineUi,
    onPreviewOnline: (OnlineSound) -> Unit,
    onUseOnline: (OnlineSound) -> Unit,
    onRetryOnline: () -> Unit,
) {
    val current = state.current[kind]
    val currentSound = (current as? Selection.Chosen)?.sound
    val catalog = state.catalog[kind] ?: SoundCatalog(emptyList(), emptyList())
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp)) {
        item(key = "now") {
            NowPanel(
                kind = kind,
                current = current,
                playing = currentSound != null && playing == currentSound.id,
                progress = progress,
                volume = volume,
                onVolume = onVolume,
                onTogglePreview = { currentSound?.let(onTogglePreview) },
            )
        }
        item(key = "import") {
            OutlinedButton(
                onClick = onImport,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 8.dp),
            ) { Text(stringResource(R.string.import_audio)) }
        }
        if (catalog.all.isEmpty()) {
            item(key = "empty") {
                Text(
                    stringResource(R.string.no_sounds),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                )
            }
        }
        if (catalog.yours.isNotEmpty()) {
            item(key = "yours-header") { SectionLabel(stringResource(R.string.your_audio)) }
            soundRows(catalog.yours, "yours", kind, currentSound, playing, progress, onUse, onTogglePreview)
            item(key = "phone-header") { SectionLabel(stringResource(R.string.phone_sounds)) }
        }
        soundRows(catalog.system, "phone", kind, currentSound, playing, progress, onUse, onTogglePreview)
        if (online.visible) {
            item(key = "online-header") { SectionLabel(stringResource(R.string.online_catalog), stringResource(R.string.online_catalog_help)) }
            when (val s = online.state) {
                OnlineState.Idle, OnlineState.Loading -> item(key = "online-loading") { StatusLine(stringResource(R.string.online_loading)) }
                OnlineState.Failed -> item(key = "online-failed") {
                    Column {
                        StatusLine(stringResource(R.string.online_failed))
                        TextButton(onClick = onRetryOnline, modifier = Modifier.padding(start = 12.dp)) { Text(stringResource(R.string.try_again)) }
                    }
                }
                is OnlineState.Loaded -> items(s.sounds.forKind(kind), key = { "online-${it.id}" }) { sound ->
                    OnlineRow(
                        sound = sound,
                        added = sound.isAddedTo(catalog.yours),
                        playing = playing == "online:${sound.id}",
                        downloading = online.downloading == sound.id,
                        busy = online.downloading != null,
                        progress = progress,
                        onTogglePreview = { onPreviewOnline(sound) },
                        onUse = { onUseOnline(sound) },
                    )
                }
            }
        }
    }
}

private data class OnlineUi(val visible: Boolean, val state: OnlineState, val downloading: String?)

@Composable
private fun StatusLine(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
    )
}

@Composable
private fun OnlineRow(
    sound: OnlineSound,
    added: Boolean,
    playing: Boolean,
    downloading: Boolean,
    busy: Boolean,
    progress: () -> Float,
    onTogglePreview: () -> Unit,
    onUse: () -> Unit,
) {
    Column {
        Row(
            Modifier.fillMaxWidth().defaultMinSize(minHeight = 72.dp).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (downloading) {
                Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 3.dp)
                }
            } else {
                PlayStopButton(
                    playing = playing,
                    onClick = onTogglePreview,
                    onClickLabel = stringResource(if (playing) R.string.stop else R.string.preview),
                )
            }
            Column(Modifier.weight(1f)) {
                Text(sound.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "${sound.artist}, ${formatClock(sound.seconds)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Waveform(
                    seed = "online:${sound.id}",
                    lit = false,
                    progress = if (playing) progress else null,
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp).height(18.dp),
                )
            }
            if (added) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CheckMark(MaterialTheme.colorScheme.primary)
                    Text(stringResource(R.string.added), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                }
            } else {
                FilledTonalButton(onClick = onUse, enabled = !busy) { Text(stringResource(R.string.use)) }
            }
        }
        HorizontalDivider(Modifier.padding(start = 72.dp), color = MaterialTheme.colorScheme.outlineVariant)
    }
}

private fun formatClock(seconds: Double): String {
    val total = seconds.toInt()
    return "%d:%02d".format(total / 60, total % 60)
}

private fun LazyListScope.soundRows(
    sounds: List<Sound>,
    keyPrefix: String,
    kind: SoundKind,
    currentSound: Sound?,
    playing: String?,
    progress: () -> Float,
    onUse: (SoundKind, Selection) -> Unit,
    onTogglePreview: (Sound) -> Unit,
) {
    items(sounds, key = { "$keyPrefix-${it.id}" }) { sound ->
        SoundRow(
            sound = sound,
            active = currentSound == sound,
            playing = playing == sound.id,
            progress = progress,
            onTogglePreview = { onTogglePreview(sound) },
            onUse = { onUse(kind, Selection.Chosen(sound)) },
        )
    }
}

@Composable
private fun SectionLabel(text: String, hint: String? = null) {
    Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 4.dp)) {
        Text(text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (hint != null) {
            Text(hint, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun NowPanel(
    kind: SoundKind,
    current: Selection?,
    playing: Boolean,
    progress: () -> Float,
    volume: VolumeUi?,
    onVolume: (Int) -> Unit,
    onTogglePreview: () -> Unit,
) {
    val sound = (current as? Selection.Chosen)?.sound
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Column(Modifier.padding(horizontal = 20.dp).padding(top = 20.dp, bottom = 8.dp)) {
            Text(
                stringResource(kind.currentLabel()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                sound?.name ?: stringResource(R.string.silent),
                style = MaterialTheme.typography.headlineMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp),
            )
            Waveform(
                seed = sound?.id ?: "silencio",
                lit = sound != null,
                progress = if (playing) progress else null,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp).height(56.dp),
            )
            if (volume != null) {
                VolumeControl(kind, volume, onVolume, Modifier.padding(top = 16.dp))
            }
            if (sound != null) {
                Box(Modifier.padding(top = 12.dp)) {
                    PlayStopButton(
                        playing = playing,
                        onClick = onTogglePreview,
                        onClickLabel = stringResource(if (playing) R.string.stop else R.string.preview),
                    )
                }
            }
        }
    }
}

@Composable
private fun SoundRow(
    sound: Sound,
    active: Boolean,
    playing: Boolean,
    progress: () -> Float,
    onTogglePreview: () -> Unit,
    onUse: () -> Unit,
) {
    Column {
        Row(
            Modifier.fillMaxWidth().defaultMinSize(minHeight = 72.dp).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PlayStopButton(
                playing = playing,
                onClick = onTogglePreview,
                onClickLabel = stringResource(if (playing) R.string.stop else R.string.preview),
            )
            Column(Modifier.weight(1f)) {
                Text(
                    sound.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Waveform(
                    seed = sound.id,
                    lit = active,
                    progress = if (playing) progress else null,
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp).height(18.dp),
                )
            }
            if (active) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CheckMark(MaterialTheme.colorScheme.primary)
                    Text(
                        stringResource(R.string.active),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            } else {
                FilledTonalButton(onClick = onUse) { Text(stringResource(R.string.use)) }
            }
        }
        HorizontalDivider(Modifier.padding(start = 72.dp), color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun ProfilesTab(
    profiles: List<Profile>,
    onSave: (String) -> Unit,
    onApply: (String) -> Unit,
    onDelete: (String) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var toDelete by remember { mutableStateOf<Profile?>(null) }
    val own = profiles.filter { it.id != QuietRingtones.ORIGINAL_ID }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp)) {
        item(key = "form") {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.profile_name_hint)) },
                    supportingText = { Text(stringResource(R.string.profile_name_help)) },
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
            }
        }
        items(profiles, key = { it.id }) { profile ->
            ProfileRow(
                profile = profile,
                onApply = { onApply(profile.id) },
                onDelete = { toDelete = profile },
            )
        }
        if (own.isEmpty()) {
            item(key = "empty") {
                Text(
                    stringResource(R.string.no_profiles),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                )
            }
        }
        item(key = "tile") { TileHint() }
    }

    toDelete?.let { profile ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text(stringResource(R.string.delete_title, profile.name)) },
            text = { Text(stringResource(R.string.delete_body)) },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(profile.id)
                    toDelete = null
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { toDelete = null }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@Composable
private fun TileHint() {
    val context = LocalContext.current
    Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.tile_title), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(R.string.tile_help),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            OutlinedButton(onClick = {
                val manager = context.getSystemService(StatusBarManager::class.java)
                manager?.requestAddTileService(
                    ComponentName(context, ProfileTileService::class.java),
                    context.getString(R.string.tile_label),
                    Icon.createWithResource(context, R.drawable.ic_tile),
                    context.mainExecutor,
                ) { }
            }) { Text(stringResource(R.string.tile_add)) }
        }
    }
}

@Composable
private fun ProfileRow(profile: Profile, onApply: () -> Unit, onDelete: () -> Unit) {
    val isOriginal = profile.id == QuietRingtones.ORIGINAL_ID
    Column {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        if (isOriginal) stringResource(R.string.profile_original) else profile.name,
                        style = MaterialTheme.typography.titleLarge,
                    )
                    if (isOriginal) {
                        Text(
                            stringResource(R.string.original_help),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (!isOriginal) {
                    TextButton(onClick = onDelete) { Text(stringResource(R.string.delete)) }
                }
                FilledTonalButton(onClick = onApply) { Text(stringResource(R.string.apply)) }
            }
            Column(Modifier.padding(top = 8.dp)) {
                SoundKind.entries.forEach { kind ->
                    Row(Modifier.padding(vertical = 2.dp)) {
                        Text(
                            stringResource(kind.label()),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(128.dp),
                        )
                        Text(
                            profile.selections[kind].displayName(),
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

private fun SoundKind.tabLabel() = when (this) {
    SoundKind.RINGTONE -> R.string.tab_calls
    SoundKind.NOTIFICATION -> R.string.tab_notifications
    SoundKind.ALARM -> R.string.tab_alarms
}

private fun SoundKind.label() = when (this) {
    SoundKind.RINGTONE -> R.string.kind_ringtone
    SoundKind.NOTIFICATION -> R.string.kind_notification
    SoundKind.ALARM -> R.string.kind_alarm
}

private fun SoundKind.currentLabel() = when (this) {
    SoundKind.RINGTONE -> R.string.current_ringtone
    SoundKind.NOTIFICATION -> R.string.current_notification
    SoundKind.ALARM -> R.string.current_alarm
}

@Composable
private fun Selection?.displayName(): String = when (this) {
    is Selection.Chosen -> sound.name
    else -> stringResource(R.string.silent)
}

private fun displayName(resolver: ContentResolver, uri: Uri): String =
    resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) cursor.getString(0) else null
    } ?: uri.lastPathSegment ?: "audio"
