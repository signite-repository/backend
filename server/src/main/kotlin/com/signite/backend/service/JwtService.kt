package com.signite.backend.service

import com.signite.backend.domain.entity.User
import com.signite.backend.repository.UserRoleRepository
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*
import javax.crypto.SecretKey

@Service
class JwtService(
    @Value("\${jwt.secret:dGVzdC1zZWNyZXQta2V5LWZvci10ZXN0aW5nLXB1cnBvc2VzLW9ubHk=}")
    private val jwtSecret: String,
    
    @Value("\${jwt.issuer:https://auth.signite.com}")
    private val jwtIssuer: String,
    
    @Value("\${jwt.audience:signite-api}")
    private val jwtAudience: String,
    
    @Value("\${jwt.expiration:86400}")
    private val jwtExpiration: Long,
    
    @Autowired
    private val userRoleRepository: UserRoleRepository
) {
    
    private val secretKey: SecretKey = Keys.hmacShaKeyFor(jwtSecret.toByteArray())
    
    fun generateToken(user: User, organizationId: Int? = null, role: String = "ACTIVE_MEMBER"): Mono<String> {
        val now = Date()
        val expiryDate = Date(now.time + jwtExpiration * 1000)
        
        val claims = mutableMapOf<String, Any>(
            "user_id" to user.id,
            "username" to (user.username ?: ""),
            "email" to (user.email ?: ""),
            "role" to role,
            "image_url" to (user.imageUrl ?: ""),
            "github_url" to (user.githubUrl ?: ""),
            "summary" to (user.summary ?: "")
        )
        
        if (organizationId != null) {
            claims["organization_id"] = organizationId
        }
        
        val token = Jwts.builder()
            .setSubject(user.username)
            .setIssuer(jwtIssuer)
            .setAudience(jwtAudience)
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .addClaims(claims)
            .signWith(secretKey, SignatureAlgorithm.HS256)
            .compact()
            
        return Mono.just(token)
    }
    
    fun generateTokenWithRole(user: User, role: String, organizationId: Int? = null): Mono<String> {
        return generateToken(user, organizationId, role)
    }
    
    fun getRoleFromContext(userId: Int): Mono<String> {
        return userRoleRepository.findByUserIdAndIsActiveOrderByCreatedAtDesc(userId, true)
            .next()
            .map { it.role }
            .defaultIfEmpty("ACTIVE_MEMBER")
    }
    
    fun getOrganizationFromContext(userId: Int): Mono<Int?> {
        return userRoleRepository.findByUserIdAndIsActiveOrderByCreatedAtDesc(userId, true)
            .next()
            .map { it.organizationId }
            .cast(Int::class.java)
    }
} 