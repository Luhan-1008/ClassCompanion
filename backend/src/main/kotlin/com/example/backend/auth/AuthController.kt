package com.example.backend.auth

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.validation.annotation.Validated

@RestController
@RequestMapping("/api/auth")
class AuthController(private val authService: AuthService) {
    @PostMapping("/register")
    fun register(@RequestBody @Validated req: RegisterRequest): ResponseEntity<RegisterResponse> =
        ResponseEntity.ok(authService.register(req))

    @PostMapping("/login")
    fun login(@RequestBody @Validated req: LoginRequest): ResponseEntity<LoginResponse> =
        ResponseEntity.ok(authService.login(req))
}
