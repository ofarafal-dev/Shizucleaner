package dev.ofarafal.enklawa.vault

import dev.ofarafal.enklawa.crypto.SessionKey
import org.json.JSONObject
import java.io.File
import java.security.GeneralSecurityException
import java.security.SecureRandom

data class VaultInfo(val id: String, val name: String)

sealed interface UnlockResult {
    data class Ok(val key: SessionKey) : UnlockResult
    data object WrongPassword : UnlockResult
    data object DeviceKeyLost : UnlockResult
    data object Damaged : UnlockResult
}

/**
 * Wiele sejfów, każdy to osobny świat: katalog <id>/ z nagłówkiem (header.json) i danymi (data/).
 * Klucz główny jest losowy; w nagłówku leży tylko zaszyfrowany: Argon2id(hasło) → AES-GCM → klucz z Android Keystore.
 * Samo hasło nigdzie się nie zapisuje.
 */
class VaultStore(
    private val root: File,
    private val wrapperFor: (String) -> DeviceKeyWrapper,
    private val params: KdfParams = KdfParams.DEFAULT,
    private val random: SecureRandom = SecureRandom(),
) {
    fun dataDir(id: String): File = File(File(root, id), "data")
    fun vaultDir(id: String): File = File(root, id)

    fun list(): List<VaultInfo> =
        root.listFiles { f -> f.isDirectory }?.mapNotNull { dir ->
            runCatching {
                val h = JSONObject(File(dir, HEADER).readText())
                VaultInfo(dir.name, h.getString("name"))
            }.getOrNull()
        }?.sortedBy { it.name.lowercase() } ?: emptyList()

    fun create(name: String, password: CharArray): VaultInfo {
        val clean = name.trim()
        require(clean.isNotEmpty() && clean.length <= MAX_NAME) { "Nazwa sejfu: 1–$MAX_NAME znaków" }
        require(password.size >= MIN_PASSWORD) { "Hasło: co najmniej $MIN_PASSWORD znaków" }
        val id = VaultCrypto.hex(ByteArray(8).also(random::nextBytes))
        val salt = ByteArray(16).also(random::nextBytes)
        val master = ByteArray(SessionKey.SIZE_BYTES).also(random::nextBytes)
        val kek = VaultCrypto.deriveKek(password, salt, params)
        try {
            val inner = VaultCrypto.seal(kek, master, random)
            val outer = wrapperFor(id).wrap(inner)
            val header = JSONObject()
                .put("v", 1)
                .put("name", clean)
                .put("salt", VaultCrypto.hex(salt))
                .put("mem", params.memoryKiB)
                .put("iter", params.iterations)
                .put("par", params.parallelism)
                .put("wrapped", VaultCrypto.hex(outer))
            val dir = vaultDir(id).apply { mkdirs() }
            val tmp = File(dir, "$HEADER.tmp")
            tmp.writeText(header.toString())
            check(tmp.renameTo(File(dir, HEADER))) { "Nie można zapisać nagłówka" }
            dataDir(id).mkdirs()
        } finally {
            master.fill(0)
            kek.fill(0)
        }
        return VaultInfo(id, clean)
    }

    fun unlock(id: String, password: CharArray): UnlockResult {
        val h = runCatching { JSONObject(File(vaultDir(id), HEADER).readText()) }.getOrNull()
            ?: return UnlockResult.Damaged
        val parsed = runCatching {
            Triple(
                VaultCrypto.unhex(h.getString("salt")),
                KdfParams(h.getInt("mem"), h.getInt("iter"), h.getInt("par")),
                VaultCrypto.unhex(h.getString("wrapped")),
            )
        }.getOrNull() ?: return UnlockResult.Damaged
        val (salt, kp, outer) = parsed

        val inner = try {
            wrapperFor(id).unwrap(outer)
        } catch (e: Exception) {
            return UnlockResult.DeviceKeyLost
        }
        val kek = VaultCrypto.deriveKek(password, salt, kp)
        return try {
            val master = VaultCrypto.open(kek, inner)
            val key = SessionKey.of(master)
            master.fill(0)
            UnlockResult.Ok(key)
        } catch (e: GeneralSecurityException) {
            UnlockResult.WrongPassword
        } catch (e: IllegalArgumentException) {
            UnlockResult.Damaged
        } finally {
            kek.fill(0)
        }
    }

    /** Usuwa sejf: najpierw klucz urządzenia (po tym dane są bezpowrotnie nieczytelne), potem pliki. */
    fun delete(id: String) {
        runCatching { wrapperFor(id).delete() }
        dev.ofarafal.enklawa.crypto.SessionVault.deleteRecursively(vaultDir(id))
    }

    companion object {
        const val HEADER = "header.json"
        const val MAX_NAME = 40
        const val MIN_PASSWORD = 8
    }
}
