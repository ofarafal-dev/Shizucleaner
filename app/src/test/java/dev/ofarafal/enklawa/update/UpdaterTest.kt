package dev.ofarafal.enklawa.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdaterTest {

    private fun release(tag: String, assetName: String = "Enklawa.apk", digest: String? = "sha256:abc123"): String {
        val digestPart = if (digest != null) ",\"digest\":\"$digest\"" else ""
        val url = "https://github.com/x/y/releases/download/$tag/$assetName"
        return "{\"tag_name\":\"$tag\",\"name\":\"0.1.${tag.removePrefix("v")}\",\"body\":\"notes\"," +
            "\"assets\":[{\"name\":\"$assetName\",\"size\":1234,\"browser_download_url\":\"$url\"$digestPart}]}"
    }

    @Test
    fun newerReleaseIsAvailable() {
        val r = Updater.parse(release("v12"), currentVersionCode = 11)
        assertTrue(r is CheckResult.Available)
        r as CheckResult.Available
        assertEquals(12, r.info.versionCode)
        assertEquals("abc123", r.info.sha256)
        assertEquals(1234L, r.info.size)
    }

    @Test
    fun sameOrOlderReleaseIsUpToDate() {
        assertEquals(CheckResult.UpToDate, Updater.parse(release("v5"), 5))
        assertEquals(CheckResult.UpToDate, Updater.parse(release("v4"), 5))
    }

    @Test
    fun missingAssetIsError() {
        assertTrue(Updater.parse(release("v9", assetName = "inne.apk"), 1) is CheckResult.Error)
    }

    @Test
    fun badTagIsError() {
        assertTrue(Updater.parse(release("wersja-x"), 1) is CheckResult.Error)
    }

    @Test
    fun garbageBodyIsError() {
        assertTrue(Updater.parse("<html>nie json</html>", 1) is CheckResult.Error)
    }

    @Test
    fun missingDigestGivesNullSha() {
        val r = Updater.parse(release("v3", digest = null), 1) as CheckResult.Available
        assertEquals(null, r.info.sha256)
    }

    @Test
    fun shaComparison() {
        assertTrue(Updater.shaMatches("ABC123", "abc123"))
        assertFalse(Updater.shaMatches("abc124", "abc123"))
        assertTrue(Updater.shaMatches("cokolwiek", null))
    }

    @Test
    fun releasesUrlUsesStableRepositoryId() {
        assertTrue(Updater.RELEASES_LATEST_URL.startsWith("https://api.github.com/repositories/"))
        assertTrue(Updater.RELEASES_LATEST_URL.endsWith("/releases/latest"))
        assertTrue(Updater.REPO_ID > 0)
    }
}
