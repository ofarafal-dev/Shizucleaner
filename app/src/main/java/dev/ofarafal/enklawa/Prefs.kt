package dev.ofarafal.enklawa

import android.content.Context

/** Proste ustawienia aplikacji (nic wrażliwego; allowBackup=false, więc poza kopią zapasową). */
object Prefs {
    private const val FILE = "enklawa_prefs"
    private const val KEY_AUTO_UPDATE = "auto_update_check"

    private fun sp(ctx: Context) = ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun autoUpdateCheck(ctx: Context): Boolean = sp(ctx).getBoolean(KEY_AUTO_UPDATE, true)

    fun setAutoUpdateCheck(ctx: Context, value: Boolean) {
        sp(ctx).edit().putBoolean(KEY_AUTO_UPDATE, value).apply()
    }
}
