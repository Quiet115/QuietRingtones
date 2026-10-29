package com.quietringtones.core

/** Lo que un perfil compartido dice de cada tipo de sonido: silencio, o un audio que viaja dentro del archivo. */
sealed interface BundleEntry {
    data object Silent : BundleEntry
    data class Audio(val title: String, val file: String) : BundleEntry
}

/** Un perfil listo para pasar a otro teléfono. Los audios van aparte; aquí solo se nombran. */
data class ProfileBundle(val name: String, val entries: Map<SoundKind, BundleEntry>)

internal const val DEFAULT_SHARED_NAME = "Shared profile"
private const val MAX_TITLE = 80

/** Un nombre libre: ni vacío, ni reservado, ni igual (sin mirar mayúsculas) a un perfil existente. */
internal fun freeProfileName(wanted: String, taken: List<String>): String {
    val base = wanted.trim().ifEmpty { DEFAULT_SHARED_NAME }
    fun free(candidate: String) =
        taken.none { it.equals(candidate, ignoreCase = true) } &&
            QuietRingtones.RESERVED_NAMES.none { it.equals(candidate, ignoreCase = true) }
    if (free(base)) return base
    return generateSequence(2) { it + 1 }.map { "$base ($it)" }.first(::free)
}

/** El título acaba como nombre de archivo en el teléfono: se quitan separadores y caracteres de control. */
internal fun safeTitle(title: String): String =
    title.filter { it !in "/\\:*?\"<>|" && !it.isISOControl() }.trim().trimStart('.').take(MAX_TITLE).ifEmpty { "Sound" }
