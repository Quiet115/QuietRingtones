package com.quietringtones.core

import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UpdatesTest {
    private fun release(version: String) = Release(
        version = Version.parse(version)!!,
        notes = "notes",
        apkUrl = "https://example.test/$version.apk",
        apkSha256 = null,
        apkSize = 10,
        pageUrl = "https://example.test/$version",
    )

    private class FakeSource(var result: () -> Release?) : ReleaseSource {
        override fun latest(): Release? = result()
    }

    private fun updater(current: String, latest: Release?) =
        Updater(FakeSource { latest }, Version.parse(current)!!)

    // --- Versiones -----------------------------------------------------------

    @Test
    fun `versions parse with or without a leading v`() {
        assertEquals(Version(listOf(1, 2, 3)), Version.parse("v1.2.3"))
        assertEquals(Version(listOf(1, 2, 3)), Version.parse("1.2.3"))
        assertEquals(Version(listOf(2)), Version.parse("V2"))
    }

    @Test
    fun `prereleases and junk are not versions`() {
        assertNull(Version.parse("v1.2.3-beta"))
        assertNull(Version.parse("latest"))
        assertNull(Version.parse(""))
        assertNull(Version.parse("1..2"))
    }

    @Test
    fun `versions compare numerically and ignore trailing zeros`() {
        assertTrue(Version.parse("0.10.0")!! > Version.parse("0.9.9")!!)
        assertTrue(Version.parse("1.0.1")!! > Version.parse("1.0")!!)
        assertEquals(0, Version.parse("1.0")!!.compareTo(Version.parse("1.0.0")!!))
        assertEquals("1.2.3", Version.parse("v1.2.3").toString())
    }

    // --- Comprobador -----------------------------------------------------------

    @Test
    fun `a newer release is offered`() {
        val latest = release("0.2.0")
        assertEquals(UpdateCheck.Available(latest), updater("0.1.0", latest).check())
    }

    @Test
    fun `the same or an older release means up to date`() {
        assertEquals(UpdateCheck.UpToDate, updater("0.2.0", release("0.2.0")).check())
        assertEquals(UpdateCheck.UpToDate, updater("0.3.0", release("0.2.0")).check())
    }

    @Test
    fun `no published releases means up to date`() {
        assertEquals(UpdateCheck.UpToDate, updater("0.1.0", null).check())
    }

    @Test
    fun `a skipped version is not offered, but a newer one is`() {
        val skipped = Version.parse("0.2.0")
        assertEquals(UpdateCheck.UpToDate, updater("0.1.0", release("0.2.0")).check(skipped))
        assertTrue(updater("0.1.0", release("0.3.0")).check(skipped) is UpdateCheck.Available)
    }

    @Test
    fun `a failing source is reported instead of thrown`() {
        val updater = Updater(FakeSource { throw IOException("offline") }, Version.parse("0.1.0")!!)

        val result = updater.check()

        assertTrue(result is UpdateCheck.Failed)
        assertNotNull((result as UpdateCheck.Failed).reason)
    }
}
