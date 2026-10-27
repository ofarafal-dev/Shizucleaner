package dev.ofarafal.enklawa.crypto

import java.security.SecureRandom
import javax.crypto.spec.SecretKeySpec

/**
 * Losowy klucz sesji AES-256 trzymany wyłącznie w RAM. Nigdy nie trafia na dysk ani do Keystore.
 * [wipe] zeruje bajty; po tym wywołaniu [bytes] i [asKey] rzucają wyjątek.
 *
 * Uczciwie: zerowanie tablicy w JVM nie gwarantuje braku kopii w pamięci (GC, swap, zrzut procesu),
 * ale usuwa jedyną referencję, której używa aplikacja. Właściwą ochroną jest to, że klucza nigdy
 * nie zapisujemy, więc po zabiciu procesu dane na dysku są nie do odszyfrowania.
 */
class SessionKey private constructor(private var material: ByteArray?) {

    val isWiped: Boolean get() = material == null

    @Synchronized
    fun bytes(): ByteArray = material?.copyOf() ?: error("Klucz sesji został zniszczony")

    @Synchronized
    fun asKey(): SecretKeySpec = SecretKeySpec(
        material ?: error("Klucz sesji został zniszczony"), "AES"
    )

    @Synchronized
    fun wipe() {
        material?.fill(0)
        material = null
    }

    companion object {
        const val SIZE_BYTES = 32

        fun generate(random: SecureRandom = SecureRandom()): SessionKey {
            val b = ByteArray(SIZE_BYTES)
            random.nextBytes(b)
            return SessionKey(b)
        }

        /** Tylko do testów i do przyszłego trybu z pamięcią (klucz wyprowadzony z hasła). */
        fun of(bytes: ByteArray): SessionKey {
            require(bytes.size == SIZE_BYTES) { "Klucz musi mieć $SIZE_BYTES bajtów" }
            return SessionKey(bytes.copyOf())
        }
    }
}
