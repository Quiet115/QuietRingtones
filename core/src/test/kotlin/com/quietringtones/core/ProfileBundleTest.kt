package com.quietringtones.core

import kotlin.test.Test
import kotlin.test.assertEquals

class ProfileBundleTest {
    private val system = FakeSoundSystem()
    private val store = FakeStore()
    private val app = QuietRingtones(system, store, newId = generateSequence(1) { it + 1 }.map { "id$it" }.iterator()::next)

    private val bundle = ProfileBundle(
        "Trip",
        mapOf(
            SoundKind.RINGTONE to BundleEntry.Audio("Bells", "ringtone.mp3"),
            SoundKind.NOTIFICATION to BundleEntry.Silent,
            SoundKind.ALARM to BundleEntry.Audio("Dawn", "alarm.ogg"),
        ),
    )

    private fun import(b: ProfileBundle = bundle) = app.importProfile(b) { "file:///cache/${it.file}" }

    @Test
    fun `an imported profile points at the sounds it brought`() {
        val profile = import()

        assertEquals("Trip", profile.name)
        assertEquals(Selection.Chosen(Sound("content://imported/Bells.mp3", "Bells")), profile.selections[SoundKind.RINGTONE])
        assertEquals(Selection.Silent, profile.selections[SoundKind.NOTIFICATION])
        assertEquals(Selection.Chosen(Sound("content://imported/Dawn.ogg", "Dawn")), profile.selections[SoundKind.ALARM])
        assertEquals(profile, app.profiles().single { it.id == profile.id })
    }

    @Test
    fun `imported audio is kept among the user's own sounds`() {
        import()

        assertEquals(listOf("content://imported/Bells.mp3"), app.catalog(SoundKind.RINGTONE).yours.map { it.id })
    }

    @Test
    fun `importing never overwrites a profile with the same name`() {
        val mine = app.saveProfile("Trip", mapOf(SoundKind.RINGTONE to Selection.Silent))
        val imported = import()

        assertEquals("Trip (2)", imported.name)
        assertEquals(setOf("Trip", "Trip (2)"), app.profiles().map { it.name }.toSet())
        assertEquals(mine, app.profiles().single { it.name == "Trip" })
    }

    @Test
    fun `the second import of the same file gets the next number`() {
        import()
        import()

        assertEquals(listOf("Trip", "Trip (2)"), app.profiles().map { it.name })
        assertEquals("Trip (3)", import().name)
    }

    @Test
    fun `a reserved or empty name is replaced`() {
        assertEquals("original state (2)", import(bundle.copy(name = "original state")).name)
        assertEquals("Shared profile", import(bundle.copy(name = "   ")).name)
    }

    @Test
    fun `titles cannot carry path separators into the file name`() {
        val profile = import(
            ProfileBundle("x", mapOf(SoundKind.RINGTONE to BundleEntry.Audio("../etc/passwd", "ringtone.mp3"))),
        )

        val chosen = profile.selections.getValue(SoundKind.RINGTONE) as Selection.Chosen
        assertEquals("content://imported/etcpasswd.mp3", chosen.sound.id)
    }
}
