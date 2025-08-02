package com.signite.authservice.service

import com.signite.authservice.dto.*
import com.signite.authservice.domain.User
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val jwtService: JwtService,
    private val passwordEncoder: PasswordEncoder
) {
    fun register(request: RegisterRequest): Mono<AuthResponse> {
        return userRepository.existsByUsername(request.username)
            .flatMap { exists ->
                if (exists) {
                    Mono.error(RuntimeException("User already exists"))
                } else {
                    val user = User(
                        username = request.username,
                        email = request.email,
                        password = passwordEncoder.encode(request.password),
                        fullName = request.fullName
                    )
                    userRepository.save(user)
                        .map { savedUser ->
                            AuthResponse(
                                accessToken = jwtService.generateAccessToken(savedUser),
                                refreshToken = jwtService.generateRefreshToken(savedUser),
                                expiresIn = 3600,
                                user = UserInfo(
                                    id = savedUser.id!!,
                                    username = savedUser.username,
                                    email = savedUser.email,
                                    fullName = savedUser.fullName,
                                    roles = savedUser.roles
                                )
                            )
                        }
                }
            }
    }

    fun login(request: LoginRequest): Mono<AuthResponse> {
        return userRepository.findByUsername(request.username)
            .filter { user ->
                passwordEncoder.matches(request.password, user.password)
            }
            .map { user ->
                AuthResponse(
                    accessToken = jwtService.generateAccessToken(user),
                    refreshToken = jwtService.generateRefreshToken(user),
                    expiresIn = 3600,
                    user = UserInfo(
                        id = user.id!!,
                        username = user.username,
                        email = user.email,
                        fullName = user.fullName,
                        roles = user.roles
                    )
                )
            }
            .switchIfEmpty(Mono.error(RuntimeException("Invalid credentials")))
    }

    fun validateToken(token: String): Mono<UserInfo> {
        return Mono.fromCallable {
            jwtService.validateToken(token)
        }.flatMap { username ->
            userRepository.findByUsername(username)
                .map { user ->
                    UserInfo(
                        id = user.id!!,
                        username = user.username,
                        email = user.email,
                        fullName = user.fullName,
                        roles = user.roles
                    )
                }
        }
    }

    fun refreshToken(refreshToken: String): Mono<AuthResponse> {
        return validateToken(refreshToken)
            .flatMap { userInfo ->
                userRepository.findByUsername(userInfo.username)
                    .map { user ->
                        AuthResponse(
                            accessToken = jwtService.generateAccessToken(user),
                            refreshToken = refreshToken,
                            expiresIn = 3600,
                            user = userInfo
                        )
                    }
            }
    }

    fun logout(token: String): Mono<Void> {
        // Add token to blacklist in Redis
        return Mono.empty()
    }
}