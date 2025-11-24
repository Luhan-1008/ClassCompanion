package com.example.myapplication.network.dto

data class RegisterRequest(
    val username: String,
    val password: String,
    val email: String? = null,
    val realName: String? = null
)

data class RegisterResponse(
    val success: Boolean,
    val message: String
)

data class LoginRequest(
    val username: String,
    val password: String
)

data class LoginResponse(
    val token: String,
    val userId: Long
)