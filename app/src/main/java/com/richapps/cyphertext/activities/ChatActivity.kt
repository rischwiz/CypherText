package com.richapps.cyphertext.activities

import android.os.Bundle
import android.util.Base64
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.richapps.cyphertext.R
import com.richapps.cyphertext.Utils
import com.richapps.cyphertext.adapters.ChatAdapter
import com.richapps.cyphertext.databinding.ActivityChatBinding
import com.richapps.cyphertext.models.MessageModel
import com.richapps.cyphertext.network.WebSocketManager
import com.richapps.cyphertext.security.DoubleRatchet
import com.richapps.cyphertext.security.EncryptedMessage
import com.richapps.cyphertext.security.KeyBundleFetcher
import com.richapps.cyphertext.security.MessageEncryption
import com.richapps.cyphertext.security.X3DHManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class ChatActivity : AppCompatActivity() {

    lateinit var binding: ActivityChatBinding
    private val serverUrl = Utils.SERVER_URL
    private var chatRoomId: String = ""
    private var otherUserId: Int = -1
    private var adapter: ChatAdapter? = null
    private var sharedSecret: ByteArray? = null
    private var ratchetState: DoubleRatchet.RatchetState? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("ChatActivity", "currentUserId: ${Utils.getCurrentUserDbId()}")
        Log.d("ChatActivity", "otherUserId: ${intent.getIntExtra("userId", -1)}")
        enableEdgeToEdge()
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        otherUserId = intent.getIntExtra("userId", -1)
        val currentUserId = Utils.getCurrentUserDbId()
        chatRoomId = getChatRoomId(currentUserId, otherUserId)

        setupToolbar()
        setupMessageAdapter()
        createOrGetChatRoom()

        // Load existing shared secret if we already have one
        val existingSecret = Utils.getSharedSecret(chatRoomId)
        if (existingSecret != null) {
            sharedSecret = existingSecret
            ratchetState = Utils.getRatchetState(chatRoomId)
            Log.d("ChatActivity", "Existing shared secret loaded for room $chatRoomId")
        }

        Log.d("ChatActivity", "Attempting WebSocket connection for user: ${Utils.getCurrentUserDbId()}")
        WebSocketManager.connect(Utils.getCurrentUserDbId())

        setupMessageListener()

        binding.sendBtn.setOnClickListener {
            val message = binding.message.text.toString().trim()
            if (message.isEmpty()) {
                binding.message.error = "Message cannot be empty"
            } else {
                sendMessage(message)
            }
        }
    }

    private fun setupMessageListener() {
        WebSocketManager.setMessageListener { json ->
            Log.d("ChatActivity", "Message received in listener: $json")
            val type = json.getString("type")
            if (type == "message") {

                // Handle X3DH key exchange if this is the first message
                if (json.has("ephemeralKey") && sharedSecret == null) {
                    val ephemeralKeyBytes = Base64.decode(
                        json.getString("ephemeralKey"), Base64.NO_WRAP
                    )
                    val oneTimePreKeyId = if (json.isNull("oneTimePreKeyId")) null
                    else json.getInt("oneTimePreKeyId")

                    sharedSecret = X3DHManager.performX3DHAsReceiver(
                        applicationContext,
                        ephemeralKeyBytes,
                        oneTimePreKeyId
                    )
                    sharedSecret?.let {
                        Utils.saveSharedSecret(chatRoomId, it)
                        Utils.markEphemeralKeyDelivered(chatRoomId)
                        Log.d("ChatActivity", "Receiver shared secret: ${
                            Base64.encodeToString(it, Base64.NO_WRAP)}")

                        // Initialize ratchet as receiver
                        ratchetState = DoubleRatchet.initialize(
                            sharedSecret = it,
                            isInitiator = false
                        )
                        ratchetState?.let { state ->
                            Utils.saveRatchetState(chatRoomId, state)
                            Log.d("ChatActivity", "Ratchet initialized as receiver")
                        }
                    }
                }

                // Decrypt the message content
                val encryptedContent = json.getString("content")
                val decryptedContent = try {
                    // Try to parse as a Double Ratchet message first
                    val contentJson = org.json.JSONObject(encryptedContent)
                    val encryptedMessage = EncryptedMessage(
                        ciphertext = contentJson.getString("ciphertext"),
                        senderRatchetKey = contentJson.getString("ratchetKey"),
                        messageIndex = contentJson.getInt("index")
                    )

                    if (ratchetState != null) {
                        val incomingRatchetKey = Base64.decode(
                            encryptedMessage.senderRatchetKey, Base64.NO_WRAP
                        )
                        val currentRatchetKey = ratchetState!!.receivingRatchetPublicKey

                        // Only perform DH ratchet step if key has CHANGED from a previously seen key
                        // NOT on the first message when receivingRatchetPublicKey is null
                        val updatedState = if (currentRatchetKey != null &&
                            !currentRatchetKey.contentEquals(incomingRatchetKey)) {
                            Log.d("ChatActivity", "Performing DH ratchet step - key changed")
                            DoubleRatchet.dhRatchetStep(ratchetState!!, incomingRatchetKey)
                        } else {
                            // First message or same ratchet key - just update key reference
                            ratchetState!!.copy(
                                receivingRatchetPublicKey = incomingRatchetKey
                            )
                        }

                        val result = DoubleRatchet.decryptMessage(updatedState, encryptedMessage)
                        if (result != null) {
                            ratchetState = result.first
                            Utils.saveRatchetState(chatRoomId, ratchetState!!)
                            Log.d("ChatActivity", "Message decrypted with Double Ratchet #${encryptedMessage.messageIndex}")
                            result.second
                        } else {
                            Log.w("ChatActivity", "Ratchet decryption failed")
                            encryptedContent
                        }
                    } else {
                        // No ratchet state yet, fall back to shared secret
                        Log.w("ChatActivity", "No ratchet state, falling back to shared secret")
                        if (sharedSecret != null) {
                            MessageEncryption.decrypt(encryptedContent, sharedSecret!!) ?: encryptedContent
                        } else encryptedContent
                    }
                } catch (e: Exception) {
                    // Not a ratchet message - try direct decryption
                    Log.d("ChatActivity", "Not a ratchet message, trying direct decryption")
                    if (sharedSecret != null) {
                        MessageEncryption.decrypt(encryptedContent, sharedSecret!!) ?: encryptedContent
                    } else {
                        Log.w("ChatActivity", "No shared secret for decryption")
                        encryptedContent
                    }
                }

                val incomingMessage = MessageModel(
                    message = decryptedContent,
                    senderId = json.getInt("senderId"),
                    timeStamp = json.getLong("timestamp")
                )
                runOnUiThread {
                    val currentList = (adapter?.messageList ?: emptyList()).toMutableList()
                    currentList.add(incomingMessage)
                    adapter?.updateMessages(currentList)
                    binding.messageList.scrollToPosition((adapter?.itemCount ?: 1) - 1)
                }
            }
        }
    }
    fun sendMessage(message: String) {
        if (sharedSecret == null) {
            CoroutineScope(Dispatchers.IO).launch {
                val bundle = KeyBundleFetcher.fetchKeyBundle(otherUserId)
                if (bundle != null) {
                    val result = X3DHManager.performX3DHAsSender(bundle)
                    if (result != null) {
                        sharedSecret = result.sharedSecret
                        Utils.saveSharedSecret(chatRoomId, result.sharedSecret)
                        Utils.saveEphemeralKey(result.ephemeralPublicKey, result.oneTimePreKeyId)

                        // Initialize ratchet as initiator
                        ratchetState = DoubleRatchet.initialize(
                            sharedSecret = result.sharedSecret,
                            isInitiator = true
                        )
                        ratchetState?.let { Utils.saveRatchetState(chatRoomId, it) }
                        Log.d("ChatActivity", "Ratchet initialized as initiator")

                        withContext(Dispatchers.Main) {
                            dispatchMessage(message)
                        }
                    }
                }
            }
        } else if (!Utils.isEphemeralKeyDelivered(chatRoomId)) {
            CoroutineScope(Dispatchers.IO).launch {
                val bundle = KeyBundleFetcher.fetchKeyBundle(otherUserId)
                if (bundle != null) {
                    val result = X3DHManager.performX3DHAsSender(bundle)
                    if (result != null) {
                        sharedSecret = result.sharedSecret
                        Utils.saveSharedSecret(chatRoomId, result.sharedSecret)
                        Utils.saveEphemeralKey(result.ephemeralPublicKey, result.oneTimePreKeyId)

                        ratchetState = DoubleRatchet.initialize(
                            sharedSecret = result.sharedSecret,
                            isInitiator = true
                        )
                        ratchetState?.let { Utils.saveRatchetState(chatRoomId, it) }

                        withContext(Dispatchers.Main) {
                            dispatchMessage(message)
                        }
                    }
                }
            }
        } else {
            dispatchMessage(message)
        }
    }

    private fun dispatchMessage(message: String) {
        val ephemeralKey = Utils.getEphemeralKey()
        val oneTimePreKeyId = Utils.getOneTimePreKeyId()

        if (ephemeralKey != null) {
            Utils.markEphemeralKeyDelivered(chatRoomId)
        }
        val contentToSend: String
        if (ratchetState != null) {
            val result = DoubleRatchet.encryptMessage(ratchetState!!, message)
            if (result != null) {
                ratchetState = result.first
                Utils.saveRatchetState(chatRoomId, ratchetState!!)
                // Encode the full encrypted message as JSON
                contentToSend = org.json.JSONObject().apply {
                    put("ciphertext", result.second.ciphertext)
                    put("ratchetKey", result.second.senderRatchetKey)
                    put("index", result.second.messageIndex)
                }.toString()
                Log.d("ChatActivity", "Message encrypted with Double Ratchet #${result.second.messageIndex}")
            } else {
                contentToSend = message
            }
        } else if (sharedSecret != null) {
            contentToSend = MessageEncryption.encrypt(message, sharedSecret!!) ?: message
        } else {
            contentToSend = message
        }

        val messageModel = MessageModel(
            message = message,
            senderId = Utils.getCurrentUserDbId(),
            timeStamp = System.currentTimeMillis()
        )

        val currentList = (adapter?.messageList ?: emptyList()).toMutableList()
        currentList.add(messageModel)
        adapter?.updateMessages(currentList)
        binding.message.text?.clear()
        binding.messageList.scrollToPosition((adapter?.itemCount ?: 1) - 1)

        WebSocketManager.sendMessage(
            otherUserId, chatRoomId, contentToSend,
            ephemeralKey, oneTimePreKeyId
        )
    }

    fun setupMessageAdapter() {
        adapter = ChatAdapter()
        binding.messageList.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        binding.messageList.adapter = adapter
    }

    override fun onDestroy() {
        super.onDestroy()
        WebSocketManager.disconnect()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.toolbar.title = intent.getStringExtra("userName")
    }

    private fun createOrGetChatRoom() {
        if (otherUserId == -1) {
            Log.e("ChatActivity", "Invalid otherUserId")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("$serverUrl/chatroom/create")
                val connection = url.openConnection() as HttpURLConnection
                connection.apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    doOutput = true
                    connectTimeout = 5000
                    readTimeout = 5000
                }

                val body = JSONObject().apply {
                    put("chatRoomId", chatRoomId)
                    put("user1Id", Utils.getCurrentUserDbId())
                    put("user2Id", otherUserId)
                }

                connection.outputStream.use { os ->
                    os.write(body.toString().toByteArray())
                }

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    Log.d("ChatActivity", "Chat room ready: $chatRoomId")
                } else {
                    Log.e("ChatActivity", "Server error creating chat room: $responseCode")
                }
                connection.disconnect()

            } catch (e: Exception) {
                Log.e("ChatActivity", "Error creating chat room: ${e.message}")
            }
        }
    }

    private fun getChatRoomId(u1: Int, u2: Int): String {
        return if (u1 < u2) "${u1}_${u2}" else "${u2}_${u1}"
    }
}