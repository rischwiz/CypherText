package com.richapps.cyphertext.network

import com.richapps.cyphertext.models.RegisterRequest
import com.richapps.cyphertext.models.RegisterResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("register")
    suspend fun registerUser(@Body request: RegisterRequest): Response<RegisterResponse>
}