package dev.ofarafal.enklawa.session

import dev.ofarafal.enklawa.crypto.SessionKey
import dev.ofarafal.enklawa.crypto.SessionVault
import java.io.File
import java.security.SecureRandom

/**
 * Jedna sesja świata w trybie amnezji. Tworzy klucz w RAM i szyfrowany magazyn.
 * [end] jest idempotentne: niszczy klucz, kasuje magazyn i odpala wszystkie zarejestrowane sprzątacze
 * (np. usunięcie profilu przeglądarki). Błąd jednego sprzątacza nie przerywa pozostałych.
 */
class WorldSession private constructor(
    val root: File,
    val key: SessionKey,
    val vault: SessionVault,
    private val persistent: Boolean = false,
) {
    private val cleanups = mutableListOf<() -> Unit>()
    private var ended = false

    val isEnded: Boolean @Synchronized get() = ended

    @Synchronized
    fun onEnd(cleanup: () -> Unit) {
        if (ended) runCatching(cleanup) else cleanups += cleanup
    }

    /** Zwraca liczbę sprzątaczy, które zakończyły się błędem. */
    @Synchronized
    fun end(): Int {
        if (ended) return 0
        ended = true
        var failures = 0
        cleanups.asReversed().forEach { if (runCatching(it).isFailure) failures++ }
        cleanups.clear()
        if (persistent) {
            // Sejf trwały: tylko zamykamy (klucz znika z RAM), pliki zostają.
            if (runCatching { vault.lock() }.isFailure) failures++
            key.wipe()
            return failures
        }
        if (runCatching { vault.destroy() }.isFailure) failures++
        key.wipe()
        SessionVault.deleteRecursively(root)
        return failures
    }

    companion object {
        const val ROOT_PREFIX = "swiat-"

        /** Katalog sesji leży w noBackup, żeby kopia zapasowa systemu go nie zabrała. */
        fun start(parent: File, random: SecureRandom = SecureRandom()): WorldSession {
            sweepLeftovers(parent)
            val id = ByteArray(8).also(random::nextBytes).joinToString("") { "%02x".format(it) }
            val root = File(parent, ROOT_PREFIX + id).apply { mkdirs() }
            val key = SessionKey.generate(random)
            return WorldSession(root, key, SessionVault(File(root, "vault"), key, random))
        }

        /** Otwiera istniejący sejf (klucz z hasła). Katalog sejfu nie podlega sprzątaniu pozostałości. */
        fun openVault(dataDir: File, key: SessionKey, random: SecureRandom = SecureRandom()): WorldSession =
            WorldSession(dataDir, key, SessionVault(dataDir, key, random, persistent = true), persistent = true)

        /**
         * Po zabiciu procesu (crash, wymuszone zamknięcie) mogły zostać katalogi poprzedniej sesji.
         * Ich klucz zniknął z RAM, więc są nieczytelne, ale i tak je usuwamy. Zwraca liczbę usuniętych.
         */
        fun sweepLeftovers(parent: File): Int {
            var n = 0
            parent.listFiles { f -> f.name.startsWith(ROOT_PREFIX) }?.forEach {
                SessionVault.deleteRecursively(it); n++
            }
            return n
        }
    }
}
