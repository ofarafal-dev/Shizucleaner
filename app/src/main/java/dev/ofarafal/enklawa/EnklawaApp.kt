package dev.ofarafal.enklawa

import android.app.Application
import dev.ofarafal.enklawa.session.WorldSession
import java.io.File

class EnklawaApp : Application() {

    /** Katalog sesji w noBackup — poza kopią zapasową systemu. */
    val sessionParent: File get() = File(noBackupFilesDir, "swiaty").apply { mkdirs() }

    override fun onCreate() {
        super.onCreate()
        // Pozostałości po zabitym procesie (crash, wymuszone zatrzymanie) kasujemy przy starcie.
        WorldSession.sweepLeftovers(sessionParent)
    }
}
