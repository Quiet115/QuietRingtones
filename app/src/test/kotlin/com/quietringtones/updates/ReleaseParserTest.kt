package com.quietringtones.updates

import com.quietringtones.core.Version
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ReleaseParserTest {
    private val sha = "a".repeat(64)
    private val url = "${GitHubRepo.ASSET_PREFIX}v0.2.0/QuietRingtones-0.2.0.apk"

    private fun json(
        tag: String = "v0.2.0",
        draft: Boolean = false,
        prerelease: Boolean = false,
        assetName: String = "QuietRingtones-0.2.0.apk",
        assetUrl: String = url,
        digest: String = "sha256:$sha",
    ) = """
        {"tag_name":"$tag","draft":$draft,"prerelease":$prerelease,"body":"  Notes  ",
         "html_url":"https://github.com/Quiet115/QuietRingtones/releases/tag/$tag",
         "assets":[{"name":"checksums.txt","browser_download_url":"x"},
                   {"name":"$assetName","browser_download_url":"$assetUrl","size":1234,"digest":"$digest"}]}
    """.trimIndent()

    @Test
    fun `a normal release is parsed with its apk, size and checksum`() {
        val release = ReleaseParser.parse(json())!!

        assertEquals(Version.parse("0.2.0"), release.version)
        assertEquals("Notes", release.notes)
        assertEquals(url, release.apkUrl)
        assertEquals(1234L, release.apkSize)
        assertEquals(sha, release.apkSha256)
    }

    @Test
    fun `a missing or malformed digest leaves the checksum empty`() {
        assertNull(ReleaseParser.parse(json(digest = ""))!!.apkSha256)
        assertNull(ReleaseParser.parse(json(digest = "sha256:abc"))!!.apkSha256)
    }

    @Test
    fun `drafts, prereleases and odd tags are ignored`() {
        assertNull(ReleaseParser.parse(json(draft = true)))
        assertNull(ReleaseParser.parse(json(prerelease = true)))
        assertNull(ReleaseParser.parse(json(tag = "nightly")))
    }

    @Test
    fun `an apk that is not hosted in this repository is refused`() {
        assertNull(ReleaseParser.parse(json(assetUrl = "https://evil.example/QuietRingtones.apk")))
    }

    @Test
    fun `a release without an apk asset is ignored`() {
        assertNull(ReleaseParser.parse(json(assetName = "source.zip")))
    }
}
