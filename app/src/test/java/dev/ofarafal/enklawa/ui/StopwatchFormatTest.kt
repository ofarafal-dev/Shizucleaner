package dev.ofarafal.enklawa.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class StopwatchFormatTest {
    @Test fun formats() {
        assertEquals("00:00.0", formatStopwatch(0))
        assertEquals("01:05.3", formatStopwatch(65_300))
        assertEquals("00:00.0", formatStopwatch(-5))
    }
}
