package com.richapps.cyphertext.security

import android.content.Context
import android.util.Base64
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.signal.libsignal.protocol.ecc.Curve
import org.signal.libsignal.protocol.ecc.ECKeyPair
import org.signal.libsignal.protocol.ecc.ECPrivateKey

object LocalKeyStore {

    private const val TAG = "LocalKeyStore"
    private const val PREFS_NAME = "cyphertext_local_keys"

    private fun getEncryptedPrefs(context: Context): android.content.SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveSignedPreKeyPair(context: Context, keyPair: ECKeyPair) {
        val prefs = getEncryptedPrefs(context)
        prefs.edit()
            .putString(
                "signed_pre_key_private",
                Base64.encodeToString(keyPair.privateKey.serialize(), Base64.NO_WRAP)
            )
            .putString(
                "signed_pre_key_public",
                Base64.encodeToString(keyPair.publicKey.serialize(), Base64.NO_WRAP)
            )
            .apply()
        Log.d(TAG, "Signed prekey pair saved locally")
    }

    fun saveOneTimePreKeyPairs(context: Context, keyPairs: List<ECKeyPair>) {
        val prefs = getEncryptedPrefs(context)
        val editor = prefs.edit()
        keyPairs.forEachIndexed { index, keyPair ->
            val keyId = index + 1
            editor.putString(
                "otk_private_$keyId",
                Base64.encodeToString(keyPair.privateKey.serialize(), Base64.NO_WRAP)
            )
            editor.putString(
                "otk_public_$keyId",
                Base64.encodeToString(keyPair.publicKey.serialize(), Base64.NO_WRAP)
            )
        }
        editor.apply()
        Log.d(TAG, "One-time prekey pairs saved locally")
    }

    fun getSignedPreKeyPair(context: Context): ECKeyPair? {
        return try {
            val prefs = getEncryptedPrefs(context)
            val privateKeyBytes = Base64.decode(
                prefs.getString("signed_pre_key_private", null) ?: return null,
                Base64.NO_WRAP
            )
            val publicKeyBytes = Base64.decode(
                prefs.getString("signed_pre_key_public", null) ?: return null,
                Base64.NO_WRAP
            )
            ECKeyPair(
                Curve.decodePoint(publicKeyBytes, 0),
                Curve.decodePrivatePoint(privateKeyBytes)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error getting signed prekey pair: ${e.message}")
            null
        }
    }

    fun getAndConsumeOneTimePreKey(context: Context, keyId: Int): ECKeyPair? {
        return try {
            val prefs = getEncryptedPrefs(context)
            val privateKeyBytes = Base64.decode(
                prefs.getString("otk_private_$keyId", null) ?: return null,
                Base64.NO_WRAP
            )
            val publicKeyBytes = Base64.decode(
                prefs.getString("otk_public_$keyId", null) ?: return null,
                Base64.NO_WRAP
            )
            prefs.edit()
                .remove("otk_private_$keyId")
                .remove("otk_public_$keyId")
                .apply()
            Log.d(TAG, "One-time prekey $keyId consumed")
            ECKeyPair(
                Curve.decodePoint(publicKeyBytes, 0),
                Curve.decodePrivatePoint(privateKeyBytes)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error getting one-time prekey: ${e.message}")
            null
        }
    }
}