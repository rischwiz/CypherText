package com.richapps.cyphertext.security

import android.content.Context
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager

object TinkManager {
    private const val KEYSET_NAME = "cyphertext_message_keyset"
    private const val PREF_FILE = "cyphertext_tink_prefs"
    private const val MASTER_KEY_URI = "android-keystore://cyphertext_tink_master_key"

    private var aead: Aead? = null

    fun initialize(context: Context) {
        AeadConfig.register()

        val keysetHandle: KeysetHandle = AndroidKeysetManager.Builder()
            .withSharedPref(context, KEYSET_NAME, PREF_FILE)
            .withKeyTemplate(KeyTemplates.get("AES256_GCM"))
            .withMasterKeyUri(MASTER_KEY_URI)
            .build()
            .keysetHandle

        aead = keysetHandle.getPrimitive(Aead::class.java)
    }

    fun encrypt(plaintext: ByteArray, associatedData: ByteArray = ByteArray(0)): ByteArray {
        return aead?.encrypt(plaintext, associatedData)
            ?: throw IllegalStateException("TinkManager not initialized")
    }

    fun decrypt(ciphertext: ByteArray, associatedData: ByteArray = ByteArray(0)): ByteArray {
        return aead?.decrypt(ciphertext, associatedData)
            ?: throw IllegalStateException("TinkManager not initialized")
    }
}