package com.richapps.cyphertext.network

import android.content.Context
import android.util.Base64
import android.util.Log
import com.richapps.cyphertext.Utils
import com.richapps.cyphertext.security.KeyStoreManager
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object KeyBundleUploader {

    private const val TAG = "KeyBundleUploader"

    fun uploadOneTimePreKeys(userId: Int, context: Context) {
        try {
            // Generate and save private keys locally
            val oneTimePreKeys = KeyStoreManager.generateAndSaveOneTimePreKeys(context, 10)

            val oneTimePreKeysJson = JSONArray()
            oneTimePreKeys.forEachIndexed { index, keyPair ->
                val keyJson = JSONObject().apply {
                    put("keyId", index + 1)
                    put("publicKey", Base64.encodeToString(
                        keyPair.publicKey.serialize(), Base64.NO_WRAP
                    ))
                }
                oneTimePreKeysJson.put(keyJson)
            }

            val url = URL("${Utils.SERVER_URL}/keys/onetimeprekeys")
            val connection = url.openConnection() as HttpURLConnection
            connection.apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
                connectTimeout = 5000
                readTimeout = 5000
            }

            val body = JSONObject().apply {
                put("userId", userId)
                put("oneTimePreKeys", oneTimePreKeysJson)
            }

            connection.outputStream.use { os ->
                os.write(body.toString().toByteArray())
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                Log.d(TAG, "One-time prekeys uploaded successfully")
            } else {
                Log.e(TAG, "Failed to upload one-time prekeys: $responseCode")
            }
            connection.disconnect()

        } catch (e: Exception) {
            Log.e(TAG, "Error uploading one-time prekeys: ${e.message}")
        }
    }
}