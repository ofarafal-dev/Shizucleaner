package dev.ofarafal.enklawa.session

import dev.ofarafal.enklawa.browser.BrowserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ProfileSweepTest {

    @Test
    fun sweepTakesOnlyAmnesiaProfiles() {
        val names = listOf("Default", "enk_deadbeef", "enk_cafe", BrowserProfile.PERSISTENT_NAME)
        assertEquals(listOf("enk_deadbeef", "enk_cafe"), BrowserProfile.leftoversToSweep(names))
    }

    @Test
    fun persistentProfileIsNeverSweptAsLeftover() {
        assertFalse(BrowserProfile.PERSISTENT_NAME.startsWith(BrowserProfile.PREFIX))
        assertEquals(emptyList<String>(), BrowserProfile.leftoversToSweep(listOf(BrowserProfile.PERSISTENT_NAME)))
    }
}
