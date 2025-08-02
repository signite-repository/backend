package com.signite.authservice.domain

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.index.Indexed
import java.time.Instant

@Document("users")
data class User(
    @Id
    var id: String? = null,
    
    @Indexed(unique = true)
    var username: String,
    
    @Indexed(unique = true)
    var email: String,
    
    var password: String,
    var fullName: String? = null,
    var bio: String? = null,
    var avatarUrl: String? = null,
    var phoneNumber: String? = null,
    var location: String? = null,
    var website: String? = null,
    
    var enabled: Boolean = true,
    var emailVerified: Boolean = false,
    var twoFactorEnabled: Boolean = false,
    
    var roles: List<String> = listOf("USER"),
    
    val createdAt: Instant = Instant.now(),
    var updatedAt: Instant? = null
)