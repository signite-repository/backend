package com.signite.postservice.web.dto

import com.signite.postservice.domain.Post
import java.time.LocalDateTime

data class PostRequest(
    val title: String,
    val content: String,
    val categoryId: String,
    val tags: List<String> = listOf()
)

data class PostResponse(
    val id: String?,
    val title: String,
    val content: String,
    val authorId: String,
    val categoryId: String,
    val tags: List<String>,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    companion object {
        fun fromEntity(post: Post): PostResponse {
            return PostResponse(
                id = post.id,
                title = post.title,
                content = post.content,
                authorId = post.authorId,
                categoryId = post.categoryId,
                tags = post.tags,
                createdAt = post.createdAt,
                updatedAt = post.updatedAt
            )
        }
    }
}
