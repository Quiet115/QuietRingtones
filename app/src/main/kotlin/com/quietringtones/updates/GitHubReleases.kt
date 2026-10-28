package com.quietringtones.updates

import com.quietringtones.core.Release
import com.quietringtones.core.ReleaseSource
import com.quietringtones.core.Version
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

object GitHubRepo {
    const val RELEASES_API = "https://api.github.com/repos/Quiet115/QuietRingtones/releases/latest"
    const val RELEASES_PAGE = "https://github.com/Quiet115/QuietRingtones/releases"

    /** Solo se descargan APK publicados en las versiones de este repositorio. */
    const val ASSET_PREFIX = "https://github.com/Quiet115/QuietRingtones/releases/download/"
}

object ReleaseParser {
    /** Devuelve null si es borrador, preliminar, no tiene versión válida o no trae un APK del repositorio. */
    fun parse(json: String): Release? {
        val root = JSONObject(json)
        if (root.optBoolean("draft") || root.optBoolean("prerelease")) return null
        val version = Version.parse(root.optString("tag_name")) ?: return null
        val assets = root.optJSONArray("assets") ?: return null
        for (i in 0 until assets.length()) {
            val asset = assets.getJSONObject(i)
            val url = asset.optString("browser_download_url")
            if (!asset.optString("name").endsWith(".apk", ignoreCase = true)) continue
            if (!url.startsWith(GitHubRepo.ASSET_PREFIX)) continue
            val digest = asset.optString("digest")
            val sha = digest.removePrefix("sha256:").lowercase()
                .takeIf { digest.startsWith("sha256:") && it.length == 64 && it.all { c -> c in "0123456789abcdef" } }
            return Release(
                version = version,
                notes = root.optString("body").trim(),
                apkUrl = url,
                apkSha256 = sha,
                apkSize = asset.optLong("size"),
                pageUrl = root.optString("html_url"),
            )
        }
        return null
    }
}

class GitHubReleases(private val userAgent: String) : ReleaseSource {
    override fun latest(): Release? {
        val connection = URL(GitHubRepo.RELEASES_API).openConnection() as HttpURLConnection
        connection.connectTimeout = 10_000
        connection.readTimeout = 10_000
        connection.setRequestProperty("Accept", "application/vnd.github+json")
        connection.setRequestProperty("User-Agent", userAgent)
        try {
            return when (val code = connection.responseCode) {
                200 -> ReleaseParser.parse(connection.inputStream.bufferedReader().use { it.readText() })
                404 -> null
                else -> throw IOException("GitHub answered $code")
            }
        } finally {
            connection.disconnect()
        }
    }
}
