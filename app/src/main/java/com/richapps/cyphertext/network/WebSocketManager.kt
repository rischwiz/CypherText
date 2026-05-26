package com.richapps.cyphertext.network

import android.util.Log
import android.util.Base64
import com.richapps.cyphertext.Utils
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.json.JSONObject

object WebSocketManager {
    private const val TAG = "WebSocketManager"
    private val SERVER_URL = Utils.WS_URL

    private var webSocket: WebSocket? = null
    private var messageListener: ((JSONObject) -> Unit)? = null
    private val client = OkHttpClient()

    fun connect(userId: Int) {
        if (webSocket != null) {
            Log.d(TAG, "Already connected")
            return
        }

        val request = Request.Builder()
            .url(SERVER_URL)
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {

            override fun onOpen(ws: WebSocket, response: okhttp3.Response) {
                Log.d(TAG, "WebSocket connected")
                // Authenticate with the server immediately after connecting
                val authMessage = JSONObject().apply {
                    put("type", "auth")
                    put("userId", userId)
                }
                ws.send(authMessage.toString())
            }

            override fun onMessage(ws: WebSocket, text: String) {
                Log.d(TAG, "Message received: $text")
                try {
                    val json = JSONObject(text)
                    messageListener?.invoke(json)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing message: ${e.message}")
                }
            }

            override fun onMessage(ws: WebSocket, bytes: ByteString) {
                Log.d(TAG, "Binary message received")
            }

            override fun onClosing(ws: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closing: $reason")
                ws.close(1000, null)
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closed")
                webSocket = null
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: okhttp3.Response?) {
                Log.e(TAG, "WebSocket error: ${t.message}")
                webSocket = null
            }
        })
    }

    fun sendMessage(
        recipientId: Int,
        chatRoomId: String,
        content: String,
        ephemeralKey: ByteArray? = null,
        oneTimePreKeyId: Int? = null
    ) {
        if (webSocket == null) {
            Log.e(TAG, "WebSocket not connected")
            return
        }

        val message = JSONObject().apply {
            put("type", "message")
            put("recipientId", recipientId)
            put("chatRoomId", chatRoomId)
            put("content", content)
            put("timestamp", System.currentTimeMillis())
            // Include ephemeral key if this is the first message
            ephemeralKey?.let {
                put("ephemeralKey", Base64.encodeToString(it, Base64.NO_WRAP))
            }
            oneTimePreKeyId?.let {
                put("oneTimePreKeyId", it)
            }
        }

        webSocket?.send(message.toString())
        Log.d(TAG, "Message sent to user $recipientId")
    }

    fun setMessageListener(listener: (JSONObject) -> Unit) {
        messageListener = listener
    }

    fun disconnect() {
        webSocket?.close(1000, "User left chat")
        webSocket = null
        messageListener = null
        Log.d(TAG, "WebSocket disconnected")
    }
}