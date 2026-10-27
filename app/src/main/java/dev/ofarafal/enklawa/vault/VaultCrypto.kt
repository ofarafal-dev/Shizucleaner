package dev.ofarafal.enklawa.vault

import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.params.Argon2Parameters
import java.nio.CharBuffer
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/** Parametry Argon2id zapisywane w nagłówku sejfu (pamięć w KiB). */
data class KdfParams(val memoryKiB: Int, val iterations: Int, val parallelism: Int) {
    companion object {
        val DEFAULT = KdfParams(memoryKiB = 32 * 1024, iterations = 3, parallelism = 1)
    }
}

/** Opakowanie klucza kluczem urządzenia (Android Keystore). W testach JVM zastępowane atrapą. */
interface DeviceKeyWrapper {
    fun wrap(plain: ByteArray): ByteArray

    /** Rzuca wyjątek, gdy klucz urządzenia zniknął lub dane są uszkodzone. */
    fun unwrap(wrapped: ByteArray): ByteArray

    fun delete()
}

internal object VaultCrypto {
    const val NONCE = 12
    const val TAG_BITS = 128
    private val AAD = "enklawa-vault-v1".toByteArray()

    fun deriveKek(password: CharArray, salt: ByteArray, p: KdfParams): ByteArray {
        val bb = Charsets.UTF_8.encode(CharBuffer.wrap(password))
        val pw = ByteArray(bb.remaining()).also { bb.get(it) }
        try {
            val gen = Argon2BytesGenerator()
            gen.init(
                Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
                    .withVersion(Argon2Parameters.ARGON2_VERSION_13)
                    .withIterations(p.iterations)
                    .withMemoryAsKB(p.memoryKiB)
                    .withParallelism(p.parallelism)
                    .withSalt(salt)
                    .build(),
            )
            val out = ByteArray(32)
            gen.generateBytes(pw, out)
            return out
        } finally {
            pw.fill(0)
        }
    }

    fun seal(kek: ByteArray, master: ByteArray, random: SecureRandom): ByteArray {
        val nonce = ByteArray(NONCE).also(random::nextBytes)
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.ENCRYPT_MODE, SecretKeySpec(kek, "AES"), GCMParameterSpec(TAG_BITS, nonce))
        c.updateAAD(AAD)
        return nonce + c.doFinal(master)
    }

    /** Rzuca AEADBadTagException (GeneralSecurityException) przy złym haśle lub uszkodzonych danych. */
    fun open(kek: ByteArray, sealed: ByteArray): ByteArray {
        require(sealed.size > NONCE + TAG_BITS / 8) { "Uszkodzony nagłówek" }
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.DECRYPT_MODE, SecretKeySpec(kek, "AES"), GCMParameterSpec(TAG_BITS, sealed.copyOfRange(0, NONCE)))
        c.updateAAD(AAD)
        return c.doFinal(sealed, NONCE, sealed.size - NONCE)
    }

    fun hex(b: ByteArray): String = b.joinToString("") { "%02x".format(it) }
    fun unhex(s: String): ByteArray = ByteArray(s.length / 2) { s.substring(it * 2, it * 2 + 2).toInt(16).toByte() }
}
