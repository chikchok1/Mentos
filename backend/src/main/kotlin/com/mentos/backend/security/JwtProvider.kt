package com.mentos.backend.security

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.*

@Component
class JwtProvider(
    @Value("\${app.jwt.secret}") private val secret: String,
    @Value("\${app.jwt.access-expiration-ms}") private val accessExpirationMs: Long,
    @Value("\${app.jwt.refresh-expiration-ms}") private val refreshExpirationMs: Long
) {
    private val key = Keys.hmacShaKeyFor(secret.toByteArray())

    fun generateAccessToken(userId: Long): String {
        return Jwts.builder()
            .subject(userId.toString())
            .issuedAt(Date())
            .expiration(Date(Date().time + accessExpirationMs))
            .signWith(key)
            .compact()
    }

    fun generateRefreshToken(userId: Long): String {
        return Jwts.builder()
            .subject(userId.toString())
            .issuedAt(Date())
            .expiration(Date(Date().time + refreshExpirationMs))
            .signWith(key)
            .compact()
    }

    fun validateToken(token: String): Boolean {
        return try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun getUserIdFromToken(token: String): Long {
        val claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).payload
        return claims.subject.toLong()
    }
}
