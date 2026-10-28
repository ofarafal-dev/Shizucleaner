package dev.ofarafal.enklawa.miniapp

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.ofarafal.enklawa.crypto.SessionVault
import org.json.JSONArray
import org.json.JSONObject

/** Zainstalowane mini-aplikacje świata. Wszystko leży w magazynie świata (zaszyfrowanym): w sejfie zostaje, w amnezji znika. */
object MiniAppStore {
    var apps: List<Manifest> by mutableStateOf(emptyList())
        private set

    fun load(vault: SessionVault) {
        apps = MiniApps.installedIds(vault.names()).mapNotNull { id ->
            runCatching { MiniApps.parseManifest(vault.read(MiniApps.storageKey(id, "m"))!!.toString(Charsets.UTF_8)) }.getOrNull()
        }
    }

    fun clear() { apps = emptyList() }

    fun find(id: String): Manifest? = apps.firstOrNull { it.id == id }

    fun install(vault: SessionVault, files: Map<String, ByteArray>): Manifest {
        val m = MiniApps.validate(files)
        val old = find(m.id)
        wipeFiles(vault, m.id)
        files.forEach { (p, b) -> if (p != MiniApps.MANIFEST) vault.write(MiniApps.fileKey(m.id, p), b) }
        vault.write(MiniApps.storageKey(m.id, "m"), files.getValue(MiniApps.MANIFEST))
        // Zmiana żądanych uprawnień unieważnia wcześniejszą zgodę: użytkownik zdecyduje od nowa.
        if (old != null && old.permissions != m.permissions) runCatching { vault.delete(MiniApps.storageKey(m.id, "grants")) }
        apps = (apps.filter { it.id != m.id } + m).sortedBy { it.name.lowercase() }
        return m
    }

    fun uninstall(vault: SessionVault, id: String) {
        vault.names().filter { it.startsWith("mini/$id/") }.forEach { runCatching { vault.delete(it) } }
        apps = apps.filter { it.id != id }
    }

    fun file(vault: SessionVault, id: String, path: String): ByteArray? =
        MiniApps.safePath(path)?.let { runCatching { vault.read(MiniApps.fileKey(id, it)) }.getOrNull() }

    /** null = użytkownik jeszcze nie zdecydował; pusty zbiór = odmówił wszystkiego. */
    fun grants(vault: SessionVault, id: String): Set<Perm>? {
        val b = runCatching { vault.read(MiniApps.storageKey(id, "grants")) }.getOrNull() ?: return null
        return runCatching {
            val arr = JSONArray(b.toString(Charsets.UTF_8))
            (0 until arr.length()).mapNotNull { Perm.fromKey(arr.optString(it)) }.toSet()
        }.getOrNull()
    }

    fun setGrants(vault: SessionVault, id: String, perms: Set<Perm>) {
        val arr = JSONArray().also { a -> perms.forEach { a.put(it.key) } }
        vault.write(MiniApps.storageKey(id, "grants"), arr.toString().toByteArray())
    }

    private fun data(vault: SessionVault, id: String): JSONObject =
        runCatching { JSONObject(vault.read(MiniApps.storageKey(id, "data"))!!.toString(Charsets.UTF_8)) }.getOrDefault(JSONObject())

    fun dataGet(vault: SessionVault, id: String, key: String): String? = data(vault, id).let { if (it.has(key)) it.optString(key) else null }

    /** false, gdy przekroczyłby limit magazynu. */
    fun dataSet(vault: SessionVault, id: String, key: String, value: String?): Boolean {
        val o = data(vault, id)
        if (value == null) o.remove(key) else o.put(key, value)
        val bytes = o.toString().toByteArray(Charsets.UTF_8)
        if (bytes.size > MiniApps.MAX_DATA_BYTES) return false
        vault.write(MiniApps.storageKey(id, "data"), bytes)
        return true
    }

    private fun wipeFiles(vault: SessionVault, id: String) {
        vault.names().filter { it.startsWith("mini/$id/f/") || it == "mini/$id/m" }.forEach { runCatching { vault.delete(it) } }
    }
}
