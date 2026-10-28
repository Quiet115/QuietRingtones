package com.quietringtones.online

import com.quietringtones.core.OnlineSound
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/** Caché de vistas previas: clips descargados para escucharlos. Se puede vaciar sin perder nada que el usuario haya añadido. */
class PreviewCache(private val dir: File) {

    fun fileFor(sound: OnlineSound) = File(dir, "${sound.id}-${sound.sha256.take(8)}.ogg")

    fun cached(sound: OnlineSound): File? = fileFor(sound).takeIf { it.isFile }

    /** Guarda el clip solo si su SHA-256 es el del catálogo; si no, no deja nada a medias. */
    fun store(sound: OnlineSound, input: InputStream): File {
        dir.mkdirs()
        val partial = File(dir, "${sound.id}.part")
        val digest = MessageDigest.getInstance("SHA-256")
        partial.outputStream().use { output ->
            val buffer = ByteArray(16 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                output.write(buffer, 0, read)
                digest.update(buffer, 0, read)
            }
        }
        val actual = digest.digest().joinToString("") { "%02x".format(it) }
        if (actual != sound.sha256) {
            partial.delete()
            throw IOException("The downloaded sound is corrupt")
        }
        val target = fileFor(sound)
        target.delete()
        if (!partial.renameTo(target)) {
            partial.delete()
            throw IOException("Couldn't save the sound")
        }
        return target
    }

    fun sizeBytes(): Long = dir.listFiles()?.sumOf { it.length() } ?: 0L

    fun clear() {
        dir.listFiles()?.forEach { it.delete() }
    }

    /** Descarga el clip del repositorio de catálogos (o lo reutiliza si ya está en la caché). */
    fun download(sound: OnlineSound, userAgent: String): File {
        cached(sound)?.let { return it }
        require(sound.url.startsWith(CatalogRepo.BASE)) { "Unexpected download address" }
        val connection = URL(sound.url).openConnection() as HttpURLConnection
        connection.connectTimeout = 10_000
        connection.readTimeout = 15_000
        connection.setRequestProperty("User-Agent", userAgent)
        try {
            if (connection.responseCode != 200) throw IOException("Download answered ${connection.responseCode}")
            return connection.inputStream.use { store(sound, it) }
        } finally {
            connection.disconnect()
        }
    }
}