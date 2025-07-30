package com.signite.backend.repository

import com.signite.backend.domain.entity.User
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Repository
interface UserRepository : ReactiveCrudRepository<User, Int> {
    @Query("SELECT * FROM `users` WHERE username = :username")
    fun findByUsername(username: String): Mono<User>
    
    @Query("SELECT * FROM `users` WHERE username = :username")
    fun findUserByUsernameWithQuery(username: String): Mono<User>

    fun existsByUsername(username: String): Mono<Boolean>

    fun findAllByIdIn(ids: List<Int>): Flux<User>
}
