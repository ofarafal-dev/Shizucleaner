package dev.ofarafal.enklawa.session

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

class WorldSessionTest {

    private lateinit var parent: File

    @Before
    fun setUp() {
        parent = Files.createTempDirectory("swiaty").toFile()
    }

    @After
    fun tearDown() {
        parent.deleteRecursively()
    }

    @Test
    fun startCreatesSessionDirectory() {
        val s = WorldSession.start(parent)
        assertTrue(s.root.isDirectory)
        assertTrue(s.root.name.startsWith(WorldSession.ROOT_PREFIX))
        s.end()
    }

    @Test
    fun endDestroysKeyAndDeletesAllFiles() {
        val s = WorldSession.start(parent)
        s.vault.write("notatka", "tajne".toByteArray())
        File(s.root, "extra.tmp").writeText("x")
        assertEquals(0, s.end())
        assertTrue(s.key.isWiped)
        assertFalse(s.root.exists())
        assertTrue(parent.listFiles().orEmpty().isEmpty())
    }

    @Test
    fun endIsIdempotentAndRunsCleanupsOnce() {
        val s = WorldSession.start(parent)
        var calls = 0
        s.onEnd { calls++ }
        s.end(); s.end()
        assertEquals(1, calls)
        assertTrue(s.isEnded)
    }

    @Test
    fun failingCleanupDoesNotBlockOthersAndIsCounted() {
        val s = WorldSession.start(parent)
        var ran = false
        s.onEnd { ran = true }
        s.onEnd { error("boom") }
        assertEquals(1, s.end())
        assertTrue(ran)
        assertTrue(s.key.isWiped)
        assertFalse(s.root.exists())
    }

    @Test
    fun cleanupsRunInReverseOrder() {
        val s = WorldSession.start(parent)
        val order = mutableListOf<Int>()
        s.onEnd { order += 1 }
        s.onEnd { order += 2 }
        s.end()
        assertEquals(listOf(2, 1), order)
    }

    @Test
    fun cleanupRegisteredAfterEndRunsImmediately() {
        val s = WorldSession.start(parent)
        s.end()
        var ran = false
        s.onEnd { ran = true }
        assertTrue(ran)
    }

    @Test
    fun sweepRemovesLeftoversFromKilledProcess() {
        File(parent, WorldSession.ROOT_PREFIX + "deadbeef/vault").mkdirs()
        File(parent, WorldSession.ROOT_PREFIX + "deadbeef/vault/abc").writeText("szyfrogram")
        File(parent, WorldSession.ROOT_PREFIX + "cafe").mkdirs()
        val other = File(parent, "inny-plik").apply { writeText("zostaje") }
        assertEquals(2, WorldSession.sweepLeftovers(parent))
        assertTrue(other.exists())
        assertEquals(1, parent.listFiles().orEmpty().size)
    }

    @Test
    fun startSweepsPreviousLeftovers() {
        File(parent, WorldSession.ROOT_PREFIX + "old/vault").mkdirs()
        val s = WorldSession.start(parent)
        assertFalse(File(parent, WorldSession.ROOT_PREFIX + "old").exists())
        s.end()
    }

    @Test
    fun twoSessionsHaveDifferentKeysAndDirs() {
        val a = WorldSession.start(parent)
        val aKey = a.key.bytes()
        a.end()
        val b = WorldSession.start(parent)
        assertFalse(aKey.contentEquals(b.key.bytes()))
        b.end()
    }
}
