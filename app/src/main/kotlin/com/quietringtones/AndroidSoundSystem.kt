package com.quietringtones

import android.content.ContentValues
import android.content.Context
import android.media.RingtoneManager
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import com.quietringtones.core.Selection
import com.quietringtones.core.Sound
import com.quietringtones.core.SoundKind
import com.quietringtones.core.SoundSystem

class AndroidSoundSystem(private val context: Context) : SoundSystem {

    override fun canModify() = Settings.System.canWrite(context)

    override fun current(kind: SoundKind): Selection {
        val uri = RingtoneManager.getActualDefaultRingtoneUri(context, kind.ringtoneType())
            ?: return Selection.Silent
        return Selection.Chosen(Sound(uri.toString(), titleOf(uri)))
    }

    override fun available(kind: SoundKind): List<Sound> {
        val manager = RingtoneManager(context).apply { setType(kind.ringtoneType()) }
        val sounds = mutableListOf<Sound>()
        manager.cursor.use { cursor ->
            while (cursor.moveToNext()) {
                val title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX).orEmpty()
                val uri = manager.getRingtoneUri(cursor.position)
                sounds += Sound(uri.toString(), title.ifBlank { FALLBACK_NAME })
            }
        }
        return sounds
    }

    override fun apply(kind: SoundKind, selection: Selection) {
        val uri = (selection as? Selection.Chosen)?.sound?.id?.let(Uri::parse)
        RingtoneManager.setActualDefaultRingtoneUri(context, kind.ringtoneType(), uri)
    }

    override fun importFile(source: String, displayName: String, kind: SoundKind): Sound {
        val resolver = context.contentResolver
        val sourceUri = Uri.parse(source)
        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Audio.Media.TITLE, displayName.substringBeforeLast('.'))
            put(MediaStore.Audio.Media.MIME_TYPE, resolver.getType(sourceUri) ?: "audio/mpeg")
            put(MediaStore.Audio.Media.RELATIVE_PATH, kind.directory())
            put(MediaStore.Audio.Media.IS_RINGTONE, kind == SoundKind.RINGTONE)
            put(MediaStore.Audio.Media.IS_NOTIFICATION, kind == SoundKind.NOTIFICATION)
            put(MediaStore.Audio.Media.IS_ALARM, kind == SoundKind.ALARM)
            put(MediaStore.Audio.Media.IS_MUSIC, false)
            put(MediaStore.Audio.Media.IS_PENDING, 1)
        }
        val target = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
            ?: error("No se pudo crear el audio en el sistema")
        try {
            val input = resolver.openInputStream(sourceUri) ?: error("No se pudo leer el audio elegido")
            val output = resolver.openOutputStream(target) ?: error("No se pudo escribir el audio")
            input.use { i -> output.use { o -> i.copyTo(o) } }
            resolver.update(target, ContentValues().apply { put(MediaStore.Audio.Media.IS_PENDING, 0) }, null, null)
        } catch (e: Exception) {
            resolver.delete(target, null, null)
            throw e
        }
        return Sound(target.toString(), displayName.substringBeforeLast('.'))
    }

    /** El título puede fallar para URIs externos sin permiso de lectura: se usa un nombre de reserva. */
    private fun titleOf(uri: Uri): String =
        runCatching { RingtoneManager.getRingtone(context, uri)?.getTitle(context) }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?: FALLBACK_NAME

    private fun SoundKind.ringtoneType() = when (this) {
        SoundKind.RINGTONE -> RingtoneManager.TYPE_RINGTONE
        SoundKind.NOTIFICATION -> RingtoneManager.TYPE_NOTIFICATION
        SoundKind.ALARM -> RingtoneManager.TYPE_ALARM
    }

    private fun SoundKind.directory() = when (this) {
        SoundKind.RINGTONE -> Environment.DIRECTORY_RINGTONES
        SoundKind.NOTIFICATION -> Environment.DIRECTORY_NOTIFICATIONS
        SoundKind.ALARM -> Environment.DIRECTORY_ALARMS
    }

    private companion object {
        const val FALLBACK_NAME = "Sonido sin nombre"
    }
}
