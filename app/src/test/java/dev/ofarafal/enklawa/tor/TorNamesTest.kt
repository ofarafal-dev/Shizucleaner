package dev.ofarafal.enklawa.tor

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TorNamesTest {
    @Test fun matchesTorDirectories() {
        assertTrue(isTorDirName("tor"))
        assertTrue(isTorDirName("Tor-data"))
        assertTrue(isTorDirName("app_tor"))
        assertTrue(isTorDirName("app_tor_cache"))
    }

    @Test fun doesNotTouchOtherData() {
        assertFalse(isTorDirName("sejfy"))
        assertFalse(isTorDirName("app_webview"))
        assertFalse(isTorDirName("datastore"))
        assertFalse(isTorDirName("vector"))
        assertFalse(isTorDirName("app_texture"))
    }
}
