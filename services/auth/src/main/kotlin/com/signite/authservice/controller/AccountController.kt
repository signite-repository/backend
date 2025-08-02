package com.signite.authservice.controller

import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono
import com.signite.authservice.service.AccountService
import com.signite.authservice.dto.*

/**
 * 계정 관리 관련 컨트롤러
 * - 비밀번호/이메일 변경, 2FA, 계정 삭제
 */
@RestController
@RequestMapping("/api/account")
@CrossOrigin(origins = ["*"])
class AccountController(
    private val accountService: AccountService
) {
    @PostMapping("/change-password")
    fun changePassword(
        @RequestHeader("Authorization") token: String,
        @RequestBody request: ChangePasswordRequest
    ): Mono<MessageResponse> {
        return accountService.changePassword(token, request)
    }

    @PostMapping("/change-email")
    fun changeEmail(
        @RequestHeader("Authorization") token: String,
        @RequestBody request: ChangeEmailRequest
    ): Mono<MessageResponse> {
        return accountService.changeEmail(token, request)
    }

    @PostMapping("/2fa")
    fun toggle2FA(
        @RequestHeader("Authorization") token: String,
        @RequestBody request: Toggle2FARequest
    ): Mono<TwoFactorResponse> {
        return accountService.toggle2FA(token, request)
    }

    @DeleteMapping("/delete")
    fun deleteAccount(
        @RequestHeader("Authorization") token: String,
        @RequestBody request: DeleteAccountRequest
    ): Mono<MessageResponse> {
        return accountService.deleteAccount(token, request)
    }
}