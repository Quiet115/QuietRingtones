package com.quietringtones.share

import com.quietringtones.core.BundleEntry
import com.quietringtones.core.ProfileBundle
import com.quietringtones.core.SoundKind
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ProfileFileTest {
    private val bundle = ProfileBundle(
        "Trip",
        mapOf(
            SoundKind.RINGTONE to BundleEntry.Audio("Bells", "ringtone.mp3"),
            SoundKind.NOTIFICATION to BundleEntry.Silent,
            SoundKind.ALARM to BundleEntry.Audio("Dawn", "alarm.ogg"),
        ),
    )
    private val audio = mapOf("ringtone.mp3" to byteArrayOf(1, 2, 3), "alarm.ogg" to ByteArray(40_000) { it.toByte() })

    private fun dir(): File = Files.createTempDirectory("qrprofile").toFile()

    private fun zip(vararg parts: Pair<String, ByteArray>): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { z ->
            parts.forEach { (name, bytes) ->
                z.putNextEntry(ZipEntry(name))
                z.write(bytes)
                z.closeEntry()
            }
        }
        return out.toByteArray()
    }

    private fun written(): ByteArray {
        val out = ByteArrayOutputStream()
        ProfileFile.write(out, bundle) { ByteArrayInputStream(audio.getValue(it.file)) }
        return out.toByteArray()
    }

    @Test
    fun `a written profile reads back with the same entries and audio`() {
        val (read, files) = ProfileFile.read(ByteArrayInputStream(written()), dir())

        assertEquals(bundle, read)
        assertEquals(setOf("ringtone.mp3", "alarm.ogg"), files.keys)
        audio.forEach { (name, bytes) -> assertContentEquals(bytes, files.getValue(name).readBytes()) }
    }

    @Test
    fun `a file that is not a profile is rejected`() {
        assertFailsWith<ProfileFileException> { ProfileFile.read(ByteArrayInputStream(byteArrayOf(1, 2, 3)), dir()) }
        assertFailsWith<ProfileFileException> { ProfileFile.read(ByteArrayInputStream(zip("other.txt" to byteArrayOf(1))), dir()) }
    }

    @Test
    fun `a newer format is rejected`() {
        val json = ProfileFile.manifest(bundle).replace("\"format\":1", "\"format\":2")

        assertFailsWith<ProfileFileException> { ProfileFile.parse(json) }
    }

    @Test
    fun `file names that could escape the folder are rejected`() {
        val json = ProfileFile.manifest(bundle).replace("ringtone.mp3", "../ringtone.mp3")

        assertFailsWith<ProfileFileException> { ProfileFile.parse(json) }
    }

    @Test
    fun `a file name must match its sound type`() {
        val json = ProfileFile.manifest(bundle).replace("alarm.ogg", "ringtone.ogg")

        assertFailsWith<ProfileFileException> { ProfileFile.parse(json) }
    }

    @Test
    fun `a profile whose audio is missing is rejected`() {
        val manifest = ProfileFile.manifest(bundle).toByteArray()

        assertFailsWith<ProfileFileException> {
            ProfileFile.read(ByteArrayInputStream(zip("profile.json" to manifest, "ringtone.mp3" to byteArrayOf(1))), dir())
        }
    }

    @Test
    fun `entries the manifest does not list are ignored`() {
        val manifest = ProfileFile.manifest(bundle).toByteArray()
        val data = zip(
            "profile.json" to manifest,
            "../evil.sh" to byteArrayOf(9),
            "ringtone.mp3" to audio.getValue("ringtone.mp3"),
            "alarm.ogg" to audio.getValue("alarm.ogg"),
        )
        val target = dir()
        val (_, files) = ProfileFile.read(ByteArrayInputStream(data), target)

        assertEquals(setOf("ringtone.mp3", "alarm.ogg"), files.keys)
        assertEquals(setOf("ringtone.mp3", "alarm.ogg"), target.list()!!.toSet())
    }

    @Test
    fun `a manifest that is too large is rejected`() {
        val huge = ByteArray(ProfileFile.MAX_MANIFEST_BYTES + 1) { ' '.code.toByte() }

        assertFailsWith<ProfileFileException> { ProfileFile.read(ByteArrayInputStream(zip("profile.json" to huge)), dir()) }
    }

    @Test
    fun `a profile with only silence is valid`() {
        val silent = ProfileBundle("Quiet", mapOf(SoundKind.RINGTONE to BundleEntry.Silent))

        assertEquals(silent, ProfileFile.parse(ProfileFile.manifest(silent)))
    }
}
