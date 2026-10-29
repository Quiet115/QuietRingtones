package com.quietringtones

import android.app.Application
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.ConnectivityManager
import android.net.Uri
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.quietringtones.core.ApplyResult
import com.quietringtones.core.Outcome
import com.quietringtones.core.Profile
import com.quietringtones.core.ProfileResult
import com.quietringtones.core.QuietRingtones
import com.quietringtones.core.OnlineSound
import com.quietringtones.core.Release
import com.quietringtones.core.UpdateCheck
import com.quietringtones.core.Updater
import com.quietringtones.contacts.ContactPrefs
import com.quietringtones.contacts.ContactSound
import com.quietringtones.contacts.ContactSounds
import com.quietringtones.contacts.PendingContact
import com.quietringtones.core.TrimSelection
import com.quietringtones.core.Version
import com.quietringtones.core.VolumeAdvice
import com.quietringtones.core.VolumeAdvisor
import com.quietringtones.ui.messageRes
import com.quietringtones.online.CatalogPrefs
import com.quietringtones.trim.AudioTrimmer
import com.quietringtones.online.OnlineCatalogSource
import com.quietringtones.online.PreviewCache
import com.quietringtones.updates.GitHubReleases
import com.quietringtones.updates.UpdateDownloader
import com.quietringtones.updates.UpdatePrefs
import java.io.File
import java.io.IOException
import com.quietringtones.core.Selection
import com.quietringtones.core.Sound
import com.quietringtones.core.SoundCatalog
import com.quietringtones.core.SoundKind
import com.quietringtones.ui.Appearance
import com.quietringtones.ui.AppearanceStore
import com.quietringtones.ui.Language
import com.quietringtones.ui.LanguageStore
import com.quietringtones.ui.playbackFraction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class Message(@StringRes val res: Int, val arg: String? = null, val arg2: String? = null)

data class UiState(
    val canModify: Boolean = false,
    val current: Map<SoundKind, Selection> = emptyMap(),
    val catalog: Map<SoundKind, SoundCatalog> = emptyMap(),
    val profiles: List<Profile> = emptyList(),
    val message: Message? = null,
)

private const val TRIM_KEY = "trim"
private const val DAY_MS = 24L * 60 * 60 * 1000

sealed interface UpdateStatus {
    data object Idle : UpdateStatus
    data object Checking : UpdateStatus
    data object UpToDate : UpdateStatus
    data object Failed : UpdateStatus
    data class Available(val release: Release) : UpdateStatus
    data class Downloading(val release: Release, val percent: Int) : UpdateStatus
    data class Ready(val release: Release, val apk: File) : UpdateStatus
    data class DownloadFailed(val release: Release) : UpdateStatus
}

data class UpdateUi(
    val status: UpdateStatus = UpdateStatus.Idle,
    val automatic: Boolean = true,
    val lastChecked: Long = 0L,
    val installedVersion: String = "",
)

sealed interface OnlineState {
    data object Idle : OnlineState
    data object Loading : OnlineState
    data class Loaded(val sounds: List<OnlineSound>) : OnlineState
    data object Failed : OnlineState
}

data class ContactsUi(
    val enabled: Boolean = false,
    val permitted: Boolean = false,
    val assigned: List<ContactSound> = emptyList(),
)

data class TrimUi(val kind: SoundKind, val uri: Uri, val displayName: String, val selection: TrimSelection, val busy: Boolean)

data class VolumeUi(val index: Int, val min: Int, val max: Int, val advice: VolumeAdvice)

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val app = QuietRingtones(AndroidSoundSystem(application), PrefsStore(application))
    private var player: MediaPlayer? = null
    private var watchJob: Job? = null

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    private val appearanceStore = AppearanceStore(application)
    private val _appearance = MutableStateFlow(appearanceStore.load())
    val appearance: StateFlow<Appearance> = _appearance

    fun setAppearance(appearance: Appearance) {
        _appearance.value = appearance
        appearanceStore.save(appearance)
    }

    // --- Volumen ------------------------------------------------------------------

    private val volumes = AndroidVolumes(application)
    private var volumeJob: Job? = null
    private val _volumeState = MutableStateFlow<Map<SoundKind, VolumeUi>>(emptyMap())
    val volume: StateFlow<Map<SoundKind, VolumeUi>> = _volumeState

    private fun readVolumes(): Map<SoundKind, VolumeUi> {
        val ringer = volumes.ringerMode()
        val dnd = volumes.doNotDisturb()
        return SoundKind.entries.associateWith { kind ->
            val v = volumes.volume(kind)
            VolumeUi(v.index, volumes.minimum(kind), v.max, VolumeAdvisor.advise(kind, v, ringer, dnd))
        }
    }

    /** Mientras la app está visible, vigila el volumen: también cambia con los botones del teléfono. */
    fun startVolumeWatch() {
        if (volumeJob != null) return
        volumeJob = viewModelScope.launch {
            while (true) {
                _volumeState.value = readVolumes()
                delay(400)
            }
        }
    }

    fun stopVolumeWatch() {
        volumeJob?.cancel()
        volumeJob = null
    }

    fun setVolume(kind: SoundKind, index: Int) {
        try {
            volumes.set(kind, index)
        } catch (e: SecurityException) {
            _state.update { it.copy(message = Message(R.string.msg_volume_blocked)) }
        }
        _volumeState.value = readVolumes()
    }

    // --- Sonido por contacto -------------------------------------------------------

    private val contactPrefs = ContactPrefs(application)
    private val contactSounds = ContactSounds(application)

    private val _contacts = MutableStateFlow(ContactsUi())
    val contacts: StateFlow<ContactsUi> = _contacts

    private val _pendingContact = MutableStateFlow<PendingContact?>(null)
    val pendingContact: StateFlow<PendingContact?> = _pendingContact

    fun refreshContacts() = launchIo {
        val permitted = contactSounds.hasPermission()
        val enabled = contactPrefs.enabled && permitted
        val unknown = getApplication<Application>().getString(R.string.contact_unknown_sound)
        _contacts.value = ContactsUi(
            enabled = contactPrefs.enabled,
            permitted = permitted,
            assigned = if (enabled) contactSounds.assigned(unknown) else emptyList(),
        )
    }

    fun setContactsEnabled(enabled: Boolean) {
        contactPrefs.enabled = enabled
        refreshContacts()
    }

    /** Tras la pantalla de permisos: sin ellos la función se queda desactivada. */
    fun onContactsPermissionResult(granted: Boolean) {
        if (granted) {
            contactPrefs.enabled = true
        } else {
            contactPrefs.enabled = false
            _state.update { it.copy(message = Message(R.string.msg_contacts_denied)) }
        }
        refreshContacts()
    }

    fun chooseContact(uri: Uri) = launchIo {
        val name = contactSounds.nameOf(uri) ?: return@launchIo
        _pendingContact.value = PendingContact(uri, name)
    }

    fun cancelPendingContact() {
        stopPreview()
        _pendingContact.value = null
    }

    fun assignToPending(sound: Sound) {
        val pending = _pendingContact.value ?: return
        stopPreview()
        _pendingContact.value = null
        launchIo {
            try {
                contactSounds.assign(pending.uri, Uri.parse(sound.id))
                _state.update { it.copy(message = Message(R.string.msg_contact_set, pending.name, sound.name)) }
            } catch (e: SecurityException) {
                _state.update { it.copy(message = Message(R.string.msg_contacts_denied)) }
            }
            refreshContacts()
        }
    }

    fun removeContactSound(id: Long, name: String) = launchIo {
        contactSounds.remove(id)
        _state.update { it.copy(message = Message(R.string.msg_contact_removed, name)) }
        refreshContacts()
    }

    // --- Recorte -------------------------------------------------------------------

    private val trimmer = AudioTrimmer(application)
    private val _trim = MutableStateFlow<TrimUi?>(null)
    val trim: StateFlow<TrimUi?> = _trim

    /** Tras elegir un audio se abre el recorte; si no se puede leer su duración, se importa entero. */
    fun beginTrim(kind: SoundKind, uri: Uri, displayName: String) {
        viewModelScope.launch {
            val duration = runCatching { withContext(Dispatchers.IO) { trimmer.durationMs(uri) } }.getOrDefault(0L)
            if (duration <= 0L) {
                importAndUse(kind, uri, displayName)
            } else {
                _trim.value = TrimUi(kind, uri, displayName, TrimSelection.initial(duration), busy = false)
            }
        }
    }

    fun setTrimStart(ms: Long) = updateTrim { it.copy(selection = it.selection.withStart(ms)) }
    fun setTrimLength(ms: Long) = updateTrim { it.copy(selection = it.selection.withLength(ms)) }
    fun trimWholeFile() = updateTrim { it.copy(selection = it.selection.whole()) }

    private fun updateTrim(change: (TrimUi) -> TrimUi) {
        val current = _trim.value ?: return
        if (current.busy) return
        stopPreview()
        _trim.value = change(current)
    }

    /** Escucha solo la parte elegida; otro toque la detiene. */
    fun previewTrim() {
        val trim = _trim.value ?: return
        if (_playing.value == TRIM_KEY) {
            stopPreview()
            return
        }
        stopPreview()
        _playing.value = TRIM_KEY
        startPreview(trim.kind, TRIM_KEY, trim.uri, trim.selection.startMs..trim.selection.endMs)
    }

    fun cancelTrim() {
        if (_trim.value?.busy == true) return
        stopPreview()
        _trim.value = null
    }

    fun confirmTrim() {
        val trim = _trim.value ?: return
        if (trim.busy) return
        stopPreview()
        _trim.value = trim.copy(busy = true)
        launchIo {
            try {
                if (trim.selection.isWholeFile) {
                    importNow(trim.kind, trim.uri, trim.displayName)
                } else {
                    val file = trimmer.trim(trim.uri, trim.selection.startMs, trim.selection.lengthMs, File(getApplication<Application>().cacheDir, "trim"))
                    val base = trim.displayName.substringBeforeLast('.')
                    val range = "${trim.selection.startMs / 1000}s-${trim.selection.endMs / 1000}s"
                    importNow(trim.kind, Uri.fromFile(file), "$base ($range).${file.extension}")
                    file.delete()
                }
            } finally {
                _trim.value = null
            }
        }
    }

    // --- Catálogo online ------------------------------------------------------------

    private val catalogPrefs = CatalogPrefs(application)
    private val previewCache = PreviewCache(File(application.cacheDir, "previews"))
    private val onlineSource = OnlineCatalogSource(File(application.cacheDir, "catalog.json"), "QuietRingtones/${application.packageName}")
    private val userAgent = "QuietRingtones"

    private val _online = MutableStateFlow<OnlineState>(OnlineState.Idle)
    val online: StateFlow<OnlineState> = _online

    private val _showOnline = MutableStateFlow(catalogPrefs.showOnline)
    val showOnline: StateFlow<Boolean> = _showOnline

    /** Identificador del sonido online que se está descargando, si hay alguno. */
    private val _downloading = MutableStateFlow<String?>(null)
    val downloading: StateFlow<String?> = _downloading

    private val _cacheSize = MutableStateFlow(previewCache.sizeBytes())
    val cacheSize: StateFlow<Long> = _cacheSize

    /** Carga el catálogo la primera vez que se necesita; sin conexión usa la última copia guardada. */
    fun loadOnline() {
        if (!_showOnline.value) return
        if (_online.value is OnlineState.Loading || _online.value is OnlineState.Loaded) return
        _online.value = OnlineState.Loading
        viewModelScope.launch {
            val result = runCatching { withContext(Dispatchers.IO) { onlineSource.load() } }
            _online.value = result.fold({ OnlineState.Loaded(it) }, { OnlineState.Failed })
        }
    }

    fun retryOnline() {
        _online.value = OnlineState.Idle
        loadOnline()
    }

    fun setShowOnline(show: Boolean) {
        catalogPrefs.showOnline = show
        _showOnline.value = show
        if (show) loadOnline() else stopPreview()
    }

    /** Un toque descarga el clip a la caché (si hace falta) y lo escucha; otro toque lo detiene. */
    fun previewOnline(kind: SoundKind, sound: OnlineSound) {
        val key = "online:${sound.id}"
        if (_playing.value == key) {
            stopPreview()
            return
        }
        if (_downloading.value != null) return
        stopPreview()
        previewCache.cached(sound)?.let { startPreview(kind, key, Uri.fromFile(it)); return }
        _downloading.value = sound.id
        viewModelScope.launch {
            val result = runCatching { withContext(Dispatchers.IO) { previewCache.download(sound, userAgent) } }
            _downloading.value = null
            _cacheSize.value = previewCache.sizeBytes()
            result.fold(
                { startPreview(kind, key, Uri.fromFile(it)) },
                { _state.update { s -> s.copy(message = Message(R.string.msg_download_failed)) } },
            )
        }
    }

    /** Usar guarda el sonido de verdad: queda entre tus audios y se aplica. */
    fun useOnline(kind: SoundKind, sound: OnlineSound) {
        if (_downloading.value != null) return
        _downloading.value = sound.id
        viewModelScope.launch {
            val message = try {
                withContext(Dispatchers.IO) {
                    val file = previewCache.download(sound, userAgent)
                    val saved = app.importSound(Uri.fromFile(file).toString(), "${sound.title}.ogg", kind)
                    when (app.apply(kind, Selection.Chosen(saved))) {
                        ApplyResult.Applied -> Message(R.string.msg_added_online, sound.title)
                        ApplyResult.NeedsPermission -> Message(R.string.msg_added_only, sound.title)
                    }
                }
            } catch (e: IOException) {
                Message(R.string.msg_download_failed)
            } catch (e: Exception) {
                Message(R.string.msg_error, e.message ?: e.javaClass.simpleName)
            }
            _downloading.value = null
            _cacheSize.value = previewCache.sizeBytes()
            withContext(Dispatchers.IO) { reload(message) }
        }
    }

    fun clearPreviewCache() {
        stopPreview()
        previewCache.clear()
        _cacheSize.value = 0
        _state.update { it.copy(message = Message(R.string.msg_cache_cleared)) }
    }

    // --- Actualizaciones --------------------------------------------------------

    private val updatePrefs = UpdatePrefs(application)
    private val installed: Version = runCatching {
        Version.parse(application.packageManager.getPackageInfo(application.packageName, 0).versionName.orEmpty())
    }.getOrNull() ?: Version(listOf(0))
    private val updater = Updater(GitHubReleases("QuietRingtones/$installed"), installed)
    private var autoChecked = false

    private val _updates = MutableStateFlow(
        UpdateUi(
            automatic = updatePrefs.automatic,
            lastChecked = updatePrefs.lastChecked,
            installedVersion = installed.toString(),
        ),
    )
    val updates: StateFlow<UpdateUi> = _updates

    private fun setUpdate(change: (UpdateUi) -> UpdateUi) = _updates.update(change)

    /** Al abrir la app, como mucho una vez al día y solo si las automáticas están activadas. */
    fun autoCheckOnStart() {
        if (autoChecked) return
        autoChecked = true
        if (!updatePrefs.automatic) return
        if (System.currentTimeMillis() - updatePrefs.lastChecked < DAY_MS) return
        runCheck(manual = false)
    }

    fun checkNow() = runCheck(manual = true)

    fun setAutomaticUpdates(enabled: Boolean) {
        updatePrefs.automatic = enabled
        setUpdate { it.copy(automatic = enabled) }
    }

    fun skip(release: Release) {
        updatePrefs.skippedVersion = release.version.toString()
        setUpdate { it.copy(status = UpdateStatus.UpToDate) }
    }

    fun download(release: Release) {
        val current = _updates.value.status
        if (current is UpdateStatus.Checking || current is UpdateStatus.Downloading) return
        setUpdate { it.copy(status = UpdateStatus.Downloading(release, 0)) }
        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    UpdateDownloader(getApplication()).download(release) { percent ->
                        setUpdate { it.copy(status = UpdateStatus.Downloading(release, percent)) }
                    }
                }
            }
            setUpdate {
                it.copy(
                    status = result.fold(
                        onSuccess = { file -> UpdateStatus.Ready(release, file) },
                        onFailure = { UpdateStatus.DownloadFailed(release) },
                    ),
                )
            }
        }
    }

    private fun runCheck(manual: Boolean) {
        val current = _updates.value.status
        if (current is UpdateStatus.Checking || current is UpdateStatus.Downloading) return
        setUpdate { it.copy(status = UpdateStatus.Checking) }
        viewModelScope.launch {
            val skipped = if (manual) null else updatePrefs.skippedVersion?.let(Version::parse)
            val result = withContext(Dispatchers.IO) { updater.check(skipped) }
            val now = System.currentTimeMillis()
            updatePrefs.lastChecked = now
            setUpdate { it.copy(lastChecked = now) }
            when (result) {
                UpdateCheck.UpToDate -> setUpdate { it.copy(status = UpdateStatus.UpToDate) }
                is UpdateCheck.Failed -> setUpdate { it.copy(status = UpdateStatus.Failed) }
                is UpdateCheck.Available -> {
                    val release = result.release
                    if (!manual && updatePrefs.automatic && isUnmetered()) {
                        setUpdate { it.copy(status = UpdateStatus.Available(release)) }
                        download(release)
                    } else {
                        setUpdate { it.copy(status = UpdateStatus.Available(release)) }
                    }
                    if (!manual) {
                        _state.update {
                            it.copy(message = Message(R.string.msg_update_available, release.version.toString()))
                        }
                    }
                }
            }
        }
    }

    private fun isUnmetered(): Boolean {
        val manager = getApplication<Application>().getSystemService(ConnectivityManager::class.java)
        return manager != null && !manager.isActiveNetworkMetered
    }

    private val languageStore = LanguageStore(application)
    private val _language = MutableStateFlow(languageStore.load())
    val language: StateFlow<Language> = _language

    fun setLanguage(language: Language) {
        languageStore.save(language)
        _language.value = language
    }

    private val _playing = MutableStateFlow<String?>(null)

    /** Identificador del sonido que se está escuchando, si hay alguno. */
    val playing: StateFlow<String?> = _playing

    private val _progress = MutableStateFlow(0f)

    /** Posición real de la reproducción (0 a 1); -1 si suena pero no se conoce la duración. */
    val progress: StateFlow<Float> = _progress

    fun refresh() = launchIo { reload() }

    fun use(kind: SoundKind, selection: Selection) = launchIo {
        val name = (selection as? Selection.Chosen)?.sound?.name.orEmpty()
        report(app.apply(kind, selection)) { Message(R.string.msg_applied, name) }
    }

    fun importAndUse(kind: SoundKind, source: Uri, displayName: String) = launchIo {
        importNow(kind, source, displayName)
    }

    private fun importNow(kind: SoundKind, source: Uri, displayName: String) {
        val sound = app.importSound(source.toString(), displayName, kind)
        val message = when (app.apply(kind, Selection.Chosen(sound))) {
            ApplyResult.Applied -> Message(R.string.msg_imported, sound.name)
            ApplyResult.NeedsPermission -> Message(R.string.msg_imported_only, sound.name)
        }
        reload(message)
    }

    fun saveProfile(name: String) = launchIo {
        val profile = app.saveCurrentAsProfile(name)
        reload(Message(R.string.msg_profile_saved, profile.name))
    }

    fun applyProfile(id: String) = launchIo {
        val message = when (val result = app.applyProfile(id)) {
            ProfileResult.NeedsPermission -> Message(R.string.msg_needs_permission)
            ProfileResult.NotFound -> Message(R.string.msg_profile_missing)
            is ProfileResult.Done -> {
                val changed = result.outcomes.values.count { it != Outcome.APPLIED }
                if (changed == 0) Message(R.string.msg_profile_applied)
                else Message(R.string.msg_profile_fallback, changed.toString())
            }
        }
        reload(message)
    }

    fun deleteProfile(id: String) = launchIo {
        app.deleteProfile(id)
        reload()
    }

    /** Un toque escucha el sonido; otro toque sobre el mismo lo detiene. */
    fun togglePreview(kind: SoundKind, sound: Sound) {
        val wasPlaying = _playing.value == sound.id
        stopPreview()
        if (wasPlaying) return
        startPreview(kind, sound.id, Uri.parse(sound.id))
    }

    /** Reproduce [uri] con el canal de audio de [kind]; [key] identifica qué fila está sonando. */
    private fun startPreview(kind: SoundKind, key: String, uri: Uri, range: LongRange? = null) {
        val advice = readVolumes().getValue(kind).advice
        if (advice != VolumeAdvice.OK && advice != VolumeAdvice.LOW) {
            _state.update { it.copy(message = Message(advice.messageRes())) }
        }
        val usage = when (kind) {
            SoundKind.RINGTONE -> AudioAttributes.USAGE_NOTIFICATION_RINGTONE
            SoundKind.NOTIFICATION -> AudioAttributes.USAGE_NOTIFICATION
            SoundKind.ALARM -> AudioAttributes.USAGE_ALARM
        }
        val next = MediaPlayer()
        player = next
        _playing.value = key
        _progress.value = 0f
        try {
            next.setAudioAttributes(
                AudioAttributes.Builder().setUsage(usage).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build(),
            )
            next.setDataSource(getApplication<Application>(), uri)
            next.setOnCompletionListener { if (player === next) stopPreview() }
            next.setOnErrorListener { _, _, _ ->
                if (player === next) stopPreview()
                true
            }
            next.setOnPreparedListener {
                if (player !== next) return@setOnPreparedListener
                if (range != null) it.seekTo(range.first, MediaPlayer.SEEK_CLOSEST)
                it.start()
                trackProgress(next, range)
            }
            next.prepareAsync()
        } catch (e: Exception) {
            stopPreview()
        }
    }

    /** Sigue la posición real del reproductor, unas 30 veces por segundo, mientras suene. */
    private fun trackProgress(active: MediaPlayer, range: LongRange? = null) {
        watchJob?.cancel()
        watchJob = viewModelScope.launch {
            while (player === active) {
                val duration = runCatching { active.duration }.getOrDefault(-1)
                val position = runCatching { active.currentPosition }.getOrDefault(0)
                if (range == null) {
                    _progress.value = playbackFraction(position, duration)
                } else {
                    // Solo suena la parte elegida: se para al llegar a su final.
                    if (position >= range.last) { stopPreview(); break }
                    _progress.value = playbackFraction((position - range.first).toInt().coerceAtLeast(0), (range.last - range.first).toInt())
                }
                delay(33)
            }
        }
    }

    fun stopPreview() {
        watchJob?.cancel()
        watchJob = null
        player?.let { current ->
            player = null
            runCatching { current.stop() }
            current.release()
        }
        _playing.value = null
        _progress.value = 0f
    }

    fun showError(error: Throwable) {
        _state.update { it.copy(message = Message(R.string.msg_error, error.message ?: error.javaClass.simpleName)) }
    }

    fun dismissMessage() = _state.update { it.copy(message = null) }

    override fun onCleared() = stopPreview()

    private fun report(result: ApplyResult, success: () -> Message) {
        val message = when (result) {
            ApplyResult.Applied -> success()
            ApplyResult.NeedsPermission -> Message(R.string.msg_needs_permission)
        }
        reload(message)
    }

    private fun reload(message: Message? = null) {
        app.ensureOriginal()
        val kinds = SoundKind.entries
        _state.value = UiState(
            canModify = app.canModify(),
            current = app.current(),
            catalog = kinds.associateWith { app.catalog(it) },
            profiles = app.profiles(),
            message = message,
        )
    }

    private fun launchIo(block: () -> Unit) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { block() }
            } catch (e: Exception) {
                showError(e)
            }
        }
    }
}
