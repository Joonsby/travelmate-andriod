package com.joonsby.travelmate.core.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.joonsby.travelmate.core.model.AuthTokens
import com.joonsby.travelmate.core.network.ApiJson
import com.joonsby.travelmate.core.network.TokenStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 토큰을 Android Keystore 의 AES-GCM 키로 암호화해 파일에 둔다.
 * noBackupFilesDir 에 두므로 클라우드 백업·기기 이전에 따라가지 않는다 (iOS Keychain 의 ThisDeviceOnly 와 같은 뜻).
 * 요청마다 읽으므로 메모리에도 들고 있다.
 */
@Singleton
class KeystoreTokenStorage @Inject constructor(
    @ApplicationContext context: Context,
) : TokenStorage {
    private val file = File(context.noBackupFilesDir, "auth_tokens.bin")
    private val lock = Any()
    @Volatile private var cached: AuthTokens? = null
    @Volatile private var loaded = false

    override fun load(): AuthTokens? {
        if (loaded) return cached
        synchronized(lock) {
            if (!loaded) {
                cached = runCatching { decrypt(file.readBytes()) }.getOrNull()
                loaded = true
            }
            return cached
        }
    }

    override fun save(tokens: AuthTokens) = synchronized(lock) {
        val bytes = encrypt(ApiJson.encodeToString(AuthTokens.serializer(), tokens).toByteArray())
        val temp = File(file.parentFile, file.name + ".tmp")
        temp.writeBytes(bytes)
        if (!temp.renameTo(file)) {
            file.delete()
            temp.renameTo(file)
        }
        cached = tokens
        loaded = true
    }

    override fun clear() = synchronized(lock) {
        file.delete()
        cached = null
        loaded = true
    }

    private fun encrypt(plain: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        return byteArrayOf(cipher.iv.size.toByte()) + cipher.iv + cipher.doFinal(plain)
    }

    private fun decrypt(stored: ByteArray): AuthTokens {
        val ivSize = stored[0].toInt()
        val iv = stored.copyOfRange(1, 1 + ivSize)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
        val plain = cipher.doFinal(stored.copyOfRange(1 + ivSize, stored.size))
        return ApiJson.decodeFromString(AuthTokens.serializer(), plain.decodeToString())
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
        const val KEY_ALIAS = "com.joonsby.travelmate.auth"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
