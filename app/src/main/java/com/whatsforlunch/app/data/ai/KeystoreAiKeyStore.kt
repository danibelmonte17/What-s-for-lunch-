package com.whatsforlunch.app.data.ai

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.core.content.edit
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

// Almacén cifrado de la API key (D2, ⚠ S05): clave AES-256 del Android Keystore
// (alias whatsforlunch_ai_key) y ciphertext AES/GCM (IV + dato, Base64) en
// SharedPreferences propias. La key en claro solo existe en memoria al consumirla;
// nunca se escribe en disco, logs ni excepciones (RK01).
class KeystoreAiKeyStore(context: Context) : AiKeyStore {

    private val preferences = context.applicationContext
        .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override fun apiKey(): String? {
        val stored = preferences.getString(KEY_API_KEY, null) ?: return null
        return runCatching { decrypt(stored) }.getOrNull()?.takeIf { it.isNotEmpty() }
    }

    // Escritura síncrona (edit(commit = true)) a propósito: la key debe quedar
    // persistida antes de que la UI confirme el guardado o el borrado.
    override fun save(key: String) {
        val encrypted = encrypt(key)
        preferences.edit(commit = true) { putString(KEY_API_KEY, encrypted) }
    }

    override fun clear() {
        preferences.edit(commit = true) { remove(KEY_API_KEY) }
    }

    private fun encrypt(value: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val ciphertext = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        val payload = cipher.iv + ciphertext
        return Base64.encodeToString(payload, Base64.NO_WRAP)
    }

    private fun decrypt(stored: String): String {
        val payload = Base64.decode(stored, Base64.NO_WRAP)
        require(payload.size > IV_LENGTH_BYTES) { "Invalid encrypted payload" }
        val iv = payload.copyOfRange(0, IV_LENGTH_BYTES)
        val ciphertext = payload.copyOfRange(IV_LENGTH_BYTES, payload.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(TAG_LENGTH_BITS, iv))
        return String(cipher.doFinal(ciphertext), Charsets.UTF_8)
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existing = runCatching { keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry }
            .getOrNull()
        existing?.secretKey?.let { return it }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(KEY_SIZE_BITS)
                .build(),
        )
        return generator.generateKey()
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "whatsforlunch_ai_key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val KEY_SIZE_BITS = 256
        const val TAG_LENGTH_BITS = 128
        const val IV_LENGTH_BYTES = 12
        const val PREFERENCES_NAME = "ai_key_prefs"
        const val KEY_API_KEY = "api_key"
    }
}
