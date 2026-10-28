package com.quietringtones.ui

import kotlin.test.Test
import kotlin.test.assertEquals

class PlaybackTest {
    @Test
    fun `the fraction follows the real position and duration`() {
        assertEquals(0f, playbackFraction(0, 8_000))
        assertEquals(0.25f, playbackFraction(2_000, 8_000))
        assertEquals(1f, playbackFraction(8_000, 8_000))
    }

    @Test
    fun `a position past the end is clamped`() {
        assertEquals(1f, playbackFraction(9_500, 8_000))
        assertEquals(0f, playbackFraction(-5, 8_000))
    }

    @Test
    fun `an unknown duration is reported as minus one`() {
        assertEquals(-1f, playbackFraction(1_000, 0))
        assertEquals(-1f, playbackFraction(1_000, -1))
    }
}
