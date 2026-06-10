package com.example.backend.auth

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.*

@Component
class JwtProvider(
    @Value("\${jwt.secret:dev_secret_change_me}") private val secret: String,
    @Value("\${jwt.expirationMinutes:120}") private val expirationMinutes: Long
) {
    private val key = Keys.hmacShaKeyFor(secret.toByteArray())

    fun createToken(userId: Long, username: String): String {
        val now = Date()
        val exp = Date(now.time + expirationMinutes * 60_000)
        return Jwts.builder()
            .setSubject(userId.toString())
            .claim("username", username)
            .setIssuedAt(now)
            .setExpiration(exp)
            .signWith(key, SignatureAlgorithm.HS256)
            .compact()
    }
}
