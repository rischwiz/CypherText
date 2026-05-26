package com.richapps.cyphertext.security

import android.util.Base64
import android.util.Log
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object MessageEncryption {

    private const val TAG = "MessageEncryption"
    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val KEY_SIZE = 256
    private const val IV_SIZE = 12
    private const val TAG_SIZE = 128

    fun encrypt(plaintext: String, sharedSecret: ByteArray): String? {
        return try {
            val iv = ByteArray(IV_SIZE)
            java.security.SecureRandom().nextBytes(iv)

            val secretKey = SecretKeySpec(sharedSecret, "AES")

            val cipher = Cipher.getInstance(ALGORITHM)
            val parameterSpec = GCMParameterSpec(TAG_SIZE, iv)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec)

            val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))

            val combined = iv + ciphertext
            val encoded = Base64.encodeToString(combined, Base64.NO_WRAP)

            Log.d(TAG, "Message encrypted successfully")
            encoded

        } catch (e: Exception) {
            Log.e(TAG, "Encryption failed: ${e.message}")
            null
        }
    }

    fun decrypt(encryptedData: String, sharedSecret: ByteArray): String? {
        return try {
            val combined = Base64.decode(encryptedData, Base64.NO_WRAP)

            val iv = combined.copyOfRange(0, IV_SIZE)
            val ciphertext = combined.copyOfRange(IV_SIZE, combined.size)

            val secretKey = SecretKeySpec(sharedSecret, "AES")

            val cipher = Cipher.getInstance(ALGORITHM)
            val parameterSpec = GCMParameterSpec(TAG_SIZE, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec)

            val plaintext = cipher.doFinal(ciphertext)

            Log.d(TAG, "Message decrypted successfully")
            String(plaintext, Charsets.UTF_8)

        } catch (e: Exception) {
            Log.e(TAG, "Decryption failed: ${e.message}")
            null
        }
    }
}