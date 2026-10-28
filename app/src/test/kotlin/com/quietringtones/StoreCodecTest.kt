package com.quietringtones

import com.quietringtones.core.Profile
import com.quietringtones.core.Selection
import com.quietringtones.core.Sound
import com.quietringtones.core.SoundKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StoreCodecTest {
    private val sound = Sound("content://media/internal/audio/media/7", "Pulso \"suave\"")

    @Test
    fun `selections round trip, keeping silence distinct from absent`() {
        val selections = mapOf(
            SoundKind.RINGTONE to Selection.Chosen(sound),
            SoundKind.ALARM to Selection.Silent,
        )
        val decoded = StoreCodec.decodeSelections(StoreCodec.encodeSelections(selections))

        assertEquals(selections, decoded)
        assertTrue(SoundKind.NOTIFICATION !in decoded)
    }

    @Test
    fun `profiles round trip`() {
        val profiles = listOf(Profile("p1", "Noche", mapOf(SoundKind.NOTIFICATION to Selection.Silent)))

        assertEquals(profiles, StoreCodec.decodeProfiles(StoreCodec.encodeProfiles(profiles)))
    }

    @Test
    fun `missing data decodes to empty`() {
        assertTrue(StoreCodec.decodeSelections(null).isEmpty())
        assertTrue(StoreCodec.decodeProfiles("").isEmpty())
    }
}
