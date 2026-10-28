package com.quietringtones.core

enum class SoundKind { RINGTONE, NOTIFICATION, ALARM }

/**
 * [id] es la referencia estable del sistema (en Android, un URI de contenido).
 * La igualdad solo mira [id]: el nombre puede variar según de dónde se lea.
 */
class Sound(val id: String, val name: String) {
    override fun equals(other: Any?) = other is Sound && other.id == id
    override fun hashCode() = id.hashCode()
    override fun toString() = "Sound($id, $name)"
}

/** Silencio es un valor deliberado, distinto de "nunca capturado". */
sealed interface Selection {
    data object Silent : Selection
    data class Chosen(val sound: Sound) : Selection
}

data class Profile(val id: String, val name: String, val selections: Map<SoundKind, Selection>)

/** Sonidos disponibles de un tipo: primero los que importó el usuario, siempre, y luego los del teléfono. */
data class SoundCatalog(val yours: List<Sound>, val system: List<Sound>) {
    val all: List<Sound> get() = yours + system
}

sealed interface ApplyResult {
    data object Applied : ApplyResult
    data object NeedsPermission : ApplyResult
}

enum class Outcome { APPLIED, FELL_BACK, SKIPPED }

sealed interface ProfileResult {
    data object NeedsPermission : ProfileResult
    data object NotFound : ProfileResult
    data class Done(val outcomes: Map<SoundKind, Outcome>) : ProfileResult
}
