package com.signite.authservice.service

import com.signite.authservice.domain.User
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.*

@Service
class JwtService {
    @Value("\${jwt.secret}")
    private lateinit var secret: String
    
    @Value("\${jwt.access-token-expiration}")
    private val accessTokenExpiration: Long = 3600000 // 1 hour
    
    @Value("\${jwt.refresh-token-expiration}")
    private val refreshTokenExpiration: Long = 604800000 // 7 days
    
    private val key by lazy {
        Keys.hmacShaKeyFor(secret.toByteArray())
    }
    
    fun generateAccessToken(user: User): String {
        return createToken(user.username, user.roles, accessTokenExpiration)
    }
    
    fun generateRefreshToken(user: User): String {
        return createToken(user.username, user.roles, refreshTokenExpiration)
    }
    
    private fun createToken(username: String, roles: List<String>, expiration: Long): String {
        val now = Date()
        val expiryDate = Date(now.time + expiration)
        
        return Jwts.builder()
            .setSubject(username)
            .claim("roles", roles)
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .signWith(key, SignatureAlgorithm.HS256)
            .compact()
    }
    
    fun validateToken(token: String): String {
        val claims = Jwts.parserBuilder()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(token)
            .body
        
        return claims.subject
    }
    
    fun isTokenValid(token: String): Boolean {
        return try {
            validateToken(token)
            true
        } catch (e: Exception) {
            false
        }
    }
}