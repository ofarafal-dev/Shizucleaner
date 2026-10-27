package dev.ofarafal.enklawa.os

/** Okno systemu. Współrzędne i rozmiary w dp; kolejność na liście = kolejność z-order (ostatnie na wierzchu). */
data class Win(
    val id: Int,
    val appId: String,
    val title: String,
    val x: Float,
    val y: Float,
    val w: Float,
    val h: Float,
    val maximized: Boolean = true,
    val minimized: Boolean = false,
)

data class WindowsState(
    val windows: List<Win> = emptyList(),
    val activeId: Int? = null,
    val nextId: Int = 1,
) {
    fun byId(id: Int): Win? = windows.firstOrNull { it.id == id }
}

/** Czysta logika menedżera okien (bez Compose), dzięki temu testowalna na JVM. */
object WindowLogic {
    const val MIN_VISIBLE = 80f
    const val TITLE_H = 32f

    fun open(
        s: WindowsState,
        appId: String,
        title: String,
        singleton: Boolean,
        areaW: Float,
        areaH: Float,
        startMaximized: Boolean = true,
    ): WindowsState {
        if (singleton) {
            s.windows.firstOrNull { it.appId == appId }?.let { return focus(s, it.id) }
        }
        val n = s.windows.size
        val w = (areaW * 0.88f).coerceAtLeast(160f)
        val h = (areaH * 0.72f).coerceAtLeast(160f)
        val off = (n % 6) * 28f
        val x = (16f + off).coerceAtMost((areaW - w).coerceAtLeast(0f))
        val y = (16f + off).coerceAtMost((areaH - h).coerceAtLeast(0f))
        val win = Win(s.nextId, appId, title, x, y, w, h, maximized = startMaximized)
        return s.copy(windows = s.windows + win, activeId = win.id, nextId = s.nextId + 1)
    }

    fun focus(s: WindowsState, id: Int): WindowsState {
        val win = s.byId(id) ?: return s
        return s.copy(windows = s.windows.filter { it.id != id } + win.copy(minimized = false), activeId = id)
    }

    fun close(s: WindowsState, id: Int): WindowsState {
        if (s.byId(id) == null) return s
        val rest = s.windows.filter { it.id != id }
        val active = if (s.activeId == id) rest.lastOrNull { !it.minimized }?.id else s.activeId
        return s.copy(windows = rest, activeId = active)
    }

    fun minimize(s: WindowsState, id: Int): WindowsState {
        if (s.byId(id) == null) return s
        val updated = s.windows.map { if (it.id == id) it.copy(minimized = true) else it }
        val active = if (s.activeId == id) updated.lastOrNull { !it.minimized }?.id else s.activeId
        return s.copy(windows = updated, activeId = active)
    }

    fun toggleMaximize(s: WindowsState, id: Int): WindowsState {
        if (s.byId(id) == null) return s
        val toggled = s.copy(windows = s.windows.map { if (it.id == id) it.copy(maximized = !it.maximized) else it })
        return focus(toggled, id)
    }

    /** Dotknięcie przycisku okna na pasku zadań: przywróć, zminimalizuj aktywne albo wyciągnij na wierzch. */
    fun taskbarClick(s: WindowsState, id: Int): WindowsState {
        val win = s.byId(id) ?: return s
        return when {
            win.minimized -> focus(s, id)
            s.activeId == id -> minimize(s, id)
            else -> focus(s, id)
        }
    }

    /** Przesuwanie za pasek tytułu; okno zawsze zostawia widoczny kawałek paska, żeby dało się je złapać. */
    fun move(s: WindowsState, id: Int, dx: Float, dy: Float, areaW: Float, areaH: Float): WindowsState {
        val win = s.byId(id) ?: return s
        if (win.maximized) return s
        val minX = MIN_VISIBLE - win.w
        val maxX = (areaW - MIN_VISIBLE).coerceAtLeast(minX)
        val maxY = (areaH - TITLE_H).coerceAtLeast(0f)
        val moved = win.copy(
            x = (win.x + dx).coerceIn(minX, maxX),
            y = (win.y + dy).coerceIn(0f, maxY),
        )
        return s.copy(windows = s.windows.map { if (it.id == id) moved else it })
    }
}
