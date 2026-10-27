package dev.ofarafal.enklawa.session

import dev.ofarafal.enklawa.browser.UrlInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UrlInputTest {

    @Test
    fun fullUrlUnchanged() {
        assertEquals("https://example.com/a?b=1", UrlInput.resolve("https://example.com/a?b=1"))
        assertEquals("http://example.com", UrlInput.resolve("http://example.com"))
    }

    @Test
    fun bareHostGetsHttps() {
        assertEquals("https://example.com", UrlInput.resolve("example.com"))
    }

    @Test
    fun textBecomesSearch() {
        val u = UrlInput.resolve("jak działa tor")
        assertTrue(u.startsWith(UrlInput.SEARCH))
        assertTrue(u.contains("jak+dzia"))
    }

    @Test
    fun emptyIsBlank() {
        assertEquals("about:blank", UrlInput.resolve("   "))
    }

    @Test
    fun dangerousSchemesAreSearchedNotLoaded() {
        assertTrue(UrlInput.resolve("javascript:alert(1)").startsWith(UrlInput.SEARCH))
        assertTrue(UrlInput.resolve("file:///etc/passwd").startsWith(UrlInput.SEARCH))
    }
}
