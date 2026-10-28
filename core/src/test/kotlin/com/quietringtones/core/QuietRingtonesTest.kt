package com.quietringtones.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class QuietRingtonesTest {
    private val a = sound("a")
    private val b = sound("b")
    private val c = sound("c")

    private fun app(
        system: FakeSoundSystem,
        store: FakeStore = FakeStore(),
    ): QuietRingtones {
        var n = 0
        return QuietRingtones(system, store) { "id${++n}" }
    }

    private fun system(
        canModify: Boolean = true,
        initial: Map<SoundKind, Selection> = emptyMap(),
    ) = FakeSoundSystem(
        canModify = canModify,
        initial = initial,
        available = SoundKind.entries.associateWith { listOf(a, b, c) },
    )

    // --- Aplicar y permiso -------------------------------------------------

    @Test
    fun `apply without permission changes nothing and captures no original`() {
        val system = system(canModify = false, initial = mapOf(SoundKind.RINGTONE to Selection.Chosen(a)))
        val store = FakeStore()
        val result = app(system, store).apply(SoundKind.RINGTONE, Selection.Chosen(b))

        assertEquals(ApplyResult.NeedsPermission, result)
        assertEquals(Selection.Chosen(a), system.current(SoundKind.RINGTONE))
        assertTrue(store.loadOriginal().isEmpty())
    }

    @Test
    fun `apply sets the system sound`() {
        val system = system()
        val result = app(system).apply(SoundKind.NOTIFICATION, Selection.Chosen(b))

        assertEquals(ApplyResult.Applied, result)
        assertEquals(Selection.Chosen(b), system.current(SoundKind.NOTIFICATION))
    }

    @Test
    fun `only the first change per kind is captured as original`() {
        val system = system(initial = mapOf(SoundKind.RINGTONE to Selection.Chosen(a)))
        val store = FakeStore()
        val app = app(system, store)

        app.apply(SoundKind.RINGTONE, Selection.Chosen(b))
        app.apply(SoundKind.RINGTONE, Selection.Chosen(c))

        assertEquals(mapOf(SoundKind.RINGTONE to Selection.Chosen(a)), store.loadOriginal())
    }

    // --- Restaurar ----------------------------------------------------------

    @Test
    fun `restore reapplies the captured originals`() {
        val system = system(initial = mapOf(SoundKind.RINGTONE to Selection.Chosen(a)))
        val app = app(system)
        app.apply(SoundKind.RINGTONE, Selection.Chosen(b))

        assertEquals(ApplyResult.Applied, app.restore())
        assertEquals(Selection.Chosen(a), system.current(SoundKind.RINGTONE))
    }

    @Test
    fun `an original of silence is restored and is not treated as never captured`() {
        val system = system(initial = mapOf(SoundKind.ALARM to Selection.Silent))
        val store = FakeStore()
        val app = app(system, store)
        app.apply(SoundKind.ALARM, Selection.Chosen(b))

        assertEquals(mapOf(SoundKind.ALARM to Selection.Silent), store.loadOriginal())
        app.restore()
        assertEquals(Selection.Silent, system.current(SoundKind.ALARM))
    }

    @Test
    fun `restore with nothing captured touches nothing`() {
        val system = system(initial = mapOf(SoundKind.RINGTONE to Selection.Chosen(a)))
        val result = app(system).restore()

        assertEquals(ApplyResult.Applied, result)
        assertTrue(system.applied.isEmpty())
    }

    @Test
    fun `restore without permission keeps the originals for later`() {
        val system = system(initial = mapOf(SoundKind.RINGTONE to Selection.Chosen(a)))
        val store = FakeStore()
        val app = app(system, store)
        app.apply(SoundKind.RINGTONE, Selection.Chosen(b))
        system.canModify = false

        assertEquals(ApplyResult.NeedsPermission, app.restore())
        assertEquals(mapOf(SoundKind.RINGTONE to Selection.Chosen(a)), store.loadOriginal())
    }

    @Test
    fun `after a restore the next change captures a fresh original`() {
        val system = system(initial = mapOf(SoundKind.RINGTONE to Selection.Chosen(a)))
        val store = FakeStore()
        val app = app(system, store)
        app.apply(SoundKind.RINGTONE, Selection.Chosen(b))
        app.restore()
        assertTrue(store.loadOriginal().isEmpty())

        // El usuario cambia el tono por su cuenta en Ajustes; ese pasa a ser el nuevo original.
        system.apply(SoundKind.RINGTONE, Selection.Chosen(c))
        app.apply(SoundKind.RINGTONE, Selection.Chosen(b))

        assertEquals(mapOf(SoundKind.RINGTONE to Selection.Chosen(c)), store.loadOriginal())
    }

    // --- Importar -------------------------------------------------------------

    @Test
    fun `an imported sound becomes available for its kind`() {
        val system = system()
        val imported = app(system).importSound("content://picker/1", "mi tono", SoundKind.RINGTONE)

        assertTrue(imported in system.available(SoundKind.RINGTONE))
        assertEquals("mi tono", imported.name)
    }

    // --- Perfiles ---------------------------------------------------------------

    @Test
    fun `saved profiles are listed and can be deleted`() {
        val app = app(system())
        val profile = app.saveProfile("  Trabajo ", mapOf(SoundKind.RINGTONE to Selection.Chosen(a)))

        assertEquals("Trabajo", profile.name)
        assertEquals(listOf(profile), app.profiles())
        app.deleteProfile(profile.id)
        assertTrue(app.profiles().isEmpty())
    }

    @Test
    fun `blank profile names are rejected`() {
        assertFailsWith<IllegalArgumentException> {
            app(system()).saveProfile("   ", emptyMap())
        }
    }

    @Test
    fun `saving a profile with an existing name replaces it`() {
        val app = app(system())
        app.saveProfile("Noche", mapOf(SoundKind.RINGTONE to Selection.Chosen(a)))
        app.saveProfile("noche", mapOf(SoundKind.RINGTONE to Selection.Chosen(b)))

        val saved = app.profiles().single()
        assertEquals(Selection.Chosen(b), saved.selections.getValue(SoundKind.RINGTONE))
    }

    @Test
    fun `current sounds can be saved as a profile`() {
        val system = system(
            initial = mapOf(
                SoundKind.RINGTONE to Selection.Chosen(a),
                SoundKind.NOTIFICATION to Selection.Chosen(b),
            ),
        )
        val profile = app(system).saveCurrentAsProfile("Casa")

        assertEquals(Selection.Chosen(a), profile.selections.getValue(SoundKind.RINGTONE))
        assertEquals(Selection.Chosen(b), profile.selections.getValue(SoundKind.NOTIFICATION))
        assertEquals(Selection.Silent, profile.selections.getValue(SoundKind.ALARM))
    }

    @Test
    fun `applying a profile sets every kind and captures originals`() {
        val system = system(initial = mapOf(SoundKind.RINGTONE to Selection.Chosen(a)))
        val store = FakeStore()
        val app = app(system, store)
        val profile = app.saveProfile(
            "Noche",
            mapOf(
                SoundKind.RINGTONE to Selection.Chosen(b),
                SoundKind.NOTIFICATION to Selection.Silent,
                SoundKind.ALARM to Selection.Chosen(c),
            ),
        )

        val result = app.applyProfile(profile.id)

        assertEquals(
            ProfileResult.Done(SoundKind.entries.associateWith { Outcome.APPLIED }),
            result,
        )
        assertEquals(Selection.Chosen(b), system.current(SoundKind.RINGTONE))
        assertEquals(Selection.Silent, system.current(SoundKind.NOTIFICATION))
        assertEquals(Selection.Chosen(c), system.current(SoundKind.ALARM))
        assertEquals(Selection.Chosen(a), store.loadOriginal().getValue(SoundKind.RINGTONE))
    }

    @Test
    fun `a missing sound falls back to the first available one`() {
        val system = system()
        val app = app(system)
        val gone = sound("borrado")
        val profile = app.saveProfile("X", mapOf(SoundKind.RINGTONE to Selection.Chosen(gone)))

        val result = app.applyProfile(profile.id)

        assertEquals(
            ProfileResult.Done(mapOf(SoundKind.RINGTONE to Outcome.FELL_BACK)),
            result,
        )
        assertEquals(Selection.Chosen(a), system.current(SoundKind.RINGTONE))
    }

    @Test
    fun `a missing sound with nothing available leaves the kind untouched`() {
        val system = FakeSoundSystem(initial = mapOf(SoundKind.RINGTONE to Selection.Chosen(a)))
        val app = app(system)
        val profile = app.saveProfile("X", mapOf(SoundKind.RINGTONE to Selection.Chosen(sound("borrado"))))

        val result = app.applyProfile(profile.id)

        assertEquals(ProfileResult.Done(mapOf(SoundKind.RINGTONE to Outcome.SKIPPED)), result)
        assertEquals(Selection.Chosen(a), system.current(SoundKind.RINGTONE))
    }

    @Test
    fun `an imported sound removed later triggers the fallback`() {
        val system = system()
        val app = app(system)
        val imported = app.importSound("content://picker/1", "mio", SoundKind.NOTIFICATION)
        val profile = app.saveProfile("X", mapOf(SoundKind.NOTIFICATION to Selection.Chosen(imported)))
        system.remove(SoundKind.NOTIFICATION, imported)

        val result = app.applyProfile(profile.id)

        assertEquals(ProfileResult.Done(mapOf(SoundKind.NOTIFICATION to Outcome.FELL_BACK)), result)
    }

    @Test
    fun `applying a profile without permission or an unknown profile is reported`() {
        val system = system(canModify = false)
        val app = app(system)
        val profile = app.saveProfile("X", mapOf(SoundKind.RINGTONE to Selection.Chosen(a)))

        assertEquals(ProfileResult.NeedsPermission, app.applyProfile(profile.id))
        assertEquals(ProfileResult.NotFound, app.applyProfile("nope"))
        assertTrue(system.applied.isEmpty())
    }
}
