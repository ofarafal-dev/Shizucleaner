package dev.ofarafal.enklawa

import android.app.Application
import dev.ofarafal.enklawa.session.WorldSession
import dev.ofarafal.enklawa.vault.AndroidKeystoreWrapper
import dev.ofarafal.enklawa.vault.VaultStore
import java.io.File

class EnklawaApp : Application() {

    /** Katalog sesji w noBackup — poza kopią zapasową systemu. */
    val sessionParent: File get() = File(noBackupFilesDir, "swiaty").apply { mkdirs() }

    /** Sejfy (tryb z pamięcią) leżą w filesDir, nie w katalogu sesji, więc sprzątanie pozostałości ich nie dotyka. */
    val vaults: VaultStore by lazy {
        VaultStore(File(filesDir, "sejfy").apply { mkdirs() }, { id -> AndroidKeystoreWrapper("enklawa-vault-$id") })
    }

    override fun onCreate() {
        super.onCreate()
        // Pozostałości po zabitym procesie (crash, wymuszone zatrzymanie) kasujemy przy starcie.
        WorldSession.sweepLeftovers(sessionParent)
    }
}
