package com.example.myapplication.data.repository

import com.example.myapplication.BuildConfig
import com.example.myapplication.data.model.User
import com.example.myapplication.network.RetrofitClient
import com.example.myapplication.network.dto.LoginRequest
import com.example.myapplication.network.dto.LoginResponse
import com.example.myapplication.network.dto.RegisterRequest
import com.example.myapplication.network.dto.RegisterResponse
import com.example.myapplication.network.dto.UpdateUserProfileRequest

// 客户端访问后端登录接口的直接证据
class RemoteUserRepository {
    private val api = RetrofitClient.api

    suspend fun register(username: String, password: String, email: String?, realName: String?): RegisterResponse {
        return api.register(RegisterRequest(username = username, password = password, email = email, realName = realName))
    }

    suspend fun login(username: String, password: String): LoginResponse {
        return api.login(LoginRequest(username = username, password = password))
    }

    suspend fun getUserProfile(userId: Long): User {
        val profile = api.getUserProfile(userId)
        return User(
            userId = profile.userId.toInt(),
            username = profile.username,
            password = "",
            studentId = profile.studentId,
            realName = profile.realName,
            email = profile.email,
            avatarUrl = profile.avatarUrl
        )
    }

    suspend fun updateUserProfile(user: User): User {
        val profile = api.updateUserProfile(
            userId = user.userId.toLong(),
            body = UpdateUserProfileRequest(
                studentId = user.studentId,
                realName = user.realName,
                email = user.email,
                avatarUrl = user.avatarUrl
            )
        )
        return User(
            userId = profile.userId.toInt(),
            username = profile.username,
            password = user.password,
            studentId = profile.studentId,
            realName = profile.realName,
            email = profile.email,
            avatarUrl = profile.avatarUrl
        )
    }
}
