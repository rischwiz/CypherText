package com.richapps.cyphertext.models

data class RegisterRequest(
    val phoneHash : String,
    val fcmToken : String,
    val identityKey : String
)
