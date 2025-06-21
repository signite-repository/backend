package com.signite.backend.handler

import com.signite.backend.domain.dto.UserFormDTO
import com.signite.backend.domain.entity.User
import com.signite.backend.service.AuthService
import com.signite.backend.service.JwtService
import com.signite.backend.service.PasswordService
import com.signite.backend.service.UserContextService
import com.signite.backend.service.UserRoleService
import com.signite.backend.service.ValidationService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse.badRequest
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.body
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono

@Component
class AuthHandler(
    @Autowired val passwordService: PasswordService,
    @Autowired val validationService: ValidationService,
    @Autowired val authService: AuthService,
    @Autowired val userContextService: UserContextService,
    @Autowired val jwtService: JwtService,
    @Autowired val userRoleService: UserRoleService,
) {
    companion object {
        private val logger = LoggerFactory.getLogger(AuthHandler::class.java)
    }

    fun register(req: ServerRequest) =
        req.bodyToMono(UserFormDTO::class.java)
            .flatMap {
                validationService.checkValidForm<UserFormDTO>(it, mapOf("유저 이름" to it.username, "비밀번호" to it.password))
            }.flatMap {
                validationService.checkValidUsername(it)
            }.flatMap {
                passwordService.changeHashedPassword(it)
            }.flatMap {
                authService.createUser(it)
            }.flatMap { user ->
                userRoleService.createUserRole(user.id, "GUEST_MEMBER")
                    .then(jwtService.generateToken(user, null, "GUEST_MEMBER"))
                    .map { token ->
                        mapOf(
                            "message" to "회원가입이 완료되었습니다",
                            "userId" to user.id,
                            "username" to user.username
                        ) to token
                    }
            }.flatMap { (response, token) ->
                ok().header("Authorization", "Bearer $token")
                    .body(Mono.just(response))
            }.onErrorResume(Exception::class.java) {
                badRequest().body(Mono.just(mapOf("error" to it.message)))
            }

    fun login(req: ServerRequest) =
        req.bodyToMono(UserFormDTO::class.java)
            .flatMap {
                validationService.checkValidForm<UserFormDTO>(it, mapOf("유저 이름" to it.username, "비밀번호" to it.password))
            }.flatMap { userForm ->
                validationService.checkNotValidUsername(userForm).toMono()
            }.flatMap { userForm ->
                authService.getUserByUsername(userForm.username!!).toMono()
                    .flatMap { user ->
                        passwordService.checkPassword(user, userForm.password!!)
                    }
            }.flatMap { user ->
                userRoleService.getUserRole(user!!.id)
                    .onErrorReturn("GUEST_MEMBER")
                    .flatMap { role ->
                        jwtService.generateToken(user, null, role)
                    }
                    .map { token ->
                        mapOf(
                            "message" to "로그인이 완료되었습니다",
                            "userId" to user.id,
                            "username" to user.username
                        ) to token
                    }
            }.flatMap { (response, token) ->
                ok().header("Authorization", "Bearer $token")
                    .body(Mono.just(response))
            }.onErrorResume(Exception::class.java) {
                badRequest().body(Mono.just(mapOf("error" to it.message)))
            }

    fun profile(req: ServerRequest) =
        userContextService.getCurrentUser(req)
            .flatMap { user ->
                ok().body(Mono.just(user))
            }.onErrorResume(Exception::class.java) {
                badRequest().body(Mono.just(mapOf("error" to it.message)))
            }
}
