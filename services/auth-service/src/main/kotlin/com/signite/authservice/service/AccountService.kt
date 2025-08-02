package com.signite.authservice.service

import com.signite.authservice.dto.*
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class AccountService(
    private val userRepository: UserRepository,
    private val jwtService: JwtService,
    private val passwordEncoder: PasswordEncoder
) {
    fun changePassword(token: String, request: ChangePasswordRequest): Mono<MessageResponse> {
        val jwt = token.removePrefix("Bearer ")
        return Mono.fromCallable {
            jwtService.validateToken(jwt)
        }.flatMap { username ->
            userRepository.findByUsername(username)
                .filter { user ->
                    passwordEncoder.matches(request.currentPassword, user.password)
                }
                .flatMap { user ->
                    user.password = passwordEncoder.encode(request.newPassword)
                    userRepository.save(user)
                        .map { MessageResponse("Password changed successfully") }
                }
                .switchIfEmpty(Mono.just(MessageResponse("Invalid current password", false)))
        }
    }

    fun changeEmail(token: String, request: ChangeEmailRequest): Mono<MessageResponse> {
        val jwt = token.removePrefix("Bearer ")
        return Mono.fromCallable {
            jwtService.validateToken(jwt)
        }.flatMap { username ->
            userRepository.findByUsername(username)
                .filter { user ->
                    passwordEncoder.matches(request.password, user.password)
                }
                .flatMap { user ->
                    userRepository.existsByEmail(request.newEmail)
                        .flatMap { exists ->
                            if (exists) {
                                Mono.just(MessageResponse("Email already in use", false))
                            } else {
                                user.email = request.newEmail
                                user.emailVerified = false
                                userRepository.save(user)
                                    .map { MessageResponse("Email changed successfully") }
                            }
                        }
                }
        }
    }

    fun toggle2FA(token: String, request: Toggle2FARequest): Mono<TwoFactorResponse> {
        // TODO: Implement 2FA
        return Mono.just(TwoFactorResponse(
            enabled = request.enable,
            message = if (request.enable) "2FA enabled" else "2FA disabled"
        ))
    }

    fun deleteAccount(token: String, request: DeleteAccountRequest): Mono<MessageResponse> {
        val jwt = token.removePrefix("Bearer ")
        return Mono.fromCallable {
            jwtService.validateToken(jwt)
        }.flatMap { username ->
            userRepository.findByUsername(username)
                .filter { user ->
                    passwordEncoder.matches(request.password, user.password)
                }
                .flatMap { user ->
                    if (request.hardDelete) {
                        userRepository.delete(user)
                            .then(Mono.just(MessageResponse("Account deleted permanently")))
                    } else {
                        user.enabled = false
                        userRepository.save(user)
                            .map { MessageResponse("Account deactivated") }
                    }
                }
        }
    }
}