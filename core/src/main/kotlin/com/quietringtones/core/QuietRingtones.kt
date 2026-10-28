package com.quietringtones.core

import java.util.UUID

class QuietRingtones(
    private val system: SoundSystem,
    private val store: Store,
    private val newId: () -> String = { UUID.randomUUID().toString() },
) {
    fun canModify() = system.canModify()

    fun current(): Map<SoundKind, Selection> = SoundKind.entries.associateWith { system.current(it) }

    fun available(kind: SoundKind): List<Sound> = system.available(kind)

    fun importSound(source: String, displayName: String, kind: SoundKind): Sound =
        system.importFile(source, displayName, kind)

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

    fun deleteProfile(id: String) {
        store.saveProfiles(store.loadProfiles().filterNot { it.id == id })
    }

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
