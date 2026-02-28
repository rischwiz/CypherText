package com.richapps.cyphertext
import android.security.keystore.KeyProperties
import java.security.KeyStore

class CryptoManager {
    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply {
        load(null)
    }

    companion object {
        private const val ALGORITHM = KeyProperties.KEY_ALGORITHM_AES
    }
}