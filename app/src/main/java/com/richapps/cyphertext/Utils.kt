package com.richapps.cyphertext

import android.content.Context
import android.util.Base64
import com.google.firebase.auth.FirebaseAuth
import com.richapps.cyphertext.security.DoubleRatchet
import org.signal.libsignal.protocol.logging.Log

object Utils {
    const val SERVER_URL = "http://10.0.0.112:3000"
    const val WS_URL = "ws://10.0.0.112:3001"
    private var firebaseAuthInstance: FirebaseAuth? = null
    private lateinit var appContext: Context

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun getFirebaseAuthInstance(): FirebaseAuth {
        if (firebaseAuthInstance == null) {
            firebaseAuthInstance = FirebaseAuth.getInstance()
        }
        return firebaseAuthInstance!!
    }

    fun getUserID(): String {
        return FirebaseAuth.getInstance().currentUser!!.uid
    }

    fun saveCurrentUserDbId(id: Int) {
        appContext.getSharedPreferences("cyphertext_prefs", Context.MODE_PRIVATE)
            .edit().putInt("current_user_db_id", id).apply()
    }

    fun getCurrentUserDbId(): Int {
        return appContext.getSharedPreferences("cyphertext_prefs", Context.MODE_PRIVATE)
            .getInt("current_user_db_id", -1)
    }

    fun saveEphemeralKey(key: ByteArray, oneTimePreKeyId: Int?) {
        appContext.getSharedPreferences("cyphertext_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString("pending_ephemeral_key", Base64.encodeToString(key, Base64.NO_WRAP))
            .putInt("pending_otk_id", oneTimePreKeyId ?: -1)
            .apply()
    }

    fun getEphemeralKey(): ByteArray? {
        val prefs = appContext.getSharedPreferences("cyphertext_prefs", Context.MODE_PRIVATE)
        val encoded = prefs.getString("pending_ephemeral_key", null) ?: return null
        prefs.edit().remove("pending_ephemeral_key").apply()
        return Base64.decode(encoded, Base64.NO_WRAP)
    }

    fun getOneTimePreKeyId(): Int? {
        val prefs = appContext.getSharedPreferences("cyphertext_prefs", Context.MODE_PRIVATE)
        val id = prefs.getInt("pending_otk_id", -1)
        prefs.edit().remove("pending_otk_id").apply()
        return if (id == -1) null else id
    }  // ← function ends here

    // These are now properly separate top-level functions
    fun markEphemeralKeyDelivered(chatRoomId: String) {
        appContext.getSharedPreferences("cyphertext_prefs", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("ephemeral_delivered_$chatRoomId", true)
            .apply()
    }

    fun isEphemeralKeyDelivered(chatRoomId: String): Boolean {
        return appContext.getSharedPreferences("cyphertext_prefs", Context.MODE_PRIVATE)
            .getBoolean("ephemeral_delivered_$chatRoomId", false)
    }

    fun saveSharedSecret(chatRoomId: String, secret: ByteArray) {
        appContext.getSharedPreferences("cyphertext_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString("shared_secret_$chatRoomId", Base64.encodeToString(secret, Base64.NO_WRAP))
            .apply()
    }

    fun getSharedSecret(chatRoomId: String): ByteArray? {
        val encoded = appContext.getSharedPreferences("cyphertext_prefs", Context.MODE_PRIVATE)
            .getString("shared_secret_$chatRoomId", null) ?: return null
        return Base64.decode(encoded, Base64.NO_WRAP)
    }

    fun saveRatchetState(chatRoomId: String, state: DoubleRatchet.RatchetState) {
        try {
            val json = org.json.JSONObject().apply {
                put("rootKey", Base64.encodeToString(state.rootKey, Base64.NO_WRAP))
                put("sendingChainKey", Base64.encodeToString(state.sendingChainKey, Base64.NO_WRAP))
                put("receivingChainKey", Base64.encodeToString(state.receivingChainKey, Base64.NO_WRAP))
                put("sendingRatchetPublicKey", Base64.encodeToString(
                    state.sendingRatchetKeyPair.publicKey.serialize(), Base64.NO_WRAP))
                put("sendingRatchetPrivateKey", Base64.encodeToString(
                    state.sendingRatchetKeyPair.privateKey.serialize(), Base64.NO_WRAP))
                put("receivingRatchetPublicKey", state.receivingRatchetPublicKey?.let {
                    Base64.encodeToString(it, Base64.NO_WRAP)
                })
                put("sendMessageCount", state.sendMessageCount)
                put("receiveMessageCount", state.receiveMessageCount)
            }
            appContext.getSharedPreferences("cyphertext_prefs", Context.MODE_PRIVATE)
                .edit()
                .putString("ratchet_state_$chatRoomId", json.toString())
                .apply()
            Log.d("Utils", "Ratchet state saved for room $chatRoomId")
        } catch (e: Exception) {
            Log.e("Utils", "Failed to save ratchet state: ${e.message}")
        }
    }

    fun getRatchetState(chatRoomId: String): DoubleRatchet.RatchetState? {
        return try {
            val json = appContext.getSharedPreferences("cyphertext_prefs", Context.MODE_PRIVATE)
                .getString("ratchet_state_$chatRoomId", null) ?: return null

            val obj = org.json.JSONObject(json)
            val publicKeyBytes = Base64.decode(obj.getString("sendingRatchetPublicKey"), Base64.NO_WRAP)
            val privateKeyBytes = Base64.decode(obj.getString("sendingRatchetPrivateKey"), Base64.NO_WRAP)

            DoubleRatchet.RatchetState(
                rootKey = Base64.decode(obj.getString("rootKey"), Base64.NO_WRAP),
                sendingChainKey = Base64.decode(obj.getString("sendingChainKey"), Base64.NO_WRAP),
                receivingChainKey = Base64.decode(obj.getString("receivingChainKey"), Base64.NO_WRAP),
                sendingRatchetKeyPair = org.signal.libsignal.protocol.ecc.ECKeyPair(
                    org.signal.libsignal.protocol.ecc.Curve.decodePoint(publicKeyBytes, 0),
                    org.signal.libsignal.protocol.ecc.Curve.decodePrivatePoint(privateKeyBytes)
                ),
                receivingRatchetPublicKey = if (obj.isNull("receivingRatchetPublicKey")) null
                else Base64.decode(obj.getString("receivingRatchetPublicKey"), Base64.NO_WRAP),
                sendMessageCount = obj.getInt("sendMessageCount"),
                receiveMessageCount = obj.getInt("receiveMessageCount")
            )
        } catch (e: Exception) {
            Log.e("Utils", "Failed to get ratchet state: ${e.message}")
            null
        }
    }
}