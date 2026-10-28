package dev.ofarafal.enklawa.miniapp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class MiniAppsTest {
    private fun zip(vararg e: Pair<String, String>): ByteArrayInputStream {
        val b = ByteArrayOutputStream()
        ZipOutputStream(b).use { z -> e.forEach { (n, c) -> z.putNextEntry(ZipEntry(n)); z.write(c.toByteArray()); z.closeEntry() } }
        return ByteArrayInputStream(b.toByteArray())
    }
    private val okManifest = """{"id":"haslo","name":"Hasła","permissions":["storage"]}"""

    private fun fails(block: () -> Unit) {
        try { block(); fail("oczekiwano MiniAppException") } catch (_: MiniAppException) {}
    }

    @Test fun safePathAcceptsNormalAndRejectsEscapes() {
        assertEquals("a/b.js", MiniApps.safePath("a//./b.js"))
        assertNull(MiniApps.safePath("../x"))
        assertNull(MiniApps.safePath("a/../../x"))
        assertNull(MiniApps.safePath("/etc/passwd"))
        assertNull(MiniApps.safePath("a\\b"))
        assertNull(MiniApps.safePath(""))
    }

    @Test fun manifestParses() {
        val m = MiniApps.parseManifest("""{"id":"gen-kodow","name":"Kody","entry":"app/index.html","permissions":["storage","network"]}""")
        assertEquals("app/index.html", m.entry)
        assertEquals(setOf(Perm.STORAGE, Perm.NETWORK), m.permissions)
    }

    @Test fun manifestDefaultsToNoPermissionsAndIndex() {
        val m = MiniApps.parseManifest("""{"id":"ab","name":"X"}""")
        assertEquals("index.html", m.entry)
        assertTrue(m.permissions.isEmpty())
    }

    @Test fun manifestRejectsBadInput() {
        fails { MiniApps.parseManifest("nie json") }
        fails { MiniApps.parseManifest("""{"id":"Zly Id","name":"X"}""") }
        fails { MiniApps.parseManifest("""{"id":"ok","name":""}""") }
        fails { MiniApps.parseManifest("""{"id":"ok","name":"X","entry":"../x.html"}""") }
        fails { MiniApps.parseManifest("""{"id":"ok","name":"X","permissions":["camera"]}""") }
    }

    @Test fun readsZipAndValidates() {
        val files = MiniApps.readZip(zip("manifest.json" to okManifest, "index.html" to "<p>hi</p>"))
        assertEquals("haslo", MiniApps.validate(files).id)
    }

    @Test fun stripsSingleRootFolder() {
        val files = MiniApps.readZip(zip("haslo/manifest.json" to okManifest, "haslo/index.html" to "x"))
        assertTrue(files.containsKey("index.html"))
    }

    @Test fun rejectsZipSlipAndMissingManifestAndEntry() {
        fails { MiniApps.readZip(zip("../evil.txt" to "x", "manifest.json" to okManifest)) }
        fails { MiniApps.readZip(zip("index.html" to "x")) }
        fails { MiniApps.validate(MiniApps.readZip(zip("manifest.json" to okManifest))) }
    }

    @Test fun rejectsOversizedFile() {
        val big = "a".repeat(MiniApps.MAX_FILE_BYTES + 1)
        fails { MiniApps.readZip(zip("manifest.json" to okManifest, "big.bin" to big)) }
    }

    @Test fun cspBlocksNetworkWithoutPermission() {
        assertTrue(MiniApps.csp(false).contains("connect-src 'self'"))
        assertFalse(MiniApps.csp(false).contains("https:"))
        assertTrue(MiniApps.csp(true).contains("https:"))
    }

    @Test fun hostsAreUniquePerAppAndInstalledIdsParse() {
        assertFalse(MiniApps.hostFor("a1") == MiniApps.hostFor("a2"))
        assertEquals(listOf("a1", "b2"), MiniApps.installedIds(listOf("mini/b2/m", "mini/a1/m", "mini/a1/f/index.html", "inne")))
    }

    @Test fun mimeTypes() {
        assertEquals("text/html", MiniApps.mime("index.html"))
        assertEquals("text/javascript", MiniApps.mime("a/app.js"))
        assertEquals("application/octet-stream", MiniApps.mime("x"))
    }

    @Test fun builtInExamplesAreValid() {
        assertEquals(emptySet<Perm>(), MiniApps.validate(Examples.passwordGenerator).permissions)
        assertEquals(setOf(Perm.STORAGE), MiniApps.validate(Examples.todo).permissions)
    }
}
