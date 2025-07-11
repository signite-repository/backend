package com.signite.backend.repository

import com.signite.backend.domain.entity.PostToTag
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono

@Repository
interface PostToTagRepository : ReactiveCrudRepository<PostToTag, Int> {
    fun deleteByPostId(postId: Int): Mono<Void>

    fun deleteByTagId(tagId: Int): Mono<Void>
}
