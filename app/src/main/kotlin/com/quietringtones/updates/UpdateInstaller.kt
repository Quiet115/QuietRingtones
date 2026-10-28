package com.quietringtones.updates

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import com.quietringtones.core.Release
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

class UpdateDownloader(private val context: Context) {

    /** Descarga el APK a la caché y comprueba su integridad. El sistema comprueba además la firma al instalar. */
    fun download(release: Release, onProgress: (Int) -> Unit): File {
        require(release.apkUrl.startsWith(GitHubRepo.ASSET_PREFIX)) { "Unexpected download address" }
        val dir = File(context.cacheDir, "updates").apply {
            deleteRecursively()
            mkdirs()
        }
        val target = File(dir, "QuietRingtones-${release.version}.apk")
        val connection = URL(release.apkUrl).openConnection() as HttpURLConnection
        connection.connectTimeout = 15_000
        connection.readTimeout = 15_000
        try {
            if (connection.responseCode != 200) throw IOException("Download answered ${connection.responseCode}")
            val total = connection.contentLengthLong.takeIf { it > 0 } ?: release.apkSize
            val digest = MessageDigest.getInstance("SHA-256")
            var done = 0L
            var lastPercent = -1
            connection.inputStream.use { input ->
                target.outputStream().use { output ->
                    val buffer = ByteArray(32 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        digest.update(buffer, 0, read)
                        done += read
                        if (total > 0) {
                            val percent = (done * 100 / total).toInt().coerceIn(0, 100)
                            if (percent != lastPercent) {
                                lastPercent = percent
                                onProgress(percent)
                            }
                        }
                    }
                }
            }
            val actual = digest.digest().joinToString("") { "%02x".format(it) }
            val corrupt = (release.apkSha256 != null && actual != release.apkSha256) ||
                (release.apkSha256 == null && release.apkSize > 0 && target.length() != release.apkSize)
            if (corrupt) {
                target.delete()
                throw IOException("The downloaded file is corrupt")
            }
            return target
        } finally {
            connection.disconnect()
        }
    }
}

object ApkInstaller {
    fun canInstall(context: Context): Boolean = context.packageManager.canRequestPackageInstalls()

    /** Abre la pantalla donde el usuario permite a esta app instalar aplicaciones. */
    fun openInstallPermission(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}")),
        )
    }

    /** Abre el instalador del sistema, que pide la confirmación del usuario y comprueba la firma. */
    fun install(context: Context, apk: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.updates", apk)
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(intent)
    }
}
