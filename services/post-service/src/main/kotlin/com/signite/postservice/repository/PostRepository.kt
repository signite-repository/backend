package com.signite.postservice.repository

import com.signite.postservice.domain.Post
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Repository
interface PostRepository : ReactiveCrudRepository<Post, Long> {
    fun findAllBy(pageable: Pageable): Flux<Post>
    fun findByAuthorId(authorId: String, pageable: Pageable): Flux<Post>
    fun findByCategoryId(categoryId: String, pageable: Pageable): Flux<Post>
    fun findByCategoryIdAndAuthorId(categoryId: String, authorId: String, pageable: Pageable): Flux<Post>
    
    fun countByAuthorId(authorId: String): Mono<Long>
    fun countByCategoryId(categoryId: String): Mono<Long>
    fun countByCategoryIdAndAuthorId(categoryId: String, authorId: String): Mono<Long>
}
