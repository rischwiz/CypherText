package com.richapps.cyphertext.models

import com.google.firebase.Timestamp


data class MessageModel(
    val message: String = "",
    val senderId: Int = -1,
    val timeStamp: Long = System.currentTimeMillis()
)
