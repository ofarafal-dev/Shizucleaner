package dev.ofarafal.enklawa.os

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WebAppsTest {
    @Test fun presetsAreHttpsAndUnique() {
        assertTrue(WebApps.presets.all { it.url.startsWith("https://") })
        assertEquals(WebApps.presets.size, WebApps.presets.map { it.url }.toSet().size)
    }

    @Test fun idRoundTrip() {
        val url = "https://example.org/a?b=1"
        assertEquals(url, WebApps.urlOf(WebApps.idFor(url)))
    }

    @Test fun nonWebIdsAndBadSchemesAreRejected() {
        assertNull(WebApps.urlOf("browser"))
        assertNull(WebApps.urlOf("web:javascript:alert(1)"))
        assertNull(WebApps.urlOf("web:file:///etc/passwd"))
    }

    @Test fun titles() {
        assertEquals("Wikipedia", WebApps.titleFor("https://pl.wikipedia.org"))
        assertEquals("example.org", WebApps.titleFor("https://www.example.org/x"))
    }
}
