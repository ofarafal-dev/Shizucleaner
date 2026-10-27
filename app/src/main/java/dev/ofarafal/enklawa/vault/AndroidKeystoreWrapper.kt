package dev.ofarafal.enklawa.vault

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Druga warstwa ochrony: klucz sejfu (już zaszyfrowany kluczem z hasła) jest dodatkowo szyfrowany kluczem AES
 * z Android Keystore, którego nie da się wyciągnąć z urządzenia. Skopiowany plik sejfu nie otworzy się
 * na innym telefonie nawet ze znanym hasłem. Cena: po utracie klucza Keystore (reset fabryczny, niektóre
 * przywracania systemu) sejf jest nie do odzyskania.
 */
class AndroidKeystoreWrapper(private val alias: String) : DeviceKeyWrapper {

    private fun store() = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    private fun key(): SecretKey {
        (store().getKey(alias, null) as? SecretKey)?.let { return it }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        gen.init(
            KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return gen.generateKey()
    }

    override fun wrap(plain: ByteArray): ByteArray {
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.ENCRYPT_MODE, key())
        return c.iv + c.doFinal(plain)
    }

    override fun unwrap(wrapped: ByteArray): ByteArray {
        val ks = store()
        check(ks.containsAlias(alias)) { "Klucz urządzenia zniknął" }
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(VaultCrypto.TAG_BITS, wrapped.copyOfRange(0, VaultCrypto.NONCE)))
        return c.doFinal(wrapped, VaultCrypto.NONCE, wrapped.size - VaultCrypto.NONCE)
    }

    override fun delete() {
        runCatching { store().deleteEntry(alias) }
    }
}
