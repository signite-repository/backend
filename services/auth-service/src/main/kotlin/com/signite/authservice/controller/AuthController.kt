package com.signite.authservice.controller

import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono
import com.signite.authservice.service.AuthService
import com.signite.authservice.dto.*
import jakarta.validation.Valid

/**
 * 인증 관련 컨트롤러
 * - 로그인, 회원가입, 토큰 갱신
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = ["*"])
class AuthController(
    private val authService: AuthService
) {
    @PostMapping("/register")
    fun register(@Valid @RequestBody request: RegisterRequest): Mono<AuthResponse> {
        return authService.register(request)
    }

    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): Mono<AuthResponse> {
        return authService.login(request)
    }

    @PostMapping("/refresh")
    fun refreshToken(@RequestHeader("Refresh-Token") refreshToken: String): Mono<AuthResponse> {
        return authService.refreshToken(refreshToken)
    }

    @GetMapping("/check")
    fun checkAuth(@RequestHeader("Authorization") token: String): Mono<UserInfo> {
        val jwt = token.removePrefix("Bearer ")
        return authService.validateToken(jwt)
    }

    @PostMapping("/logout")
    fun logout(@RequestHeader("Authorization") token: String): Mono<Void> {
        val jwt = token.removePrefix("Bearer ")
        return authService.logout(jwt)
    }
}