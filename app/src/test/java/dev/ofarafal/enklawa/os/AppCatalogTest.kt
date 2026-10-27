package dev.ofarafal.enklawa.os

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppCatalogTest {
    private val a = InstalledApp("Wiki", "https://pl.wikipedia.org")
    private val b = InstalledApp("Mapy", "https://www.openstreetmap.org")

    @Test fun roundTrip() {
        val list = listOf(a, b)
        assertEquals(list, AppCatalog.parse(AppCatalog.serialize(list)))
    }

    @Test fun parseBadInputGivesEmpty() {
        assertTrue(AppCatalog.parse(null).isEmpty())
        assertTrue(AppCatalog.parse("").isEmpty())
        assertTrue(AppCatalog.parse("nie json").isEmpty())
    }

    @Test fun parseDropsUnsafeUrls() {
        val json = """[{"name":"x","url":"javascript:alert(1)"},{"name":"ok","url":"https://example.org"}]"""
        assertEquals(listOf(InstalledApp("ok", "https://example.org")), AppCatalog.parse(json))
    }

    @Test fun addDeduplicatesByUrl() {
        val list = AppCatalog.add(listOf(a, b), InstalledApp("Nowa nazwa", a.url))
        assertEquals(2, list.size)
        assertEquals("Nowa nazwa", list.first { it.url == a.url }.name)
    }

    @Test fun removeByUrl() = assertEquals(listOf(b), AppCatalog.remove(listOf(a, b), a.url))

    @Test fun originKeepsSchemeHostPort() {
        assertEquals("https://example.org", AppCatalog.originOf("https://Example.org/a/b?q=1#x"))
        assertEquals("http://localhost:8080", AppCatalog.originOf("http://localhost:8080/x"))
    }

    @Test fun originRejectsOtherSchemes() {
        assertNull(AppCatalog.originOf("javascript:alert(1)"))
        assertNull(AppCatalog.originOf("file:///etc/passwd"))
        assertNull(AppCatalog.originOf("about:blank"))
        assertNull(AppCatalog.originOf(""))
    }

    @Test fun displayNameUsesTitleThenHost() {
        assertEquals("Wikipedia", AppCatalog.displayName("  Wikipedia ", "https://pl.wikipedia.org"))
        assertEquals("example.org", AppCatalog.displayName("", "https://www.example.org"))
        assertEquals("example.org", AppCatalog.displayName("https://example.org/x", "https://example.org"))
        assertEquals(24, AppCatalog.displayName("x".repeat(60), "https://a.pl").length)
    }

    @Test fun initialAndIconKey() {
        assertEquals("W", AppCatalog.initial("  wikipedia"))
        assertEquals("?", AppCatalog.initial("…"))
        assertEquals(AppCatalog.iconKey("https://a.pl"), AppCatalog.iconKey("https://a.pl"))
        assertFalse(AppCatalog.iconKey("https://a.pl") == AppCatalog.iconKey("https://b.pl"))
        assertTrue(AppCatalog.iconKey("https://a.pl").startsWith("icon:"))
    }

    @Test fun internalNames() {
        assertTrue(AppCatalog.isInternalName(AppCatalog.FILE))
        assertTrue(AppCatalog.isInternalName("icon:abcd"))
        assertFalse(AppCatalog.isInternalName("notatka"))
    }
}
