package com.quietringtones.share

import com.quietringtones.core.BundleEntry
import com.quietringtones.core.ProfileBundle
import com.quietringtones.core.SoundKind
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import org.json.JSONObject

enum class ProfileFileProblem { NOT_A_PROFILE, NEWER_FORMAT, TOO_LARGE }

/** [problem] decide qué se le dice al usuario: un archivo ajeno, uno de una versión más nueva o uno con un audio demasiado grande. */
class ProfileFileException(val problem: ProfileFileProblem, message: String) : Exception(message)

/**
 * Formato del archivo de perfil (.qrprofile): un zip con «profile.json» primero y un audio por cada tipo de sonido.
 * Al leerlo no se confía en nada: los nombres de archivo se validan y los tamaños tienen tope.
 */
object ProfileFile {
    const val EXTENSION = "qrprofile"
    const val MANIFEST = "profile.json"
    const val MAX_MANIFEST_BYTES = 64 * 1024
    const val MAX_AUDIO_BYTES = 50L * 1024 * 1024
    private const val FORMAT = 1
    private val AUDIO_NAME = Regex("^(ringtone|notification|alarm)\\.(mp3|m4a|aac|ogg|wav|flac)$")

    fun audioName(kind: SoundKind, extension: String) = "${kind.name.lowercase()}.$extension"

    fun manifest(bundle: ProfileBundle): String {
        val sounds = JSONObject()
        bundle.entries.forEach { (kind, entry) ->
            sounds.put(
                kind.name,
                when (entry) {
                    BundleEntry.Silent -> JSONObject().put("silent", true)
                    is BundleEntry.Audio -> JSONObject().put("title", entry.title).put("file", entry.file)
                },
            )
        }
        return JSONObject().put("app", "QuietRingtones").put("format", FORMAT).put("name", bundle.name).put("sounds", sounds).toString()
    }

    fun parse(json: String): ProfileBundle {
        val root = try {
            JSONObject(json)
        } catch (e: Exception) {
            throw ProfileFileException(ProfileFileProblem.NOT_A_PROFILE, "Not a profile")
        }
        val format = root.optInt("format", -1)
        if (format > FORMAT) throw ProfileFileException(ProfileFileProblem.NEWER_FORMAT, "Made by a newer version")
        if (format != FORMAT) throw ProfileFileException(ProfileFileProblem.NOT_A_PROFILE, "Unsupported format")
        val sounds = root.optJSONObject("sounds") ?: throw ProfileFileException(ProfileFileProblem.NOT_A_PROFILE, "No sounds")
        val entries = mutableMapOf<SoundKind, BundleEntry>()
        SoundKind.entries.forEach { kind ->
            val item = sounds.optJSONObject(kind.name) ?: return@forEach
            entries[kind] = when {
                item.optBoolean("silent", false) -> BundleEntry.Silent
                else -> {
                    val file = item.optString("file")
                    if (!AUDIO_NAME.matches(file) || !file.startsWith(kind.name.lowercase())) throw ProfileFileException(ProfileFileProblem.NOT_A_PROFILE, "Bad file name")
                    BundleEntry.Audio(item.optString("title"), file)
                }
            }
        }
        if (entries.isEmpty()) throw ProfileFileException(ProfileFileProblem.NOT_A_PROFILE, "No sounds")
        return ProfileBundle(root.optString("name"), entries)
    }

    /** El manifiesto va primero; después, el audio de cada entrada que lo tiene. */
    fun write(out: OutputStream, bundle: ProfileBundle, open: (BundleEntry.Audio) -> InputStream) {
        ZipOutputStream(out).use { zip ->
            zip.putNextEntry(ZipEntry(MANIFEST))
            zip.write(manifest(bundle).toByteArray(Charsets.UTF_8))
            zip.closeEntry()
            bundle.entries.values.filterIsInstance<BundleEntry.Audio>().forEach { audio ->
                zip.putNextEntry(ZipEntry(audio.file))
                open(audio).use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
    }

    /** Lee el perfil y deja sus audios en [into] (que debe estar vacía). Devuelve cada audio por su nombre en el archivo. */
    fun read(input: InputStream, into: File): Pair<ProfileBundle, Map<String, File>> {
        into.mkdirs()
        ZipInputStream(input).use { zip ->
            val first = zip.nextEntry
            if (first == null || first.name != MANIFEST) throw ProfileFileException(ProfileFileProblem.NOT_A_PROFILE, "Manifest must come first")
            val bundle = parse(String(readLimited(zip, MAX_MANIFEST_BYTES.toLong()), Charsets.UTF_8))
            val wanted = bundle.entries.values.filterIsInstance<BundleEntry.Audio>().map { it.file }.toSet()
            val files = mutableMapOf<String, File>()
            while (true) {
                val entry = zip.nextEntry ?: break
                if (entry.name !in wanted || entry.name in files) continue
                val target = File(into, entry.name)
                target.outputStream().use { out -> copyLimited(zip, out, MAX_AUDIO_BYTES) }
                files[entry.name] = target
            }
            if (files.keys != wanted) throw ProfileFileException(ProfileFileProblem.NOT_A_PROFILE, "Audio missing")
            return bundle to files
        }
    }

    private fun readLimited(input: InputStream, limit: Long): ByteArray {
        val buffer = java.io.ByteArrayOutputStream()
        copyLimited(input, buffer, limit)
        return buffer.toByteArray()
    }

    private fun copyLimited(input: InputStream, out: OutputStream, limit: Long) {
        val chunk = ByteArray(16 * 1024)
        var total = 0L
        while (true) {
            val n = input.read(chunk)
            if (n < 0) return
            total += n
            if (total > limit) throw ProfileFileException(ProfileFileProblem.TOO_LARGE, "Too large")
            out.write(chunk, 0, n)
        }
    }
}
