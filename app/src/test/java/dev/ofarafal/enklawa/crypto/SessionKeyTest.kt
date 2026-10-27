package dev.ofarafal.enklawa.crypto

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class SessionKeyTest {

    @Test
    fun generatesKeyOfCorrectSize() {
        assertEquals(SessionKey.SIZE_BYTES, SessionKey.generate().bytes().size)
    }

    @Test
    fun twoKeysDiffer() {
        assertFalse(SessionKey.generate().bytes().contentEquals(SessionKey.generate().bytes()))
    }

    @Test
    fun bytesReturnsCopy() {
        val k = SessionKey.of(ByteArray(32) { 7 })
        k.bytes().fill(0)
        assertArrayEquals(ByteArray(32) { 7 }, k.bytes())
    }

    @Test
    fun wipeZeroesInternalMaterial() {
        val src = ByteArray(32) { 9 }
        val k = SessionKey.of(src)
        k.wipe()
        assertTrue(k.isWiped)
    }

    @Test
    fun afterWipeAccessFails() {
        val k = SessionKey.generate()
        k.wipe()
        try {
            k.bytes(); fail("bytes() po wipe powinno rzucić")
        } catch (_: IllegalStateException) {
        }
        try {
            k.asKey(); fail("asKey() po wipe powinno rzucić")
        } catch (_: IllegalStateException) {
        }
    }

    @Test
    fun wipeIsIdempotent() {
        val k = SessionKey.generate()
        k.wipe(); k.wipe()
        assertTrue(k.isWiped)
    }

    @Test
    fun rejectsWrongSize() {
        try {
            SessionKey.of(ByteArray(16)); fail()
        } catch (_: IllegalArgumentException) {
        }
    }

    @Test
    fun ofCopiesInput() {
        val src = ByteArray(32) { 1 }
        val k = SessionKey.of(src)
        src.fill(0)
        assertNotEquals(0, k.bytes()[0].toInt())
    }
}
