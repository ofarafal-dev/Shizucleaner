package dev.ofarafal.enklawa.session

/** Automatyczna blokada sejfu po dłuższym pobycie aplikacji w tle. Czysta logika, testowalna na JVM. */
object AutoLock {
    const val DEFAULT_LIMIT_MS = 3 * 60_000L

    /**
     * @param stoppedAt czas (elapsedRealtime), gdy aplikacja zeszła w tło; null, gdy nie była w tle
     * @param now bieżący czas (elapsedRealtime)
     */
    fun shouldLock(stoppedAt: Long?, now: Long, limitMs: Long = DEFAULT_LIMIT_MS): Boolean =
        stoppedAt != null && now - stoppedAt >= limitMs
}
