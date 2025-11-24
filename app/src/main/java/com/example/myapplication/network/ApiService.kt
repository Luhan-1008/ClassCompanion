package com.example.myapplication.network

import com.example.myapplication.network.dto.LoginRequest
import com.example.myapplication.network.dto.LoginResponse
import com.example.myapplication.network.dto.RegisterRequest
import com.example.myapplication.network.dto.RegisterResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("api/auth/login")
    suspend fun login(@Body body: LoginRequest): LoginResponse

    @POST("api/auth/register")
    suspend fun register(@Body body: RegisterRequest): RegisterResponse
}
