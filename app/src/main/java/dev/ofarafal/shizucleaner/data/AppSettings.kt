package dev.ofarafal.shizucleaner.data

import android.content.Context

class AppSettings(ctx: Context) {

    private val prefs = ctx.getSharedPreferences("settings", Context.MODE_PRIVATE)

    /** Liczba przebiegów nadpisywania przed skasowaniem pliku (0 = zwykłe kasowanie). */
    var overwritePasses: Int
        get() = prefs.getInt(KEY_PASSES, 1).coerceIn(0, 3)
        set(value) = prefs.edit().putInt(KEY_PASSES, value.coerceIn(0, 3)).apply()

    private companion object {
        const val KEY_PASSES = "overwrite_passes"
    }
}
