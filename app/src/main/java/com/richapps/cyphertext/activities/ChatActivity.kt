package com.richapps.cyphertext.activities

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.Timestamp
import com.richapps.cyphertext.R
import com.richapps.cyphertext.Utils
import com.richapps.cyphertext.adapters.ChatAdapter
import com.richapps.cyphertext.databinding.ActivityChatBinding
import com.richapps.cyphertext.models.MessageModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class ChatActivity : AppCompatActivity() {

    lateinit var binding: ActivityChatBinding
    private val serverUrl = "http://10.0.2.2:3000"
    private var chatRoomId: String = ""
    private var otherUserId: Int = -1
    private var adapter: ChatAdapter? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
        createOrGetChatRoom()
        setupMessageAdapter()

        binding.sendBtn.setOnClickListener {
            val message: String = binding.message.text.toString().trim()
            if (message.isEmpty()) {
                binding.message.error = "Message cannot be empty"
            } else {
                sendMessage(message)
            }
        }
    }

    fun setupMessageAdapter() {
        adapter = ChatAdapter()
        binding.messageList.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        binding.messageList.adapter = adapter
        // TODO: load messages once messaging is endpoint is ready

    }

    fun sendMessage(message: String) {
        val messageModel = MessageModel(
            message = message,
            senderId = Utils.getCurrentUserDbId(),
            timeStamp = System.currentTimeMillis()
        )
        // TODO: encrypt and send to server

        val currentList = (adapter?.messageList ?: emptyList()).toMutableList()
        currentList.add(messageModel)
        adapter?.updateMessages(currentList)
        binding.message.text?.clear()
        binding.messageList.scrollToPosition((adapter?.itemCount ?: 1) - 1)
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
                    // TODO: load messages once messaging is implemented
                } else {
                    Log.e("ChatActivity", "Server error creating chat room: $responseCode")

                }
                connection.disconnect()

            }catch (e: Exception) {
                Log.e("ChatActivity", "Error creating chat room: ${e.message}")
            }
        }
    }

    private fun getChatRoomId(u1: Int, u2: Int): String {
        return if (u1 < u2) "${u1}_${u2}" else "${u2}_${u1}"
    }
}