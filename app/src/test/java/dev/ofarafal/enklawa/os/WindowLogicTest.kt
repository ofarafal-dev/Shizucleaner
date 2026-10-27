package dev.ofarafal.enklawa.os

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WindowLogicTest {

    private val aw = 360f
    private val ah = 600f

    private fun open(s: WindowsState, app: String, singleton: Boolean = true) =
        WindowLogic.open(s, app, app, singleton, aw, ah)

    @Test
    fun openCreatesActiveWindowOnTop() {
        val s = open(open(WindowsState(), "a"), "b")
        assertEquals(listOf("a", "b"), s.windows.map { it.appId })
        assertEquals(s.windows.last().id, s.activeId)
    }

    @Test
    fun singletonReusesExistingWindowAndFocusesIt() {
        var s = open(WindowsState(), "a")
        s = open(s, "b")
        s = open(s, "a")
        assertEquals(2, s.windows.size)
        assertEquals("a", s.windows.last().appId)
        assertEquals(s.windows.last().id, s.activeId)
    }

    @Test
    fun singletonRestoresMinimizedWindow() {
        var s = open(WindowsState(), "a")
        s = WindowLogic.minimize(s, s.activeId!!)
        s = open(s, "a")
        assertFalse(s.windows.single().minimized)
    }

    @Test
    fun nonSingletonOpensSecondWindow() {
        val s = open(open(WindowsState(), "a", singleton = false), "a", singleton = false)
        assertEquals(2, s.windows.size)
        assertTrue(s.windows[0].id != s.windows[1].id)
    }

    @Test
    fun newWindowsAreCascadedAndFitInArea() {
        var s = WindowsState()
        repeat(8) { s = open(s, "app$it") }
        s.windows.forEach {
            assertTrue(it.x >= 0f && it.y >= 0f)
            assertTrue(it.x + it.w <= aw + 0.01f)
            assertTrue(it.y + it.h <= ah + 0.01f)
        }
        assertTrue(s.windows[0].x != s.windows[1].x)
    }

    @Test
    fun closeActivatesTopmostVisibleWindow() {
        var s = open(open(open(WindowsState(), "a"), "b"), "c")
        val b = s.windows[1].id
        s = WindowLogic.minimize(s, b)
        s = WindowLogic.close(s, s.activeId!!) // zamyka c
        assertEquals("a", s.windows.single { it.id == s.activeId }.appId)
        assertEquals(2, s.windows.size)
    }

    @Test
    fun closingLastWindowLeavesNoActive() {
        var s = open(WindowsState(), "a")
        s = WindowLogic.close(s, s.activeId!!)
        assertTrue(s.windows.isEmpty())
        assertNull(s.activeId)
    }

    @Test
    fun closingNonActiveKeepsActive() {
        var s = open(open(WindowsState(), "a"), "b")
        val a = s.windows[0].id
        val active = s.activeId
        s = WindowLogic.close(s, a)
        assertEquals(active, s.activeId)
    }

    @Test
    fun minimizeActivatesAnotherWindow() {
        var s = open(open(WindowsState(), "a"), "b")
        s = WindowLogic.minimize(s, s.activeId!!)
        assertEquals("a", s.windows.single { it.id == s.activeId }.appId)
        assertTrue(s.windows.last().minimized)
    }

    @Test
    fun minimizingOnlyWindowLeavesNoActive() {
        var s = open(WindowsState(), "a")
        s = WindowLogic.minimize(s, s.activeId!!)
        assertNull(s.activeId)
    }

    @Test
    fun taskbarClickCyclesRestoreMinimizeFocus() {
        var s = open(open(WindowsState(), "a"), "b")
        val a = s.windows[0].id
        val b = s.windows[1].id
        s = WindowLogic.taskbarClick(s, a) // a nieaktywne -> na wierzch
        assertEquals(a, s.activeId)
        assertEquals(a, s.windows.last().id)
        s = WindowLogic.taskbarClick(s, a) // aktywne -> minimalizuj
        assertTrue(s.byId(a)!!.minimized)
        assertEquals(b, s.activeId)
        s = WindowLogic.taskbarClick(s, a) // zminimalizowane -> przywróć
        assertFalse(s.byId(a)!!.minimized)
        assertEquals(a, s.activeId)
    }

    @Test
    fun toggleMaximizeFlipsAndFocuses() {
        var s = open(open(WindowsState(), "a"), "b")
        val a = s.windows[0].id
        val before = s.byId(a)!!.maximized
        s = WindowLogic.toggleMaximize(s, a)
        assertEquals(!before, s.byId(a)!!.maximized)
        assertEquals(a, s.activeId)
    }

    @Test
    fun moveIgnoredWhenMaximized() {
        var s = open(WindowsState(), "a")
        val id = s.activeId!!
        val before = s.byId(id)!!
        s = WindowLogic.move(s, id, 50f, 50f, aw, ah)
        assertEquals(before, s.byId(id))
    }

    @Test
    fun moveClampsSoTitleBarStaysReachable() {
        var s = WindowLogic.open(WindowsState(), "a", "a", true, aw, ah, startMaximized = false)
        val id = s.activeId!!
        s = WindowLogic.move(s, id, 100000f, 100000f, aw, ah)
        val w = s.byId(id)!!
        assertEquals(aw - WindowLogic.MIN_VISIBLE, w.x, 0.001f)
        assertEquals(ah - WindowLogic.TITLE_H, w.y, 0.001f)
        s = WindowLogic.move(s, id, -100000f, -100000f, aw, ah)
        val w2 = s.byId(id)!!
        assertEquals(WindowLogic.MIN_VISIBLE - w2.w, w2.x, 0.001f)
        assertEquals(0f, w2.y, 0.001f)
    }

    @Test
    fun unknownIdIsNoOp() {
        val s = open(WindowsState(), "a")
        assertEquals(s, WindowLogic.close(s, 999))
        assertEquals(s, WindowLogic.focus(s, 999))
        assertEquals(s, WindowLogic.minimize(s, 999))
        assertEquals(s, WindowLogic.taskbarClick(s, 999))
        assertEquals(s, WindowLogic.toggleMaximize(s, 999))
    }
}
