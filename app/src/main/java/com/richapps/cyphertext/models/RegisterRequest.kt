package com.richapps.cyphertext.models

import com.google.gson.annotations.SerializedName

// Request model for user registration.
// Backend expects: phoneHash, userName, fcmToken
data class RegisterRequest(
    @SerializedName("phoneNumber") val phoneNumber: String,
    @SerializedName("username") val userName: String,
    @SerializedName("fcmToken") val fcmToken: String,
    @SerializedName("firebaseUid") val firebaseUid: String
)

// Response model for user registration.
// Backend returns: success, message, userId
data class RegisterResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("userId") val userId: Int?
)
