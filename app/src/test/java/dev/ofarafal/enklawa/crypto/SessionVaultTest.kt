package dev.ofarafal.enklawa.crypto

import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files
import javax.crypto.AEADBadTagException

class SessionVaultTest {

    private lateinit var dir: File
    private lateinit var key: SessionKey
    private lateinit var vault: SessionVault

    @Before
    fun setUp() {
        dir = Files.createTempDirectory("vault").toFile()
        key = SessionKey.generate()
        vault = SessionVault(File(dir, "v"), key)
    }

    @After
    fun tearDown() {
        SessionVault.deleteRecursively(dir)
    }

    private fun diskFiles() = File(dir, "v").listFiles().orEmpty().toList()

    @Test
    fun roundTrip() {
        val data = "notatka śćłó 123".toByteArray()
        vault.write("n1", data)
        assertArrayEquals(data, vault.read("n1"))
    }

    @Test
    fun plaintextNotOnDisk() {
        val secret = "SEKRETNY-TEKST-DO-ZNALEZIENIA".toByteArray()
        vault.write("n1", secret)
        val raw = diskFiles().single().readBytes()
        assertFalse(String(raw, Charsets.ISO_8859_1).contains("SEKRETNY"))
        assertEquals(SessionVault.NONCE_SIZE + secret.size + SessionVault.TAG_BITS / 8, raw.size)
    }

    @Test
    fun diskNamesDoNotRevealLogicalNames() {
        vault.write("haslo-do-banku", byteArrayOf(1))
        assertFalse(diskFiles().single().name.contains("haslo"))
    }

    @Test
    fun sameDataEncryptedTwiceDiffers() {
        vault.write("a", ByteArray(64) { 5 })
        val first = diskFiles().single().readBytes()
        vault.write("a", ByteArray(64) { 5 })
        val second = diskFiles().single().readBytes()
        assertFalse(first.contentEquals(second)) // nowy nonce przy każdym zapisie
    }

    @Test
    fun overwriteKeepsSingleFile() {
        vault.write("a", byteArrayOf(1))
        vault.write("a", byteArrayOf(2))
        assertEquals(1, diskFiles().size)
        assertArrayEquals(byteArrayOf(2), vault.read("a"))
    }

    @Test
    fun missingNameReturnsNull() {
        assertNull(vault.read("nie-ma"))
    }

    @Test
    fun tamperedCiphertextIsRejected() {
        vault.write("a", ByteArray(40) { 3 })
        val f = diskFiles().single()
        val raw = f.readBytes()
        raw[raw.size - 1] = (raw[raw.size - 1].toInt() xor 1).toByte()
        f.writeBytes(raw)
        try {
            vault.read("a"); fail("zmodyfikowany plik powinien zostać odrzucony")
        } catch (_: AEADBadTagException) {
        }
    }

    @Test
    fun swappingFilesBetweenNamesIsDetected() {
        vault.write("a", ByteArray(32) { 1 })
        vault.write("b", ByteArray(32) { 2 })
        val files = diskFiles()
        val (f1, f2) = files
        val b1 = f1.readBytes(); val b2 = f2.readBytes()
        f1.writeBytes(b2); f2.writeBytes(b1)
        try {
            vault.read("a"); fail("AAD powinno wykryć podmianę")
        } catch (_: AEADBadTagException) {
        }
    }

    @Test
    fun wrongKeyCannotDecrypt() {
        vault.write("a", ByteArray(32) { 1 })
        val other = SessionVault(File(dir, "v"), SessionKey.generate())
        // drugi magazyn nie zna indeksu; sprawdzamy bezpośrednio, że obcy klucz nie odszyfruje pliku
        val raw = diskFiles().single().readBytes()
        val cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            javax.crypto.Cipher.DECRYPT_MODE, SessionKey.generate().asKey(),
            javax.crypto.spec.GCMParameterSpec(128, raw.copyOfRange(0, 12))
        )
        cipher.updateAAD("a".toByteArray())
        try {
            cipher.doFinal(raw, 12, raw.size - 12); fail()
        } catch (_: AEADBadTagException) {
        }
        assertNull(other.read("a"))
    }

    @Test
    fun deleteRemovesFile() {
        vault.write("a", byteArrayOf(1))
        vault.delete("a")
        assertTrue(diskFiles().isEmpty())
        assertNull(vault.read("a"))
    }

    @Test
    fun destroyWipesKeyAndRemovesEverything() {
        vault.write("a", byteArrayOf(1, 2, 3))
        vault.write("b", byteArrayOf(4))
        vault.destroy()
        assertTrue(key.isWiped)
        assertFalse(File(dir, "v").exists())
        assertTrue(vault.names().isEmpty())
    }

    @Test
    fun writeAfterDestroyFails() {
        vault.destroy()
        try {
            vault.write("a", byteArrayOf(1)); fail("po destroy zapis musi się nie udać")
        } catch (_: IllegalStateException) {
        }
    }
}
