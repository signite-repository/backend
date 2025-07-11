package com.signite.backend.service

import com.signite.backend.domain.dto.UserFormDTO
import com.signite.backend.domain.entity.User
import com.signite.backend.repository.UserCacheRepository
import com.signite.backend.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class AuthService(
        private val userRepository: UserRepository,
        private val userCacheRepository: UserCacheRepository,
) {
    companion object {
        private val logger = LoggerFactory.getLogger(AuthService::class.java)
    }

    // 유저 생성
    fun createUser(userForm: UserFormDTO): Mono<User> {
        val user =
                User(
                        username = userForm.username,
                        hashedPassword = userForm.password,
                )
        return userRepository.save(user)
    }

    // 유저 아이디로 가져오기
    fun getUserById(userId: Int): Mono<User> {
        return userRepository.findById(userId).flatMap {
            if (it == null) {
                throw error("유저가 없습니다")
            } else {
                Mono.just(it)
            }
        }
    }

    // 유저 가져오기
    fun getUser(user: User): Mono<User> {
        return userRepository.findById(user.id).flatMap {
            if (it == null) {
                throw error("유저가 없습니다")
            } else {
                Mono.just(it)
            }
        }
    }

    fun getUserByUsername(username: String): Mono<User> {
        return userRepository.findByUsername(username)
    }
}
