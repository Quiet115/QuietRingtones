package com.quietringtones.online

import com.quietringtones.core.OnlineSound
import com.quietringtones.core.SoundKind
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

object CatalogRepo {
    /** Solo se descarga de aquí: el catálogo vive en el repositorio de catálogos del proyecto. */
    const val BASE = "https://raw.githubusercontent.com/Quiet115/QuietRingtones-catalog/main/"
    const val CATALOG_URL = BASE + "catalog.json"
}

object CatalogParser {
    private val idPattern = Regex("[a-z0-9-]+")
    private val shaPattern = Regex("[0-9a-f]{64}")

    /** Lee el índice; las entradas mal formadas se descartan en vez de romper todo el catálogo. */
    fun parse(json: String): List<OnlineSound> {
        val root = JSONObject(json)
        if (root.optInt("version") != 1) return emptyList()
        val sounds = root.optJSONArray("sounds") ?: return emptyList()
        return (0 until sounds.length()).mapNotNull { i -> runCatching { entry(sounds.getJSONObject(i)) }.getOrNull() }
    }

    private fun entry(o: JSONObject): OnlineSound {
        val id = o.getString("id")
        require(idPattern.matches(id)) { "bad id" }
        val file = o.getString("file")
        require(file.startsWith("clips/") && file.endsWith(".ogg") && !file.contains("..") && !file.contains("//")) { "bad file" }
        val sha = o.getString("sha256").lowercase()
        require(shaPattern.matches(sha)) { "bad checksum" }
        val kindsJson = o.getJSONArray("kinds")
        val kinds = (0 until kindsJson.length()).mapNotNull {
            when (kindsJson.getString(it)) {
                "ringtone" -> SoundKind.RINGTONE
                "notification" -> SoundKind.NOTIFICATION
                "alarm" -> SoundKind.ALARM
                else -> null
            }
        }.toSet()
        require(kinds.isNotEmpty()) { "no kinds" }
        return OnlineSound(
            id = id,
            title = o.getString("title"),
            artist = o.optString("artist"),
            kinds = kinds,
            url = CatalogRepo.BASE + file,
            seconds = o.optDouble("seconds", 0.0),
            bytes = o.optLong("bytes"),
            sha256 = sha,
            license = o.optString("license"),
            source = o.optString("source"),
        )
    }
}

/** Lee el índice de GitHub y guarda una copia, para poder mostrarlo sin conexión. */
class OnlineCatalogSource(private val cacheFile: File, private val userAgent: String) {
    fun load(): List<OnlineSound> {
        try {
            val connection = URL(CatalogRepo.CATALOG_URL).openConnection() as HttpURLConnection
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.setRequestProperty("User-Agent", userAgent)
            try {
                if (connection.responseCode != 200) throw IOException("GitHub answered ${connection.responseCode}")
                val text = connection.inputStream.bufferedReader().use { it.readText() }
                val parsed = CatalogParser.parse(text)
                cacheFile.parentFile?.mkdirs()
                cacheFile.writeText(text)
                return parsed
            } finally {
                connection.disconnect()
            }
        } catch (e: IOException) {
            if (cacheFile.isFile) return CatalogParser.parse(cacheFile.readText())
            throw e
        }
    }
}