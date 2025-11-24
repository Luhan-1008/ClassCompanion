package com.example.backend.auth

import com.example.backend.user.UserEntity
import com.example.backend.user.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtProvider: JwtProvider
) {
    fun register(req: RegisterRequest): RegisterResponse {
        if (userRepository.existsByUsername(req.username)) {
            return RegisterResponse(success = false, message = "用户名已存在")
        }
        val hashed = passwordEncoder.encode(req.password)
        val saved = userRepository.save(
            UserEntity(
                username = req.username,
                password = hashed,
                email = req.email,
                realName = req.realName
            )
        )
        return RegisterResponse(success = true, message = "注册成功", userId = saved.id)
    }

    fun login(req: LoginRequest): LoginResponse {
        val user = userRepository.findByUsername(req.username) ?: throw RuntimeException("用户名或密码错误")
        if (!passwordEncoder.matches(req.password, user.password)) {
            throw RuntimeException("用户名或密码错误")
        }
        val token = jwtProvider.createToken(user.id!!, user.username)
        return LoginResponse(token = token, userId = user.id!!)
    }
}
