package dev.ofarafal.shizucleaner.service

import dev.ofarafal.shizucleaner.data.Wire
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.nio.file.Files

class FsOpsTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private lateinit var root: File
    private lateinit var ops: FsOps

    @Before
    fun setUp() {
        root = tmp.newFolder("sdcard")
        ops = FsOps(root.path, "dev.test.own")
    }

    private fun file(rel: String, bytes: ByteArray = ByteArray(10) { 7 }): File {
        val f = File(ops.root, rel)
        f.parentFile!!.mkdirs()
        f.writeBytes(bytes)
        return f
    }

    // ---------- resolve ----------

    @Test
    fun resolveRejectsRootAndOutside() {
        assertNull(ops.resolve(ops.root.path))
        assertNull(ops.resolve("/etc/passwd"))
        assertNull(ops.resolve(ops.root.path + "/../outside"))
        assertNull(ops.resolve("relative/path"))
        assertNull(ops.resolve(""))
    }

    @Test
    fun resolveRejectsProtectedTopLevelButAllowsChildren() {
        for (name in listOf("Download", "DCIM", "Music", "Android")) {
            assertNull("$name powinien być chroniony", ops.resolve(File(ops.root, name).path))
        }
        assertNull(ops.resolve(File(ops.root, "Android/data").path))
        assertNull(ops.resolve(File(ops.root, "Android/obb").path))
        assertNotNull(ops.resolve(File(ops.root, "Download/film.mp4").path))
        assertNotNull(ops.resolve(File(ops.root, "Android/data/com.some.app").path))
        assertNotNull(ops.resolve(File(ops.root, "Telegram").path))
    }

    @Test
    fun resolveRejectsOwnPackageData() {
        assertNull(ops.resolve(File(ops.root, "Android/data/dev.test.own").path))
        assertNull(ops.resolve(File(ops.root, "Android/data/dev.test.own/files/x").path))
    }

    @Test
    fun resolveRejectsSymlinkEscape() {
        val outside = tmp.newFolder("outside")
        File(outside, "secret.txt").writeText("x")
        Files.createSymbolicLink(File(ops.root, "link").toPath(), outside.toPath())
        assertNull(ops.resolve(File(ops.root, "link").path))
        assertNull(ops.resolve(File(ops.root, "link/secret.txt").path))
    }

    // ---------- delete ----------

    @Test
    fun deleteRemovesWholeTree() {
        file("Telegram/a.bin")
        file("Telegram/sub/b.bin")
        val res = Wire.deleteResults(ops.delete(listOf(File(ops.root, "Telegram").path), 1))
        assertEquals(1, res.size)
        assertTrue(res[0].ok)
        assertEquals(2L, res[0].files)
        assertEquals(20L, res[0].bytes)
        assertFalse(File(ops.root, "Telegram").exists())
    }

    @Test
    fun deleteRefusesProtectedAndOutsidePaths() {
        file("Download/keep.txt")
        val outside = tmp.newFile("outside.txt")
        val res = Wire.deleteResults(
            ops.delete(listOf(File(ops.root, "Download").path, outside.path, ops.root.path), 0)
        )
        assertEquals(3, res.size)
        assertTrue(res.none { it.ok })
        assertTrue(File(ops.root, "Download/keep.txt").exists())
        assertTrue(outside.exists())
    }

    @Test
    fun deleteDoesNotFollowSymlinks() {
        val outside = tmp.newFolder("outside2")
        val precious = File(outside, "precious.txt").apply { writeText("do not touch") }
        File(ops.root, "Telegram").mkdirs()
        Files.createSymbolicLink(File(ops.root, "Telegram/link").toPath(), outside.toPath())
        val res = Wire.deleteResults(ops.delete(listOf(File(ops.root, "Telegram").path), 1))
        assertTrue(res[0].ok)
        assertFalse(File(ops.root, "Telegram").exists())
        assertTrue(precious.exists())
        assertEquals("do not touch", precious.readText())
    }

    @Test
    fun deleteWithPassesOverwritesContentBeforeUnlink() {
        // Dowiązanie twarde trzyma ten sam i-węzeł: po nadpisaniu i ucięciu widać, że plik był zapisany, a nie tylko odpięty.
        val f = file("Telegram/data.bin", ByteArray(5000) { 42 })
        val hard = File(ops.root, "hardlink.bin")
        Files.createLink(hard.toPath(), f.toPath())
        ops.delete(listOf(f.path), 1)
        assertFalse(f.exists())
        assertEquals(0L, hard.length())
    }

    @Test
    fun deleteWithZeroPassesLeavesContentIntact() {
        val f = file("Telegram/data.bin", ByteArray(5000) { 42 })
        val hard = File(ops.root, "hardlink.bin")
        Files.createLink(hard.toPath(), f.toPath())
        ops.delete(listOf(f.path), 0)
        assertFalse(f.exists())
        assertEquals(5000L, hard.length())
    }

    @Test
    fun deleteMissingPathIsOk() {
        val res = Wire.deleteResults(ops.delete(listOf(File(ops.root, "Nie/istnieje").path), 1))
        assertTrue(res[0].ok)
        assertEquals(0L, res[0].files)
    }

    // ---------- skany ----------

    @Test
    fun largeFilesAreSortedAndFiltered() {
        file("a/small.bin", ByteArray(100))
        file("a/mid.bin", ByteArray(2000))
        file("b/big.bin", ByteArray(5000))
        file("Android/data/com.x/hidden.bin", ByteArray(9000)) // Android jest pomijany
        val hits = Wire.hits(ops.scanLarge(1000, 10))
        assertEquals(listOf(5000L, 2000L), hits.map { it.size })
    }

    @Test
    fun duplicatesAreGroupedByContent() {
        val same = ByteArray(3000) { (it % 251).toByte() }
        val other = ByteArray(3000) { ((it + 1) % 251).toByte() }
        file("x/a.bin", same)
        file("y/b.bin", same)
        file("z/c.bin", other)
        file("z/tiny.bin", ByteArray(10))
        val groups = Wire.dups(ops.scanDuplicates(1000, 10))
        assertEquals(1, groups.size)
        assertEquals(2, groups[0].files.size)
        assertEquals(3000L, groups[0].wasted)
        assertTrue(groups[0].files.none { it.path.endsWith("c.bin") })
    }

    @Test
    fun emptyDirsReportTopmostOnly() {
        File(ops.root, "e1/e2").mkdirs()
        file("full/file.txt")
        File(ops.root, "Download/inner").mkdirs()
        val paths = Wire.hits(ops.scanEmptyDirs(10)).map { File(it.path).relativeTo(ops.root).path }.toSet()
        assertTrue("e1" in paths)
        assertFalse("e1/e2" in paths)
        assertFalse("full" in paths)
        assertFalse("Download" in paths)
        assertTrue("Download/inner" in paths)
    }

    @Test
    fun apkScanFindsApksCaseInsensitive() {
        file("Download/app.apk")
        file("Download/OTHER.APK")
        file("Download/readme.txt")
        assertEquals(2, Wire.hits(ops.scanApks(10)).size)
    }

    @Test
    fun appDirsListSizesAndExtraTopFolders() {
        file("Android/data/com.gone.app/cache/x.bin", ByteArray(1500))
        file("Android/obb/com.gone.app/main.obb", ByteArray(500))
        file("Telegram/pic.jpg", ByteArray(300))
        val dirs = Wire.appDirs(ops.scanAppDirs(listOf("Telegram", "../etc", "Nie ma")))
        val data = dirs.single { it.root == "data" }
        assertEquals("com.gone.app", data.name)
        assertEquals(1500L, data.size)
        assertEquals(500L, dirs.single { it.root == "obb" }.size)
        val top = dirs.filter { it.root == "top" }
        assertEquals(listOf("Telegram"), top.map { it.name })
    }
}
