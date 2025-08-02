package com.signite.authservice.dto

/**
 * 계정 관리 관련 DTO
 */

// 요청 DTOs
data class ChangePasswordRequest(
    val currentPassword: String,
    val newPassword: String
)

data class ChangeEmailRequest(
    val newEmail: String,
    val password: String
)

data class Toggle2FARequest(
    val enable: Boolean,
    val password: String,
    val code: String? = null
)

data class DeleteAccountRequest(
    val password: String,
    val reason: String? = null,
    val hardDelete: Boolean = false
)

// 응답 DTOs
data class MessageResponse(
    val message: String,
    val success: Boolean = true
)

data class TwoFactorResponse(
    val enabled: Boolean,
    val secret: String? = null,
    val qrCode: String? = null,
    val message: String
)