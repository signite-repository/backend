package com.signite.authservice.web

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono
import com.signite.authservice.service.AuthService
import com.signite.authservice.dto.*
import org.springframework.security.crypto.password.PasswordEncoder

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = ["*"])
class AuthController(
    private val authService: AuthService,
    private val passwordEncoder: PasswordEncoder
) {

    @PostMapping("/register")
    fun register(@RequestBody request: RegisterRequest): Mono<ResponseEntity<AuthResponse>> {
        return authService.register(request)
            .map { ResponseEntity.ok(it) }
            .onErrorResume { 
                when (it.message) {
                    "User already exists" -> Mono.just(ResponseEntity.status(HttpStatus.CONFLICT).build())
                    else -> Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).build())
                }
            }
    }

    @PostMapping("/login")
    fun login(@RequestBody request: LoginRequest): Mono<ResponseEntity<AuthResponse>> {
        return authService.login(request)
            .map { ResponseEntity.ok(it) }
            .onErrorResume {
                when (it.message) {
                    "Invalid credentials" -> Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build())
                    else -> Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).build())
                }
            }
    }

    @GetMapping("/check")
    fun checkAuth(@RequestHeader("Authorization") token: String): Mono<ResponseEntity<UserInfo>> {
        val jwtToken = token.removePrefix("Bearer ")
        return authService.validateToken(jwtToken)
            .map { ResponseEntity.ok(it) }
            .onErrorResume { 
                Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build())
            }
    }

    @PostMapping("/refresh")
    fun refreshToken(@RequestBody request: RefreshTokenRequest): Mono<ResponseEntity<AuthResponse>> {
        return authService.refreshToken(request.refreshToken)
            .map { ResponseEntity.ok(it) }
            .onErrorResume {
                Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build())
            }
    }

    @PostMapping("/logout")
    fun logout(@RequestHeader("Authorization") token: String): Mono<ResponseEntity<Void>> {
        val jwtToken = token.removePrefix("Bearer ")
        return authService.logout(jwtToken)
            .then(Mono.just(ResponseEntity.ok().build()))
    }
}

// DTOs
data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String,
    val fullName: String? = null
)

data class LoginRequest(
    val username: String,
    val password: String
)

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

data class RefreshTokenRequest(
    val refreshToken: String
)