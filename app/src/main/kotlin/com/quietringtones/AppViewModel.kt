package com.quietringtones

import android.app.Application
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.quietringtones.core.ApplyResult
import com.quietringtones.core.Outcome
import com.quietringtones.core.Profile
import com.quietringtones.core.ProfileResult
import com.quietringtones.core.QuietRingtones
import com.quietringtones.core.Selection
import com.quietringtones.core.Sound
import com.quietringtones.core.SoundKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class Message(@StringRes val res: Int, val arg: String? = null)

data class UiState(
    val canModify: Boolean = false,
    val current: Map<SoundKind, Selection> = emptyMap(),
    val available: Map<SoundKind, List<Sound>> = emptyMap(),
    val profiles: List<Profile> = emptyList(),
    val message: Message? = null,
)

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val app = QuietRingtones(AndroidSoundSystem(application), PrefsStore(application))
    private var ringtone: Ringtone? = null

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    fun refresh() = launchIo { reload() }

    fun use(kind: SoundKind, selection: Selection) = launchIo {
        report(app.apply(kind, selection)) { Message(R.string.msg_applied) }
    }

    fun importAndUse(kind: SoundKind, source: Uri, displayName: String) = launchIo {
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

    fun preview(sound: Sound) {
        stopPreview()
        ringtone = runCatching {
            RingtoneManager.getRingtone(getApplication(), Uri.parse(sound.id))?.also { it.play() }
        }.getOrNull()
    }

    fun stopPreview() {
        ringtone?.stop()
        ringtone = null
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
        val kinds = SoundKind.entries
        _state.value = UiState(
            canModify = app.canModify(),
            current = app.current(),
            available = kinds.associateWith { app.available(it) },
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
