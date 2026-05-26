package com.richapps.cyphertext.security

import android.util.Base64
import android.util.Log
import com.richapps.cyphertext.Utils
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class RecipientKeyBundle(
    val identityKey: ByteArray,
    val signedPreKey: ByteArray,
    val oneTimePreKey: ByteArray?,
    val oneTimePreKeyId: Int?
)

object KeyBundleFetcher {

    private const val TAG = "KeyBundleFetcher"

    fun fetchKeyBundle(recipientUserId: Int): RecipientKeyBundle? {
        return try {
            val url = URL("${Utils.SERVER_URL}/keys/bundle/$recipientUserId")
            val connection = url.openConnection() as HttpURLConnection
            connection.apply {
                requestMethod = "GET"
                setRequestProperty("Content-Type", "application/json")
                connectTimeout = 5000
                readTimeout = 5000
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().readText()
                val json = JSONObject(response)

                Log.d(TAG, "Key bundle fetched for user $recipientUserId")

                RecipientKeyBundle(
                    identityKey = Base64.decode(json.getString("identityKey"), Base64.NO_WRAP),
                    signedPreKey = Base64.decode(json.getString("signedPreKey"), Base64.NO_WRAP),
                    oneTimePreKey = if (json.isNull("oneTimePreKey")) null
                    else Base64.decode(json.getString("oneTimePreKey"), Base64.NO_WRAP),
                    oneTimePreKeyId = if (json.isNull("oneTimePreKeyId")) null
                    else json.getInt("oneTimePreKeyId")
                )
            } else {
                Log.e(TAG, "Failed to fetch key bundle: $responseCode")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching key bundle: ${e.message}")
            null
        }
    }
}