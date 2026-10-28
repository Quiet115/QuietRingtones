package com.quietringtones.core

class FakeSoundSystem(
    var canModify: Boolean = true,
    initial: Map<SoundKind, Selection> = emptyMap(),
    available: Map<SoundKind, List<Sound>> = emptyMap(),
) : SoundSystem {
    private val active = SoundKind.entries.associateWith { initial[it] ?: Selection.Silent }.toMutableMap()
    private val catalog = SoundKind.entries.associateWith { (available[it] ?: emptyList()).toMutableList() }.toMutableMap()
    val applied = mutableListOf<Pair<SoundKind, Selection>>()

    override fun canModify() = canModify
    override fun current(kind: SoundKind) = active.getValue(kind)
    override fun available(kind: SoundKind) = catalog.getValue(kind).toList()

    override fun apply(kind: SoundKind, selection: Selection) {
        check(canModify) { "apply without permission" }
        active[kind] = selection
        applied += kind to selection
    }

    override fun importFile(source: String, displayName: String, kind: SoundKind): Sound {
        val sound = Sound("content://imported/$displayName", displayName)
        catalog.getValue(kind).add(sound)
        return sound
    }

    fun remove(kind: SoundKind, sound: Sound) {
        catalog.getValue(kind).remove(sound)
    }
}

class FakeStore : Store {
    private var original: Map<SoundKind, Selection> = emptyMap()
    private var profiles: List<Profile> = emptyList()
    private var imported: Map<SoundKind, List<String>> = emptyMap()

    override fun loadOriginal() = original
    override fun saveOriginal(original: Map<SoundKind, Selection>) { this.original = original }
    override fun loadProfiles() = profiles
    override fun saveProfiles(profiles: List<Profile>) { this.profiles = profiles }
    override fun loadImported() = imported
    override fun saveImported(imported: Map<SoundKind, List<String>>) { this.imported = imported }
}

fun sound(name: String) = Sound("content://sys/$name", name)
