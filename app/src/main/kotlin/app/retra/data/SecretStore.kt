package app.retra.data

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Small encrypted store for API keys. Values are AES-256-GCM encrypted with a key that lives in the Android
 * Keystore (it never leaves the secure hardware where there is any), so `secrets.json` is useless off-device.
 * The files are also excluded from backups. Keys never go in settings.json.
 */
class SecretStore(root: File) {
    private val file = File(root, "secrets.json")
    private val lock = Any()

    private fun key(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        gen.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return gen.generateKey()
    }

    private fun readAll(): MutableMap<String, String> = runCatching {
        if (!file.exists()) return@runCatching mutableMapOf()
        file.readLines().mapNotNull { l -> l.split('=', limit = 2).takeIf { it.size == 2 }?.let { it[0] to it[1] } }.toMap().toMutableMap()
    }.getOrDefault(mutableMapOf())

    private fun writeAll(m: Map<String, String>) {
        val tmp = File(file.parentFile, "secrets.tmp")
        tmp.writeText(m.entries.joinToString("\n") { "${it.key}=${it.value}" })
        tmp.renameTo(file)
    }

    fun get(name: String): String? = synchronized(lock) {
        val packed = readAll()[name] ?: return null
        runCatching {
            val raw = Base64.decode(packed, Base64.NO_WRAP)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, raw.copyOfRange(0, IV_BYTES)))
            String(cipher.doFinal(raw, IV_BYTES, raw.size - IV_BYTES), Charsets.UTF_8)
        }.getOrNull()
    }

    /** Returns false when the device can't encrypt (no secure keystore); nothing is stored then. */
    fun put(name: String, value: String): Boolean = synchronized(lock) {
        runCatching {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key())
            val enc = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
            val m = readAll()
            m[name] = Base64.encodeToString(cipher.iv + enc, Base64.NO_WRAP)
            writeAll(m)
        }.isSuccess
    }

    fun remove(name: String) = synchronized(lock) { writeAll(readAll().also { it.remove(name) }) }

    fun has(name: String) = synchronized(lock) { readAll().containsKey(name) }

    companion object {
        private const val ALIAS = "retra_secrets_v1"
        private const val IV_BYTES = 12
        const val OPENROUTER = "openrouter_api_key"
    }
}
