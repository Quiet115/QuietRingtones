package com.quietringtones.core

/** Puerto hacia los ajustes de sonido del sistema operativo. */
interface SoundSystem {
    fun canModify(): Boolean
    fun current(kind: SoundKind): Selection
    fun available(kind: SoundKind): List<Sound>
    fun apply(kind: SoundKind, selection: Selection)

    /** Incorpora un archivo del usuario al sistema como sonido de [kind]. */
    fun importFile(source: String, displayName: String, kind: SoundKind): Sound
}

/** Puerto de persistencia local. Un tipo ausente en [loadOriginal] significa "nunca capturado". */
interface Store {
    fun loadOriginal(): Map<SoundKind, Selection>
    fun saveOriginal(original: Map<SoundKind, Selection>)
    fun loadProfiles(): List<Profile>
    fun saveProfiles(profiles: List<Profile>)

    /** Sonidos que el usuario importó, por tipo y con el más reciente primero. */
    fun loadImported(): Map<SoundKind, List<String>>
    fun saveImported(imported: Map<SoundKind, List<String>>)
}
