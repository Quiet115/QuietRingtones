package com.quietringtones.online

import com.quietringtones.core.OnlineSound
import com.quietringtones.core.SoundKind
import java.io.ByteArrayInputStream
import java.io.IOException
import java.security.MessageDigest
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OnlineCatalogTest {
    private val sha = "a".repeat(64)

    private fun entry(
        id: String = "bach-air",
        file: String = "clips/bach-air.ogg",
        checksum: String = sha,
        kinds: String = """["ringtone","alarm"]""",
    ) = """{"id":"$id","title":"Air","artist":"Bach","kinds":$kinds,"file":"$file","seconds":30.0,"bytes":10,"sha256":"$checksum","license":"Public domain","source":"https://example.test"}"""

    private fun catalog(vararg entries: String, version: Int = 1) = """{"version":$version,"sounds":[${entries.joinToString(",")}]}"""

    @Test
    fun `a valid entry is read with its download address under the catalog repository`() {
        val sound = CatalogParser.parse(catalog(entry())).single()

        assertEquals("bach-air", sound.id)
        assertEquals(setOf(SoundKind.RINGTONE, SoundKind.ALARM), sound.kinds)
        assertEquals(CatalogRepo.BASE + "clips/bach-air.ogg", sound.url)
    }

    @Test
    fun `malformed entries are skipped and the rest are kept`() {
        val parsed = CatalogParser.parse(
            catalog(
                entry(id = "ok"),
                entry(id = "Bad Id"),
                entry(file = "../secrets.ogg"),
                entry(file = "other/x.ogg"),
                entry(checksum = "abc"),
                entry(kinds = """["unknown"]"""),
            ),
        )

        assertEquals(listOf("ok"), parsed.map { it.id })
    }

    @Test
    fun `an unknown catalog version is ignored`() {
        assertTrue(CatalogParser.parse(catalog(entry(), version = 2)).isEmpty())
    }

    private fun soundFor(bytes: ByteArray): OnlineSound {
        val checksum = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
        return OnlineSound("clip", "Clip", "X", setOf(SoundKind.RINGTONE), CatalogRepo.BASE + "clips/clip.ogg", 1.0, bytes.size.toLong(), checksum, "CC0", "")
    }

    @Test
    fun `a clip is stored when its checksum matches and counted in the cache size`() {
        val dir = createTempDirectory().toFile()
        val bytes = ByteArray(1000) { it.toByte() }
        val sound = soundFor(bytes)
        val cache = PreviewCache(dir)

        assertNull(cache.cached(sound))
        cache.store(sound, ByteArrayInputStream(bytes))

        assertEquals(1000L, cache.sizeBytes())
        assertEquals(bytes.toList(), cache.cached(sound)!!.readBytes().toList())
        dir.deleteRecursively()
    }

    @Test
    fun `a clip with the wrong checksum is refused and leaves nothing behind`() {
        val dir = createTempDirectory().toFile()
        val sound = soundFor(ByteArray(10))
        val cache = PreviewCache(dir)

        assertFailsWith<IOException> { cache.store(sound, ByteArrayInputStream(ByteArray(10) { 1 })) }

        assertEquals(0L, cache.sizeBytes())
        assertNull(cache.cached(sound))
        dir.deleteRecursively()
    }

    @Test
    fun `clearing the cache removes every preview`() {
        val dir = createTempDirectory().toFile()
        val bytes = ByteArray(50) { 7 }
        val sound = soundFor(bytes)
        val cache = PreviewCache(dir)
        cache.store(sound, ByteArrayInputStream(bytes))

        cache.clear()

        assertEquals(0L, cache.sizeBytes())
        assertNull(cache.cached(sound))
        dir.deleteRecursively()
    }
}