package dev.ofarafal.enklawa.vault

import dev.ofarafal.enklawa.crypto.SessionVault
import dev.ofarafal.enklawa.session.WorldSession
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

class VaultStoreTest {

    /** Atrapa Keystore: odwraca bajty, a po delete() odmawia odczytu. */
    private class FakeWrapper : DeviceKeyWrapper {
        var lost = false
        override fun wrap(plain: ByteArray) = plain.reversedArray()
        override fun unwrap(wrapped: ByteArray): ByteArray {
            check(!lost) { "zniknął" }
            return wrapped.reversedArray()
        }
        override fun delete() { lost = true }
    }

    private lateinit var root: File
    private val wrappers = HashMap<String, FakeWrapper>()
    private val fast = KdfParams(memoryKiB = 64, iterations = 1, parallelism = 1)
    private fun store() = VaultStore(root, { wrappers.getOrPut(it) { FakeWrapper() } }, fast)
    private val pw = "correct horse".toCharArray()

    @Before fun setUp() { root = Files.createTempDirectory("sejfy").toFile() }
    @After fun tearDown() { SessionVault.deleteRecursively(root) }

    @Test fun createAndListWithName() {
        val s = store()
        val v = s.create("  Praca ", pw)
        assertEquals("Praca", v.name)
        assertEquals(listOf(v), s.list())
    }

    @Test fun unlockWithRightPasswordGivesSameKeyEveryTime() {
        val s = store()
        val v = s.create("A", pw)
        val k1 = (s.unlock(v.id, pw) as UnlockResult.Ok).key
        val k2 = (s.unlock(v.id, pw) as UnlockResult.Ok).key
        assertArrayEquals(k1.bytes(), k2.bytes())
        assertEquals(32, k1.bytes().size)
    }

    @Test fun wrongPasswordIsRejected() {
        val s = store()
        val v = s.create("A", pw)
        assertEquals(UnlockResult.WrongPassword, s.unlock(v.id, "inne haslo!".toCharArray()))
    }

    @Test fun lostDeviceKeyIsReportedSeparately() {
        val s = store()
        val v = s.create("A", pw)
        wrappers.getValue(v.id).lost = true
        assertEquals(UnlockResult.DeviceKeyLost, s.unlock(v.id, pw))
    }

    @Test fun tamperedHeaderDoesNotUnlock() {
        val s = store()
        val v = s.create("A", pw)
        val f = File(s.vaultDir(v.id), VaultStore.HEADER)
        val text = f.readText()
        val i = text.indexOf("\"wrapped\":\"") + 11
        val flipped = if (text[i] == '0') '1' else '0'
        f.writeText(text.substring(0, i) + flipped + text.substring(i + 1))
        val r = s.unlock(v.id, pw)
        assertTrue(r is UnlockResult.WrongPassword || r is UnlockResult.DeviceKeyLost || r is UnlockResult.Damaged)
    }

    @Test fun vaultsHaveIndependentKeys() {
        val s = store()
        val a = s.create("A", pw)
        val b = s.create("B", pw)
        val ka = (s.unlock(a.id, pw) as UnlockResult.Ok).key.bytes()
        val kb = (s.unlock(b.id, pw) as UnlockResult.Ok).key.bytes()
        assertFalse(ka.contentEquals(kb))
    }

    @Test fun headerHasNoPlaintextPasswordOrKey() {
        val s = store()
        val v = s.create("A", "tajne-haslo-123".toCharArray())
        val header = File(s.vaultDir(v.id), VaultStore.HEADER).readText()
        assertFalse(header.contains("tajne-haslo-123"))
        val key = (s.unlock(v.id, "tajne-haslo-123".toCharArray()) as UnlockResult.Ok).key.bytes()
        assertFalse(header.contains(VaultCrypto.hex(key)))
    }

    @Test fun shortPasswordAndBadNameRejected() {
        val s = store()
        assertThrowsIae { s.create("A", "krotkie".toCharArray()) }
        assertThrowsIae { s.create("   ", pw) }
        assertThrowsIae { s.create("x".repeat(41), pw) }
    }

    @Test fun deleteRemovesFilesAndDeviceKey() {
        val s = store()
        val v = s.create("A", pw)
        s.delete(v.id)
        assertTrue(s.list().isEmpty())
        assertFalse(s.vaultDir(v.id).exists())
        assertTrue(wrappers.getValue(v.id).lost)
    }

    @Test fun dataSurvivesLockAndUnlock() {
        val s = store()
        val v = s.create("A", pw)
        val key1 = (s.unlock(v.id, pw) as UnlockResult.Ok).key
        val w1 = WorldSession.openVault(s.dataDir(v.id), key1)
        w1.vault.write("notatka", "treść".toByteArray())
        assertEquals(0, w1.end()) // zamknięcie nie kasuje plików

        val key2 = (s.unlock(v.id, pw) as UnlockResult.Ok).key
        val w2 = WorldSession.openVault(s.dataDir(v.id), key2)
        assertEquals(listOf("notatka"), w2.vault.names())
        assertEquals("treść", String(w2.vault.read("notatka")!!))
        w2.end()
    }

    @Test fun lockedVaultFilesAreUnreadableWithoutKey() {
        val s = store()
        val v = s.create("A", pw)
        val key = (s.unlock(v.id, pw) as UnlockResult.Ok).key
        val w = WorldSession.openVault(s.dataDir(v.id), key)
        w.vault.write("tajne", "SEKRET-XYZ".toByteArray())
        w.end()
        assertTrue(key.isWiped)
        val onDisk = s.dataDir(v.id).listFiles()!!.joinToString("") { String(it.readBytes(), Charsets.ISO_8859_1) }
        assertFalse(onDisk.contains("SEKRET-XYZ"))
        assertFalse(onDisk.contains("tajne"))
    }

    @Test fun wrongKeyCannotOpenPersistentIndex() {
        val s = store()
        val a = s.create("A", pw)
        val b = s.create("B", pw)
        val ka = (s.unlock(a.id, pw) as UnlockResult.Ok).key
        val wa = WorldSession.openVault(s.dataDir(a.id), ka)
        wa.vault.write("x", byteArrayOf(1))
        wa.end()
        val kb = (s.unlock(b.id, pw) as UnlockResult.Ok).key
        var failed = false
        try { SessionVault(s.dataDir(a.id), kb, persistent = true) } catch (e: Exception) { failed = true }
        assertTrue(failed)
        assertNull(null)
    }

    private fun assertThrowsIae(block: () -> Unit) {
        try { block(); throw AssertionError("brak wyjątku") } catch (e: IllegalArgumentException) { }
    }
}
