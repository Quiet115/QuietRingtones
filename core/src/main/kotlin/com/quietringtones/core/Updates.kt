package com.quietringtones.core

/** Número de versión (1.2.3). Solo versiones finales: las etiquetas de preliminares no cuentan. */
data class Version(val parts: List<Int>) : Comparable<Version> {
    override fun compareTo(other: Version): Int {
        val size = maxOf(parts.size, other.parts.size)
        for (i in 0 until size) {
            val diff = parts.getOrElse(i) { 0 }.compareTo(other.parts.getOrElse(i) { 0 })
            if (diff != 0) return diff
        }
        return 0
    }

    // Dos versiones que se comparan igual (1.0 y 1.0.0) deben ser iguales también para colecciones y pruebas.
    override fun equals(other: Any?) = other is Version && compareTo(other) == 0
    override fun hashCode() = parts.dropLastWhile { it == 0 }.hashCode()
    override fun toString() = parts.joinToString(".")

    companion object {
        fun parse(text: String): Version? {
            val clean = text.trim().removePrefix("v").removePrefix("V")
            if (clean.isEmpty()) return null
            val numbers = clean.split('.').map { it.toIntOrNull() ?: return null }
            return if (numbers.any { it < 0 }) null else Version(numbers)
        }
    }
}

/** Una versión publicada con su APK. [apkSha256] puede faltar si la fuente no lo da. */
data class Release(
    val version: Version,
    val notes: String,
    val apkUrl: String,
    val apkSha256: String?,
    val apkSize: Long,
    val pageUrl: String,
)

/** Puerto hacia donde se publican las versiones. Lanza una excepción si no se puede consultar. */
interface ReleaseSource {
    /** La última versión publicada, o null si todavía no hay ninguna. */
    fun latest(): Release?
}

sealed interface UpdateCheck {
    data object UpToDate : UpdateCheck
    data class Available(val release: Release) : UpdateCheck
    data class Failed(val reason: String) : UpdateCheck
}

class Updater(private val source: ReleaseSource, private val current: Version) {
    /** [skipped] es una versión que el usuario decidió omitir: no se ofrece de nuevo, pero sí una posterior. */
    fun check(skipped: Version? = null): UpdateCheck = try {
        val latest = source.latest()
        when {
            latest == null || latest.version <= current -> UpdateCheck.UpToDate
            skipped != null && latest.version <= skipped -> UpdateCheck.UpToDate
            else -> UpdateCheck.Available(latest)
        }
    } catch (e: Exception) {
        UpdateCheck.Failed(e.message ?: e.javaClass.simpleName)
    }
}
