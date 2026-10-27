package dev.ofarafal.shizucleaner.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class WireTest {

    @Test
    fun diskStatsParsesColumns() {
        val text = """
            Some other line
            Package Names: ["com.a.app","com.b.app"]
            App Sizes: [1000,2000]
            App Data Sizes: [10,20]
            Cache Sizes: [1,2]
        """.trimIndent()
        val m = DiskStats.parse(text)
        assertEquals(DiskStats.Sizes(1000, 10, 1), m["com.a.app"])
        assertEquals(DiskStats.Sizes(2000, 20, 2), m["com.b.app"])
    }

    @Test
    fun diskStatsToleratesMissingOrShortColumns() {
        assertTrue(DiskStats.parse("nic tu nie ma").isEmpty())
        val m = DiskStats.parse("Package Names: [\"com.a.app\"]\nApp Sizes: []")
        assertEquals(DiskStats.Sizes(0, 0, 0), m["com.a.app"])
    }

    @Test
    fun hitsAndDupsDecode() {
        val raw = "H\t100\t5\t/p/a b.bin\nH\tx\t1\t/bad\nD\t0\t50\t1\t/a\nD\t0\t50\t2\t/b\nD\t1\t70\t3\t/c\nD\t1\t70\t4\t/d\n"
        val hits = Wire.hits(raw)
        assertEquals(1, hits.size)
        assertEquals("/p/a b.bin", hits[0].path)
        val groups = Wire.dups(raw)
        assertEquals(2, groups.size)
        assertEquals(50L, groups[0].wasted)
        assertEquals(70L, groups[1].wasted)
    }

    @Test
    fun deleteResultsDecodeOkAndErr() {
        val r = Wire.deleteResults("OK\t/a\t3\t300\t1\nERR\t/b\tnie można\n\nśmieci\n")
        assertEquals(2, r.size)
        assertTrue(r[0].ok)
        assertEquals(300L, r[0].bytes)
        assertEquals(1L, r[0].notOverwritten)
        assertFalse(r[1].ok)
        assertEquals("nie można", r[1].message)
    }

    @Test
    fun checkErrorThrowsOnlyOnErrorMarker() {
        Wire.checkError("H\t1\t2\t/x\n")
        try {
            Wire.checkError("E\tboom")
            fail("oczekiwano wyjątku")
        } catch (e: IllegalStateException) {
            assertEquals("boom", e.message)
        }
    }
}
