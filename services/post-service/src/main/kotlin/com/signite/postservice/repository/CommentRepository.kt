package com.signite.postservice.repository

import com.signite.postservice.domain.Comment
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Repository
interface CommentRepository : ReactiveCrudRepository<Comment, Long> {
    // 특정 게시글의 모든 댓글 조회 (path 순서로 정렬)
    fun findByPostIdOrderByPath(postId: Long): Flux<Comment>
    
    // 특정 게시글의 최상위 댓글만 조회
    fun findByPostIdAndDepth(postId: Long, depth: Int): Flux<Comment>
    
    // 특정 댓글의 대댓글 조회
    fun findByParentIdOrderByCreatedAt(parentId: Long): Flux<Comment>
    
    // 삭제되지 않은 댓글만 조회
    fun findByPostIdAndIsDeletedFalseOrderByPath(postId: Long): Flux<Comment>
    
    // 댓글 수 조회
    fun countByPostIdAndIsDeletedFalse(postId: Long): Mono<Long>
    
    // 특정 댓글과 그 하위 댓글들 조회 (path like 사용)
    @Query("SELECT * FROM comments WHERE post_id = :postId AND path LIKE :pathPrefix% ORDER BY path")
    fun findByPostIdAndPathStartingWith(postId: Long, pathPrefix: String): Flux<Comment>
}