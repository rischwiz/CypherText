package com.richapps.cyphertext.models

import com.google.firebase.Timestamp


data class ChatRoomModel(
    val chatRoomId: String = "",
    val userIds: ArrayList<String> = ArrayList(),
    val lastMessageTimeStamp: Long = System.currentTimeMillis(),
    val lastMessageSenderId: String = ""
)
