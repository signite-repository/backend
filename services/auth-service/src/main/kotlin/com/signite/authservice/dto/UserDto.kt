package com.signite.authservice.dto

/**
 * 사용자 프로필 관련 DTO
 */

// 요청 DTOs
data class UpdateProfileRequest(
    val fullName: String? = null,
    val bio: String? = null,
    val phoneNumber: String? = null,
    val location: String? = null,
    val website: String? = null
)

// 응답 DTOs
data class UserProfileResponse(
    val id: String,
    val username: String,
    val email: String,
    val fullName: String?,
    val bio: String?,
    val avatarUrl: String?,
    val phoneNumber: String?,
    val location: String?,
    val website: String?,
    val emailVerified: Boolean,
    val twoFactorEnabled: Boolean,
    val roles: List<String>,
    val createdAt: String,
    val updatedAt: String?
)