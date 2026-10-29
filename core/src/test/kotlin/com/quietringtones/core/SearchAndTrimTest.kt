package com.quietringtones.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SearchAndTrimTest {
    // --- Búsqueda -----------------------------------------------------------------

    @Test
    fun `search ignores case and accents`() {
        assertTrue(Search.matches("Gymnopédie No. 1", "gymnopedie"))
        assertTrue(Search.matches("Air on the G String", "AIR"))
    }

    @Test
    fun `every typed word must appear, in any order`() {
        assertTrue(Search.matches("Symphony No. 5, I. Allegro con brio", "allegro symphony"))
        assertFalse(Search.matches("Symphony No. 5, I. Allegro con brio", "allegro mozart"))
    }

    @Test
    fun `an empty search matches everything`() {
        assertTrue(Search.matches("Anything", "   "))
    }

    @Test
    fun `lists are filtered by name, and the catalog also by artist`() {
        val sounds = listOf(Sound("1", "Light Touch"), Sound("2", "Morning Xylophone"))
        assertEquals(listOf("Light Touch"), sounds.matching("touch").map { it.name })

        val online = listOf(
            OnlineSound("a", "Air", "Johann Sebastian Bach", setOf(SoundKind.RINGTONE), "u", 30.0, 1, "0".repeat(64), "PD", "s"),
            OnlineSound("b", "Clair de lune", "Claude Debussy", setOf(SoundKind.RINGTONE), "u", 30.0, 1, "0".repeat(64), "PD", "s"),
        )
        assertEquals(listOf("a"), online.matchingOnline("bach").map { it.id })
        assertEquals(listOf("b"), online.matchingOnline("clair").map { it.id })
    }

    // --- Recorte ------------------------------------------------------------------

    private val minute = 60_000L

    @Test
    fun `a long audio starts as its first 30 seconds and a short one as the whole`() {
        val long = TrimSelection.initial(4 * minute)
        assertEquals(0L, long.startMs)
        assertEquals(30_000L, long.lengthMs)

        val short = TrimSelection.initial(12_000)
        assertEquals(12_000L, short.lengthMs)
        assertTrue(short.isWholeFile)
    }

    @Test
    fun `the user can pick any length up to the whole audio, such as a minute for a call`() {
        val selection = TrimSelection.initial(4 * minute).withLength(minute)

        assertEquals(minute, selection.lengthMs)
        assertEquals(0L, selection.startMs)
        assertEquals(4 * minute, selection.withLength(10 * minute).lengthMs)
    }

    @Test
    fun `moving the start keeps the length and never passes the end of the audio`() {
        val selection = TrimSelection.initial(2 * minute).withLength(20_000).withStart(minute + 50_000)

        assertEquals(20_000L, selection.lengthMs)
        assertEquals(minute + 40_000, selection.startMs)
        assertEquals(2 * minute, selection.endMs)
        assertEquals(0L, selection.withStart(-5).startMs)
    }

    @Test
    fun `lengthening a selection near the end moves the start back so it still fits`() {
        val selection = TrimSelection.initial(minute).withLength(10_000).withStart(50_000).withLength(25_000)

        assertEquals(25_000L, selection.lengthMs)
        assertEquals(35_000L, selection.startMs)
        assertEquals(minute, selection.endMs)
    }

    @Test
    fun `a selection is never shorter than one second, or than the audio itself`() {
        assertEquals(1_000L, TrimSelection.initial(minute).withLength(10).lengthMs)
        assertEquals(400L, TrimSelection.initial(400).withLength(10).lengthMs)
    }

    @Test
    fun `whole file selects everything and is detected`() {
        val whole = TrimSelection.initial(3 * minute).withStart(10_000).whole()

        assertTrue(whole.isWholeFile)
        assertEquals(3 * minute, whole.lengthMs)
        assertFalse(TrimSelection.initial(3 * minute).isWholeFile)
    }
}