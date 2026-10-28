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
    fun `older saved ids are normalized when read`() {
        val old = """{"RINGTONE":{"id":"content://0@media/internal/audio/media/16?title=Luz&canonical=1","name":"Luz"}}"""

        val decoded = StoreCodec.decodeSelections(old).getValue(SoundKind.RINGTONE) as Selection.Chosen

        assertEquals("content://media/internal/audio/media/16", decoded.sound.id)
    }

    @Test
    fun `imported sounds round trip in order, without duplicates`() {
        val imported = mapOf(
            SoundKind.RINGTONE to listOf("content://media/external/audio/media/9", "content://media/external/audio/media/3"),
            SoundKind.ALARM to listOf("content://media/external/audio/media/5"),
        )

        assertEquals(imported, StoreCodec.decodeImported(StoreCodec.encodeImported(imported)))
        assertTrue(StoreCodec.decodeImported("not json").isEmpty())
        assertTrue(StoreCodec.decodeImported(null).isEmpty())
    }

    @Test
    fun `missing data decodes to empty`() {
        assertTrue(StoreCodec.decodeSelections(null).isEmpty())
        assertTrue(StoreCodec.decodeProfiles("").isEmpty())
    }
}
