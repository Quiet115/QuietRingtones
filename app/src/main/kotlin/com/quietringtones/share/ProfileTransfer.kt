package com.quietringtones.share

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.quietringtones.core.BundleEntry
import com.quietringtones.core.Profile
import com.quietringtones.core.ProfileBundle
import com.quietringtones.core.Selection
import com.quietringtones.core.SoundKind
import java.io.File

class ExportedProfile(val uri: Uri, val skipped: Int)

/** Pasa perfiles de un teléfono a otro como archivo: el audio de cada sonido viaja dentro, porque el otro teléfono no lo tiene. */
class ProfileTransfer(private val context: Context) {
    private val shareDir get() = File(context.cacheDir, "shared")
    private val importDir get() = File(context.cacheDir, "profile-import")

    /** Null si no se pudo leer ningún sonido del perfil. [skipped] cuenta los que el teléfono no dejó leer. */
    fun export(profile: Profile, displayName: String): ExportedProfile? {
        val resolver = context.contentResolver
        var skipped = 0
        val sources = mutableMapOf<String, Uri>()
        val entries = mutableMapOf<SoundKind, BundleEntry>()
        profile.selections.forEach { (kind, selection) ->
            when (selection) {
                Selection.Silent -> entries[kind] = BundleEntry.Silent
                is Selection.Chosen -> {
                    val uri = Uri.parse(selection.sound.id)
                    val readable = runCatching { resolver.openInputStream(uri)?.use { true } ?: false }.getOrDefault(false)
                    if (readable) {
                        val file = ProfileFile.audioName(kind, extensionOf(resolver.getType(uri)))
                        entries[kind] = BundleEntry.Audio(selection.sound.name, file)
                        sources[file] = uri
                    } else {
                        skipped++
                    }
                }
            }
        }
        if (entries.isEmpty()) return null

        shareDir.deleteRecursively()
        shareDir.mkdirs()
        val target = File(shareDir, "${fileStem(displayName)}.${ProfileFile.EXTENSION}")
        target.outputStream().use { out ->
            ProfileFile.write(out, ProfileBundle(displayName, entries)) { audio ->
                resolver.openInputStream(sources.getValue(audio.file)) ?: error("No se pudo leer el audio")
            }
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.updates", target)
        return ExportedProfile(uri, skipped)
    }

    /** Lee un archivo de perfil; sus audios quedan en la caché hasta [cleanUpImport]. Lanza [ProfileFileException] si no es válido. */
    fun read(source: Uri): Pair<ProfileBundle, Map<String, File>> {
        importDir.deleteRecursively()
        val input = context.contentResolver.openInputStream(source) ?: throw ProfileFileException("Cannot open")
        return input.use { ProfileFile.read(it, importDir) }
    }

    fun cleanUpImport() {
        importDir.deleteRecursively()
    }

    private fun extensionOf(mime: String?) = when (mime?.lowercase()) {
        "audio/mp4", "audio/x-m4a", "audio/m4a" -> "m4a"
        "audio/aac", "audio/aacp" -> "aac"
        "audio/ogg", "application/ogg", "audio/vorbis" -> "ogg"
        "audio/x-wav", "audio/wav", "audio/wave" -> "wav"
        "audio/flac", "audio/x-flac" -> "flac"
        else -> "mp3"
    }

    private fun fileStem(name: String) =
        name.filter { it.isLetterOrDigit() || it in " -_" }.trim().take(40).ifEmpty { "profile" }
}
