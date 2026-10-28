package com.quietringtones

import kotlin.test.Test
import kotlin.test.assertEquals

class SoundIdTest {
    @Test
    fun `strips user info and query so both forms of the same sound match`() {
        val fromSettings = "content://0@media/internal/audio/media/16?title=Light%20Touch&canonical=1"
        val fromList = "content://media/internal/audio/media/16"

        assertEquals(fromList, SoundId.normalize(fromSettings))
        assertEquals(fromList, SoundId.normalize(fromList))
    }

    @Test
    fun `leaves other schemes untouched`() {
        assertEquals("file:///sdcard/a.ogg", SoundId.normalize("file:///sdcard/a.ogg"))
    }
}
