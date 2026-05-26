package com.richapps.cyphertext.security

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import androidx.annotation.RequiresApi
import org.signal.libsignal.protocol.IdentityKeyPair
import org.signal.libsignal.protocol.ecc.Curve
import org.signal.libsignal.protocol.ecc.ECKeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore

object KeyStoreManager {
    private const val TAG = "KeyStoreManager"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val IDENTITY_KEY_ALIAS = "cyphertext_identity_key"

    @RequiresApi(Build.VERSION_CODES.S)
    fun generateIdentityKeyPair() { // Generates identity key on first launch
        if (identityKeyExists()) return

        val keyPairGenerator = KeyPairGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_EC,
            ANDROID_KEYSTORE
        )

            val parameterSpec = KeyGenParameterSpec.Builder(
                IDENTITY_KEY_ALIAS,
                KeyProperties.PURPOSE_AGREE_KEY
            )

                .setAlgorithmParameterSpec(java.security.spec.ECGenParameterSpec("secp256r1"))
                .build()

        keyPairGenerator.initialize(parameterSpec)
        keyPairGenerator.generateKeyPair()
        Log.d(TAG, "Identity key pair generated")
    }

    fun identityKeyExists() : Boolean {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null)
        return keyStore.containsAlias(IDENTITY_KEY_ALIAS)
    }

    fun getIdentityPublicKey(): ByteArray {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null)
        val publicKey = keyStore.getCertificate(IDENTITY_KEY_ALIAS).publicKey
        return publicKey.encoded
    }

    fun generateAndSaveSignedPreKey(context: Context): ECKeyPair {
        val keyPair = Curve.generateKeyPair()
        LocalKeyStore.saveSignedPreKeyPair(context, keyPair)
        return keyPair
    }


    fun generateAndSaveOneTimePreKeys(context: Context, count: Int = 10): List<ECKeyPair> {
        val keyPairs = (1..count).map { Curve.generateKeyPair() }
        LocalKeyStore.saveOneTimePreKeyPairs(context, keyPairs)
        return keyPairs
    }

}