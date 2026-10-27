package dev.ofarafal.enklawa.session

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoLockTest {
    @Test fun notInBackgroundNeverLocks() = assertFalse(AutoLock.shouldLock(null, 1_000_000))
    @Test fun shortStayDoesNotLock() = assertFalse(AutoLock.shouldLock(1_000, 1_000 + 60_000))
    @Test fun limitLocks() = assertTrue(AutoLock.shouldLock(1_000, 1_000 + AutoLock.DEFAULT_LIMIT_MS))
    @Test fun longStayLocks() = assertTrue(AutoLock.shouldLock(0, 3_600_000))
    @Test fun customLimit() = assertTrue(AutoLock.shouldLock(0, 10_000, limitMs = 5_000))
}
