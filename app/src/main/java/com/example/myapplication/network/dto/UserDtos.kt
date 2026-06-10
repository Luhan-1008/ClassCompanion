package com.example.myapplication.network.dto

data class UserProfileResponse(
    val userId: Long,
    val username: String,
    val studentId: String?,
    val realName: String?,
    val email: String?,
    val avatarUrl: String?
)

data class UpdateUserProfileRequest(
    val studentId: String? = null,
    val realName: String? = null,
    val email: String? = null,
    val avatarUrl: String? = null
)
