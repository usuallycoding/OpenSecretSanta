package com.usuallycoding.opensecretsanta.data.session

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Keeps the access token encrypted with an AES-GCM key that lives in the Android Keystore and never
 * leaves it; only the ciphertext and its IV are written, to a private preferences file that backups
 * exclude. Anything that goes wrong (key gone, data from another device, corrupt value) is treated
 * as "no stored token", so a broken store can only ever cost the person a sign-in.
 */
class AndroidTokenStore(context: Context) : TokenStore {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override suspend fun read(): String? = try {
        val packed = prefs.getString(KEY_TOKEN, null)?.let { Base64.decode(it, Base64.NO_WRAP) }
        if (packed == null || packed.size <= IV_BYTES) {
            null
        } else {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(TAG_BITS, packed, 0, IV_BYTES))
            cipher.doFinal(packed, IV_BYTES, packed.size - IV_BYTES).decodeToString()
        }
    } catch (_: Exception) {
        clear()
        null
    }

    override suspend fun write(token: String) {
        try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key())
            val packed = cipher.iv + cipher.doFinal(token.encodeToByteArray())
            prefs.edit().putString(KEY_TOKEN, Base64.encodeToString(packed, Base64.NO_WRAP)).commit()
        } catch (_: Exception) {
            // Not stored: the session still works until the app is closed.
        }
    }

    override suspend fun clear() {
        try {
            prefs.edit().remove(KEY_TOKEN).commit()
        } catch (_: Exception) {
            // Nothing more to do.
        }
    }

    private fun key(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "opensecretsanta.session"
        const val PREFS_NAME = "session"
        const val KEY_TOKEN = "token"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_BYTES = 12
        const val TAG_BITS = 128
    }
}
