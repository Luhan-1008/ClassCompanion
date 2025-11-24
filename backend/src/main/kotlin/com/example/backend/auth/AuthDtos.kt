package com.example.backend.auth

import jakarta.validation.constraints.NotBlank

data class RegisterRequest(
    @field:NotBlank val username: String,
    @field:NotBlank val password: String,
    val email: String? = null,
    val realName: String? = null
)

data class RegisterResponse(
    val success: Boolean,
    val message: String,
    val userId: Long? = null
)

data class LoginRequest(
    @field:NotBlank val username: String,
    @field:NotBlank val password: String
)

data class LoginResponse(
    val token: String,
    val userId: Long
)
