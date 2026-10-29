package com.quietringtones.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
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
    fun `the first change captures every kind as original`() {
        val system = system(
            initial = mapOf(
                SoundKind.RINGTONE to Selection.Chosen(a),
                SoundKind.NOTIFICATION to Selection.Chosen(b),
            ),
        )
        val store = FakeStore()
        app(system, store).apply(SoundKind.RINGTONE, Selection.Chosen(c))

        assertEquals(
            mapOf(
                SoundKind.RINGTONE to Selection.Chosen(a),
                SoundKind.NOTIFICATION to Selection.Chosen(b),
                SoundKind.ALARM to Selection.Silent,
            ),
            store.loadOriginal(),
        )
    }

    @Test
    fun `the original is never overwritten by later changes`() {
        val system = system(initial = mapOf(SoundKind.RINGTONE to Selection.Chosen(a)))
        val store = FakeStore()
        val app = app(system, store)

        app.apply(SoundKind.RINGTONE, Selection.Chosen(b))
        app.apply(SoundKind.RINGTONE, Selection.Chosen(c))

        assertEquals(Selection.Chosen(a), store.loadOriginal().getValue(SoundKind.RINGTONE))
    }

    @Test
    fun `kinds missing from an older original are captured on the next change`() {
        val system = system(initial = mapOf(SoundKind.ALARM to Selection.Chosen(b)))
        val store = FakeStore().apply { saveOriginal(mapOf(SoundKind.RINGTONE to Selection.Chosen(a))) }

        app(system, store).apply(SoundKind.ALARM, Selection.Chosen(c))

        assertEquals(Selection.Chosen(a), store.loadOriginal().getValue(SoundKind.RINGTONE))
        assertEquals(Selection.Chosen(b), store.loadOriginal().getValue(SoundKind.ALARM))
    }

    // --- Estado original como perfil ------------------------------------------

    @Test
    fun `there is no original profile until something is changed`() {
        assertTrue(app(system()).profiles().isEmpty())
    }

    @Test
    fun `ensuring the original captures every kind once and never overwrites it`() {
        val system = system(initial = mapOf(SoundKind.RINGTONE to Selection.Chosen(a)))
        val store = FakeStore()
        val app = app(system, store)

        app.ensureOriginal()
        system.apply(SoundKind.RINGTONE, Selection.Chosen(b))
        app.ensureOriginal()

        assertEquals(SoundKind.entries.toSet(), store.loadOriginal().keys)
        assertEquals(Selection.Chosen(a), store.loadOriginal().getValue(SoundKind.RINGTONE))
        assertEquals(QuietRingtones.ORIGINAL_ID, app.profiles().first().id)
    }

    @Test
    fun `the original shows up as the first profile`() {
        val system = system(initial = mapOf(SoundKind.RINGTONE to Selection.Chosen(a)))
        val app = app(system)
        app.saveProfile("Noche", emptyMap())
        app.apply(SoundKind.RINGTONE, Selection.Chosen(b))

        val first = app.profiles().first()
        assertEquals(QuietRingtones.ORIGINAL_ID, first.id)
        assertEquals(QuietRingtones.ORIGINAL_NAME, first.name)
        assertEquals(Selection.Chosen(a), first.selections.getValue(SoundKind.RINGTONE))
        assertEquals(listOf("Noche"), app.profiles().drop(1).map { it.name })
    }

    @Test
    fun `applying the original profile restores the sounds including silence`() {
        val system = system(initial = mapOf(SoundKind.RINGTONE to Selection.Chosen(a)))
        val app = app(system)
        app.apply(SoundKind.RINGTONE, Selection.Chosen(b))
        app.apply(SoundKind.ALARM, Selection.Chosen(c))

        val result = app.applyProfile(QuietRingtones.ORIGINAL_ID)

        assertEquals(ProfileResult.Done(SoundKind.entries.associateWith { Outcome.APPLIED }), result)
        assertEquals(Selection.Chosen(a), system.current(SoundKind.RINGTONE))
        assertEquals(Selection.Silent, system.current(SoundKind.ALARM))
    }

    @Test
    fun `the original can be applied again after a later change`() {
        val system = system(initial = mapOf(SoundKind.RINGTONE to Selection.Chosen(a)))
        val app = app(system)
        app.apply(SoundKind.RINGTONE, Selection.Chosen(b))
        app.applyProfile(QuietRingtones.ORIGINAL_ID)
        app.apply(SoundKind.RINGTONE, Selection.Chosen(c))

        app.applyProfile(QuietRingtones.ORIGINAL_ID)

        assertEquals(Selection.Chosen(a), system.current(SoundKind.RINGTONE))
    }

    @Test
    fun `the original profile cannot be deleted or reused as a name`() {
        val system = system(initial = mapOf(SoundKind.RINGTONE to Selection.Chosen(a)))
        val app = app(system)
        app.apply(SoundKind.RINGTONE, Selection.Chosen(b))

        app.deleteProfile(QuietRingtones.ORIGINAL_ID)

        assertEquals(QuietRingtones.ORIGINAL_ID, app.profiles().first().id)
        assertFailsWith<IllegalArgumentException> { app.saveProfile(" estado ORIGINAL ", emptyMap()) }
        assertFailsWith<IllegalArgumentException> { app.saveProfile("original STATE", emptyMap()) }
    }

    @Test
    fun `applying the original without permission is reported`() {
        val system = system(initial = mapOf(SoundKind.RINGTONE to Selection.Chosen(a)))
        val app = app(system)
        app.apply(SoundKind.RINGTONE, Selection.Chosen(b))
        system.canModify = false

        assertEquals(ProfileResult.NeedsPermission, app.applyProfile(QuietRingtones.ORIGINAL_ID))
    }

    // --- Importar -------------------------------------------------------------

    @Test
    fun `an imported sound becomes available for its kind`() {
        val system = system()
        val imported = app(system).importSound("content://picker/1", "mi tono", SoundKind.RINGTONE)

        assertTrue(imported in system.available(SoundKind.RINGTONE))
        assertEquals("mi tono", imported.name)
    }

    @Test
    fun `imported sounds are listed first, newest first, and the rest follow`() {
        val system = system()
        val app = app(system)
        val first = app.importSound("content://picker/1", "uno", SoundKind.RINGTONE)
        val second = app.importSound("content://picker/2", "dos", SoundKind.RINGTONE)

        val catalog = app.catalog(SoundKind.RINGTONE)

        assertEquals(listOf(second, first), catalog.yours)
        assertEquals(listOf(a, b, c), catalog.system)
        assertEquals(listOf(second, first, a, b, c), app.available(SoundKind.RINGTONE))
    }

    @Test
    fun `an import stays first for that user across sessions`() {
        val system = system()
        val store = FakeStore()
        val imported = app(system, store).importSound("content://picker/1", "mio", SoundKind.NOTIFICATION)

        val reopened = app(system, store)

        assertEquals(listOf(imported), reopened.catalog(SoundKind.NOTIFICATION).yours)
        assertTrue(reopened.catalog(SoundKind.RINGTONE).yours.isEmpty())
    }

    @Test
    fun `importing the same file again moves it to the front without duplicating it`() {
        val system = system()
        val app = app(system)
        val one = app.importSound("content://picker/1", "uno", SoundKind.ALARM)
        app.importSound("content://picker/2", "dos", SoundKind.ALARM)
        system.remove(SoundKind.ALARM, one)
        val again = app.importSound("content://picker/1", "uno", SoundKind.ALARM)

        assertEquals(1, app.catalog(SoundKind.ALARM).yours.count { it == again })
        assertEquals(again, app.catalog(SoundKind.ALARM).yours.first())
    }

    @Test
    fun `an imported file that no longer exists is not listed`() {
        val system = system()
        val app = app(system)
        val gone = app.importSound("content://picker/1", "borrado", SoundKind.RINGTONE)
        system.remove(SoundKind.RINGTONE, gone)

        assertTrue(app.catalog(SoundKind.RINGTONE).yours.isEmpty())
    }
    // --- Cambio rápido de perfil -------------------------------------------------

    @Test
    fun `the active profile is the one whose sounds are playing now`() {
        val system = system(initial = mapOf(SoundKind.RINGTONE to Selection.Chosen(a), SoundKind.NOTIFICATION to Selection.Chosen(b)))
        val app = app(system)
        val night = app.saveProfile("Noche", mapOf(SoundKind.RINGTONE to Selection.Chosen(c)))
        val home = app.saveProfile("Casa", mapOf(SoundKind.RINGTONE to Selection.Chosen(a), SoundKind.NOTIFICATION to Selection.Chosen(b)))

        assertEquals(home.id, app.activeProfile()?.id)
        app.applyProfile(night.id)
        assertEquals(night.id, app.activeProfile()?.id)
    }

    @Test
    fun `no profile is active when none matches or none exists`() {
        val system = system(initial = mapOf(SoundKind.RINGTONE to Selection.Chosen(a)))
        val app = app(system)
        assertNull(app.activeProfile())
        app.saveProfile("Noche", mapOf(SoundKind.RINGTONE to Selection.Chosen(b)))
        assertNull(app.activeProfile())
    }

    @Test
    fun `switching cycles through the profiles, original first, and wraps around`() {
        val system = system(initial = mapOf(SoundKind.RINGTONE to Selection.Chosen(a)))
        val app = app(system)
        app.saveProfile("Noche", mapOf(SoundKind.RINGTONE to Selection.Chosen(b)))
        app.saveProfile("Trabajo", mapOf(SoundKind.RINGTONE to Selection.Chosen(c)))
        app.apply(SoundKind.RINGTONE, Selection.Chosen(a)) // captura el original y lo deja activo

        val names = mutableListOf<String>()
        repeat(4) {
            app.switchToNextProfile()
            names += app.activeProfile()!!.name
        }

        assertEquals(listOf("Noche", "Trabajo", "Estado original".let { QuietRingtones.ORIGINAL_NAME }, "Noche"), names)
    }

    @Test
    fun `with no active profile switching starts at the first one`() {
        val system = system(initial = mapOf(SoundKind.RINGTONE to Selection.Chosen(c)))
        val app = app(system)
        val first = app.saveProfile("Noche", mapOf(SoundKind.RINGTONE to Selection.Chosen(a)))
        app.saveProfile("Casa", mapOf(SoundKind.RINGTONE to Selection.Chosen(b)))

        assertEquals(first.id, app.nextProfile()?.id)
    }

    @Test
    fun `switching with no profiles does nothing, and without permission it is reported`() {
        assertNull(app(system()).switchToNextProfile())
        val locked = system(canModify = false)
        val app = app(locked)
        app.saveProfile("Noche", mapOf(SoundKind.RINGTONE to Selection.Chosen(a)))
        assertEquals(ProfileResult.NeedsPermission, app.switchToNextProfile())
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
