package com.example.myapplication.data.repository

import com.example.myapplication.network.RetrofitClient
import com.example.myapplication.network.dto.LoginRequest
import com.example.myapplication.network.dto.LoginResponse
import com.example.myapplication.network.dto.RegisterRequest
import com.example.myapplication.network.dto.RegisterResponse

class RemoteUserRepository {
    private val api = RetrofitClient.api

    suspend fun register(username: String, password: String, email: String?, realName: String?): RegisterResponse {
        return api.register(RegisterRequest(username = username, password = password, email = email, realName = realName))
    }

    suspend fun login(username: String, password: String): LoginResponse {
        return api.login(LoginRequest(username = username, password = password))
    }
}