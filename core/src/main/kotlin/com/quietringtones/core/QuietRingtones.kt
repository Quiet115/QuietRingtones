package com.quietringtones.core

import java.util.UUID

class QuietRingtones(
    private val system: SoundSystem,
    private val store: Store,
    private val newId: () -> String = { UUID.randomUUID().toString() },
) {
    fun canModify() = system.canModify()

    fun current(): Map<SoundKind, Selection> = SoundKind.entries.associateWith { system.current(it) }

    fun available(kind: SoundKind): List<Sound> = catalog(kind).all

    /** Los audios importados van siempre al inicio, el más reciente primero; si el archivo ya no existe, no salen. */
    fun catalog(kind: SoundKind): SoundCatalog {
        val importedIds = store.loadImported()[kind].orEmpty()
        val available = system.available(kind)
        val byId = available.associateBy { it.id }
        return SoundCatalog(
            yours = importedIds.mapNotNull { byId[it] },
            system = available.filter { it.id !in importedIds },
        )
    }

    fun importSound(source: String, displayName: String, kind: SoundKind): Sound {
        val sound = system.importFile(source, displayName, kind)
        val imported = store.loadImported()
        store.saveImported(imported + (kind to (listOf(sound.id) + imported[kind].orEmpty().filter { it != sound.id })))
        return sound
    }

    fun apply(kind: SoundKind, selection: Selection): ApplyResult {
        if (!system.canModify()) return ApplyResult.NeedsPermission
        captureOriginal()
        system.apply(kind, selection)
        return ApplyResult.Applied
    }

    /** El Estado original, si existe, va primero como un perfil más (solo lectura). */
    fun profiles(): List<Profile> {
        val original = store.loadOriginal()
        val originalProfile = if (original.isEmpty()) emptyList() else listOf(Profile(ORIGINAL_ID, ORIGINAL_NAME, original))
        return originalProfile + store.loadProfiles()
    }

    fun saveProfile(name: String, selections: Map<SoundKind, Selection>): Profile {
        val clean = name.trim()
        require(clean.isNotEmpty()) { "El nombre del perfil no puede estar vacío" }
        require(RESERVED_NAMES.none { it.equals(clean, ignoreCase = true) }) { "\"$clean\" is a reserved name" }
        val existing = store.loadProfiles()
        val id = existing.firstOrNull { it.name.equals(clean, ignoreCase = true) }?.id ?: newId()
        val profile = Profile(id, clean, selections)
        store.saveProfiles(existing.filterNot { it.id == id } + profile)
        return profile
    }

    fun saveCurrentAsProfile(name: String): Profile = saveProfile(name, current())

    /**
     * Crea el perfil que llega de otro teléfono. Cada audio se importa como sonido de su tipo y el perfil apunta a él.
     * Nunca pisa un perfil propio: si el nombre ya existe, se le añade un número.
     */
    fun importProfile(bundle: ProfileBundle, audioSource: (BundleEntry.Audio) -> String): Profile {
        val selections = bundle.entries.mapValues { (kind, entry) ->
            when (entry) {
                BundleEntry.Silent -> Selection.Silent
                is BundleEntry.Audio -> {
                    val extension = entry.file.substringAfterLast('.', "")
                    val title = safeTitle(entry.title)
                    val displayName = if (extension.isEmpty()) title else "$title.$extension"
                    Selection.Chosen(importSound(audioSource(entry), displayName, kind))
                }
            }
        }
        return saveProfile(freeProfileName(bundle.name, store.loadProfiles().map { it.name }), selections)
    }

    fun deleteProfile(id: String) {
        store.saveProfiles(store.loadProfiles().filterNot { it.id == id })
    }

    /** El perfil cuyos sonidos son los que suenan ahora, si hay alguno (el Estado original cuenta). */
    fun activeProfile(): Profile? {
        val now = current()
        return profiles().firstOrNull { profile ->
            profile.selections.isNotEmpty() && profile.selections.all { (kind, selection) -> now[kind] == selection }
        }
    }

    /** El perfil que sigue al activo, en el orden de la lista y dando la vuelta; si ninguno está activo, el primero. */
    fun nextProfile(): Profile? {
        val all = profiles()
        if (all.isEmpty()) return null
        val active = activeProfile() ?: return all.first()
        return all[(all.indexOfFirst { it.id == active.id } + 1) % all.size]
    }

    /** Pasa al perfil siguiente (para el tile de ajustes rápidos). Null si no hay ningún perfil. */
    fun switchToNextProfile(): ProfileResult? = nextProfile()?.let { applyProfile(it.id) }

    fun applyProfile(id: String): ProfileResult {
        val profile = profiles().firstOrNull { it.id == id } ?: return ProfileResult.NotFound
        if (!system.canModify()) return ProfileResult.NeedsPermission

        val outcomes = profile.selections.mapValues { (kind, selection) -> applyOne(kind, selection) }
        return ProfileResult.Done(outcomes)
    }

    private fun applyOne(kind: SoundKind, selection: Selection): Outcome {
        if (selection is Selection.Chosen && selection.sound !in system.available(kind)) {
            val fallback = system.available(kind).firstOrNull() ?: return Outcome.SKIPPED
            apply(kind, Selection.Chosen(fallback))
            return Outcome.FELL_BACK
        }
        apply(kind, selection)
        return Outcome.APPLIED
    }

    /** Guarda el Estado original en cuanto se puede (al abrir la app), sin esperar al primer cambio. */
    fun ensureOriginal() = captureOriginal()

    /** Antes de cualquier cambio, captura lo que falte del Estado original; lo ya capturado no se toca. */
    private fun captureOriginal() {
        val original = store.loadOriginal()
        val missing = SoundKind.entries.filter { it !in original }
        if (missing.isEmpty()) return
        store.saveOriginal(original + missing.associateWith { system.current(it) })
    }

    companion object {
        const val ORIGINAL_ID = "original"
        const val ORIGINAL_NAME = "Original state"

        /** El nombre del Estado original en cada idioma de la app: ningún perfil propio puede usarlos. */
        val RESERVED_NAMES = setOf(ORIGINAL_NAME, "Estado original")
    }
}
