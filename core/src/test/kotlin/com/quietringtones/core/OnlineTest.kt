package com.quietringtones.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OnlineTest {
    private fun online(id: String, title: String, vararg kinds: SoundKind) = OnlineSound(
        id = id, title = title, artist = "Someone", kinds = kinds.toSet(),
        url = "https://example.test/$id.ogg", seconds = 30.0, bytes = 1, sha256 = "0".repeat(64),
        license = "Public domain", source = "https://example.test/$id",
    )

    @Test
    fun `the catalog is filtered by the tab it is shown in, keeping its order`() {
        val list = listOf(
            online("a", "A", SoundKind.RINGTONE, SoundKind.ALARM),
            online("b", "B", SoundKind.NOTIFICATION),
            online("c", "C", SoundKind.ALARM),
        )

        assertEquals(listOf("a"), list.forKind(SoundKind.RINGTONE).map { it.id })
        assertEquals(listOf("b"), list.forKind(SoundKind.NOTIFICATION).map { it.id })
        assertEquals(listOf("a", "c"), list.forKind(SoundKind.ALARM).map { it.id })
    }

    @Test
    fun `a sound counts as added once it is among the user's own audio by title`() {
        val entry = online("a", "Air on the G String", SoundKind.RINGTONE)

        assertFalse(entry.isAddedTo(listOf(Sound("content://x/1", "Something else"))))
        assertTrue(entry.isAddedTo(listOf(Sound("content://x/2", "Air on the G String"))))
    }
}