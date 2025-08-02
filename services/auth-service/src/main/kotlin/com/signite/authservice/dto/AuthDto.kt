package com.signite.authservice.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/**
 * 인증 관련 DTO
 */

// 요청 DTOs
data class RegisterRequest(
    @field:NotBlank(message = "사용자명은 필수 항목입니다")
    @field:Size(min = 3, max = 20, message = "사용자명은 3자 이상 20자 이하여야 합니다")
    val username: String,
    
    @field:NotBlank(message = "이메일은 필수 항목입니다")
    @field:Email(message = "올바른 이메일 형식이 아닙니다")
    val email: String,
    
    @field:NotBlank(message = "비밀번호는 필수 항목입니다")
    @field:Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다")
    val password: String,
    
    val fullName: String? = null
)

data class LoginRequest(
    @field:NotBlank(message = "사용자명은 필수 항목입니다")
    val username: String,
    
    @field:NotBlank(message = "비밀번호는 필수 항목입니다")
    val password: String
)

data class RefreshTokenRequest(
    val refreshToken: String
)

// 응답 DTOs
data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "Bearer",
    val expiresIn: Long,
    val user: UserInfo
)

data class UserInfo(
    val id: String,
    val username: String,
    val email: String,
    val fullName: String?,
    val roles: List<String>
)