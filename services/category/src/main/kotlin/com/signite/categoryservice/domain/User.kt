package com.signite.categoryservice.domain

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.index.Indexed
import java.time.Instant

/**
 * 사용자 도메인 모델
 */
@Document("users")
data class User(
    @Id
    var id: String? = null,
    
    @Indexed(unique = true)
    var username: String,
    
    @Indexed(unique = true)
    var email: String,
    
    var password: String,
    
    // 기본 프로필 정보
    var fullName: String? = null,
    var bio: String? = null,
    var avatarUrl: String? = null,
    var phoneNumber: String? = null,
    var location: String? = null,
    var website: String? = null,
    
    // 계정 상태
    var enabled: Boolean = true,
    var emailVerified: Boolean = false,
    var phoneVerified: Boolean = false,
    
    // 보안
    var roles: List<String> = listOf("USER"),
    var twoFactorEnabled: Boolean = false,
    var twoFactorSecret: String? = null,
    
    // 소셜 로그인
    var googleId: String? = null,
    var facebookId: String? = null,
    var githubId: String? = null,
    
    // 알림 설정
    var emailNotifications: Boolean = true,
    var pushNotifications: Boolean = false,
    
    // 개인정보 설정
    var profileVisibility: ProfileVisibility = ProfileVisibility.PUBLIC,
    var showEmail: Boolean = false,
    var showPhone: Boolean = false,
    
    // 타임스탬프
    val createdAt: Instant = Instant.now(),
    var updatedAt: Instant? = null,
    var lastLoginAt: Instant? = null,
    var deletedAt: Instant? = null,
    
    // 기타
    var loginAttempts: Int = 0,
    var lockedUntil: Instant? = null,
    var preferences: Map<String, Any>? = null
) {
    // 계정 잠금 확인
    fun isAccountLocked(): Boolean {
        return lockedUntil?.isAfter(Instant.now()) ?: false
    }
    
    // 권한 확인
    fun hasRole(role: String): Boolean {
        return roles.contains(role)
    }
    
    // 관리자 확인
    fun isAdmin(): Boolean {
        return hasRole("ADMIN") || hasRole("SUPER_ADMIN")
    }
}

/**
 * 프로필 공개 설정
 */
enum class ProfileVisibility {
    PUBLIC,      // 모든 사용자에게 공개
    FRIENDS,     // 친구에게만 공개
    PRIVATE      // 비공개
}

/**
 * 사용자 활동 로그
 */
@Document("user_activities")
data class UserActivity(
    @Id
    val id: String? = null,
    val userId: String,
    val action: String,
    val ipAddress: String? = null,
    val userAgent: String? = null,
    val metadata: Map<String, Any>? = null,
    val createdAt: Instant = Instant.now()
)

/**
 * 사용자 세션
 */
@Document("user_sessions")
data class UserSession(
    @Id
    val id: String? = null,
    val userId: String,
    val refreshToken: String,
    val deviceId: String? = null,
    val deviceName: String? = null,
    val ipAddress: String? = null,
    val userAgent: String? = null,
    val createdAt: Instant = Instant.now(),
    val expiresAt: Instant,
    val lastUsedAt: Instant = Instant.now()
)