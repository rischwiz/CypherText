package com.richapps.cyphertext.network

import com.richapps.cyphertext.models.RegisterRequest
import com.richapps.cyphertext.models.RegisterResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RegistrationRepository(private val apiService: ApiService) {

    suspend fun register(phoneNumber: String, username: String, fcmToken: String, firebaseUid: String): Result<RegisterResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.registerUser(
                    RegisterRequest(phoneNumber, username, fcmToken, firebaseUid)
                )
                if (response.isSuccessful) {
                    Result.success(response.body()!!)
                } else {
                    Result.failure(Exception("Registration failed: ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}