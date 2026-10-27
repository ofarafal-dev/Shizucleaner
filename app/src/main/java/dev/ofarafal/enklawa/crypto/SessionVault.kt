package dev.ofarafal.enklawa.crypto

import java.io.File
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec

/**
 * Szyfrowany magazyn plików świata. Każdy plik: [12 B nonce][szyfrogram AES-256-GCM + tag].
 * Nazwa logiczna trafia do AAD, więc zamiana zawartości między plikami jest wykrywana.
 * Nazwy plików na dysku są losowe (hex), więc nie zdradzają, co zapisano.
 *
 * [destroy] realizuje crypto-erase: niszczy klucz (po tym nic nie da się odczytać), a następnie
 * kasuje pliki. Kasowanie to tylko porządek; bezpieczeństwo na flashu opiera się na zniszczeniu klucza,
 * nie na nadpisywaniu.
 */
class SessionVault(
    private val dir: File,
    private val key: SessionKey,
    private val random: SecureRandom = SecureRandom(),
    /** Sejf trwały: indeks nazw (zaszyfrowany tym samym kluczem) leży na dysku i wraca po ponownym otwarciu. */
    private val persistent: Boolean = false,
) {
    private val index = LinkedHashMap<String, String>() // nazwa logiczna -> nazwa pliku na dysku

    init {
        dir.mkdirs()
        if (persistent) loadIndex()
    }

    private fun saveIndex() {
        val json = org.json.JSONObject()
        index.forEach { (k, v) -> json.put(k, v) }
        val nonce = ByteArray(NONCE_SIZE).also(random::nextBytes)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key.asKey(), GCMParameterSpec(TAG_BITS, nonce))
        cipher.updateAAD(INDEX_AAD)
        val tmp = File(dir, "$INDEX_FILE.tmp")
        tmp.writeBytes(nonce + cipher.doFinal(json.toString().toByteArray(Charsets.UTF_8)))
        check(tmp.renameTo(File(dir, INDEX_FILE))) { "Nie można zapisać indeksu" }
    }

    /** Rzuca wyjątek (AEADBadTagException), gdy klucz jest zły albo indeks uszkodzony. */
    private fun loadIndex() {
        val f = File(dir, INDEX_FILE)
        if (!f.exists()) return
        val raw = f.readBytes()
        require(raw.size > NONCE_SIZE + TAG_BITS / 8) { "Uszkodzony indeks" }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key.asKey(), GCMParameterSpec(TAG_BITS, raw.copyOfRange(0, NONCE_SIZE)))
        cipher.updateAAD(INDEX_AAD)
        val json = org.json.JSONObject(String(cipher.doFinal(raw, NONCE_SIZE, raw.size - NONCE_SIZE), Charsets.UTF_8))
        json.keys().forEach { index[it] = json.getString(it) }
    }

    /** Zamyka sejf trwały: niszczy klucz w RAM, nie rusza plików. */
    @Synchronized
    fun lock() {
        key.wipe()
        index.clear()
    }

    @Synchronized
    fun write(name: String, data: ByteArray) {
        val diskName = index.getOrPut(name) { randomHex(16) }
        val nonce = ByteArray(NONCE_SIZE).also(random::nextBytes)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key.asKey(), GCMParameterSpec(TAG_BITS, nonce))
        cipher.updateAAD(name.toByteArray(Charsets.UTF_8))
        File(dir, diskName).writeBytes(nonce + cipher.doFinal(data))
        if (persistent) saveIndex()
    }

    @Synchronized
    fun read(name: String): ByteArray? {
        val diskName = index[name] ?: return null
        val raw = File(dir, diskName).takeIf { it.exists() }?.readBytes() ?: return null
        require(raw.size > NONCE_SIZE + TAG_BITS / 8) { "Uszkodzony plik" }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.DECRYPT_MODE, key.asKey(),
            GCMParameterSpec(TAG_BITS, raw.copyOfRange(0, NONCE_SIZE))
        )
        cipher.updateAAD(name.toByteArray(Charsets.UTF_8))
        return cipher.doFinal(raw, NONCE_SIZE, raw.size - NONCE_SIZE)
    }

    @Synchronized
    fun names(): List<String> = index.keys.toList()

    @Synchronized
    fun delete(name: String) {
        index.remove(name)?.let { File(dir, it).delete() }
        if (persistent) saveIndex()
    }

    /** Niszczy klucz, potem kasuje wszystko w katalogu magazynu. */
    @Synchronized
    fun destroy() {
        key.wipe()
        index.clear()
        deleteRecursively(dir)
    }

    private fun randomHex(bytes: Int): String =
        ByteArray(bytes).also(random::nextBytes).joinToString("") { "%02x".format(it) }

    companion object {
        const val NONCE_SIZE = 12
        const val TAG_BITS = 128
        private const val INDEX_FILE = "index.enk"
        private val INDEX_AAD = "enklawa-index".toByteArray()

        fun deleteRecursively(f: File) {
            if (f.isDirectory) f.listFiles()?.forEach(::deleteRecursively)
            f.delete()
        }
    }
}
