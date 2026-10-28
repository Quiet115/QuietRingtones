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
        captureOriginal(kind)
        system.apply(kind, selection)
        return ApplyResult.Applied
    }

    fun restore(): ApplyResult {
        if (!system.canModify()) return ApplyResult.NeedsPermission
        val original = store.loadOriginal()
        original.forEach { (kind, selection) -> system.apply(kind, selection) }
        store.saveOriginal(emptyMap())
        return ApplyResult.Applied
    }

    fun profiles(): List<Profile> = store.loadProfiles()

    fun saveProfile(name: String, selections: Map<SoundKind, Selection>): Profile {
        val clean = name.trim()
        require(clean.isNotEmpty()) { "El nombre del perfil no puede estar vacío" }
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
        val profile = store.loadProfiles().firstOrNull { it.id == id } ?: return ProfileResult.NotFound
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

    private fun captureOriginal(kind: SoundKind) {
        val original = store.loadOriginal()
        if (kind in original) return
        store.saveOriginal(original + (kind to system.current(kind)))
    }
}
