package com.signite.authservice.service

import com.signite.authservice.dto.*
import com.signite.authservice.domain.User
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import org.springframework.http.codec.multipart.FilePart

@Service
class UserService(
    private val userRepository: UserRepository,
    private val jwtService: JwtService
) {
    fun getProfile(token: String): Mono<UserProfileResponse> {
        val jwt = token.removePrefix("Bearer ")
        return Mono.fromCallable {
            jwtService.validateToken(jwt)
        }.flatMap { username ->
            userRepository.findByUsername(username)
                .map { user ->
                    UserProfileResponse(
                        id = user.id!!,
                        username = user.username,
                        email = user.email,
                        fullName = user.fullName,
                        bio = user.bio,
                        avatarUrl = user.avatarUrl,
                        phoneNumber = user.phoneNumber,
                        location = user.location,
                        website = user.website,
                        emailVerified = user.emailVerified,
                        twoFactorEnabled = user.twoFactorEnabled,
                        roles = user.roles,
                        createdAt = user.createdAt.toString(),
                        updatedAt = user.updatedAt?.toString()
                    )
                }
        }
    }

    fun updateProfile(token: String, request: UpdateProfileRequest): Mono<UserProfileResponse> {
        val jwt = token.removePrefix("Bearer ")
        return Mono.fromCallable {
            jwtService.validateToken(jwt)
        }.flatMap { username ->
            userRepository.findByUsername(username)
                .flatMap { user ->
                    request.fullName?.let { user.fullName = it }
                    request.bio?.let { user.bio = it }
                    request.phoneNumber?.let { user.phoneNumber = it }
                    request.location?.let { user.location = it }
                    request.website?.let { user.website = it }
                    
                    userRepository.save(user)
                        .map { savedUser ->
                            UserProfileResponse(
                                id = savedUser.id!!,
                                username = savedUser.username,
                                email = savedUser.email,
                                fullName = savedUser.fullName,
                                bio = savedUser.bio,
                                avatarUrl = savedUser.avatarUrl,
                                phoneNumber = savedUser.phoneNumber,
                                location = savedUser.location,
                                website = savedUser.website,
                                emailVerified = savedUser.emailVerified,
                                twoFactorEnabled = savedUser.twoFactorEnabled,
                                roles = savedUser.roles,
                                createdAt = savedUser.createdAt.toString(),
                                updatedAt = savedUser.updatedAt?.toString()
                            )
                        }
                }
        }
    }

    fun uploadAvatar(token: String, file: Mono<FilePart>): Mono<Map<String, String>> {
        // TODO: Implement file upload
        return Mono.just(mapOf("avatarUrl" to "/uploads/avatar.jpg"))
    }
}