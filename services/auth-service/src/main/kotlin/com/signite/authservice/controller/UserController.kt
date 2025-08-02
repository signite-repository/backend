package com.signite.authservice.controller

import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono
import com.signite.authservice.service.UserService
import com.signite.authservice.dto.*
import org.springframework.http.codec.multipart.FilePart

/**
 * 사용자 프로필 관련 컨트롤러
 * - 프로필 조회/수정, 비밀번호 변경
 */
@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = ["*"])
class UserController(
    private val userService: UserService
) {
    @GetMapping("/profile")
    fun getProfile(@RequestHeader("Authorization") token: String): Mono<UserProfileResponse> {
        return userService.getProfile(token)
    }

    @PutMapping("/profile")
    fun updateProfile(
        @RequestHeader("Authorization") token: String,
        @RequestBody request: UpdateProfileRequest
    ): Mono<UserProfileResponse> {
        return userService.updateProfile(token, request)
    }

    @PostMapping("/profile/avatar")
    fun uploadAvatar(
        @RequestHeader("Authorization") token: String,
        @RequestPart("file") file: Mono<FilePart>
    ): Mono<Map<String, String>> {
        return userService.uploadAvatar(token, file)
    }
}