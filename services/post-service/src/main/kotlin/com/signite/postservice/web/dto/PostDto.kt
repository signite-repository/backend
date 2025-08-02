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
    val id: Long?,
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
            // JSON 문자열을 List<String>으로 파싱
            val tagList = try {
                post.tags.removeSurrounding("[", "]")
                    .split(",")
                    .map { it.trim().removeSurrounding("\"") }
                    .filter { it.isNotBlank() }
            } catch (e: Exception) {
                emptyList<String>()
            }
            
            return PostResponse(
                id = post.id,
                title = post.title,
                content = post.content,
                authorId = post.authorId,
                categoryId = post.categoryId,
                tags = tagList,
                createdAt = post.createdAt,
                updatedAt = post.updatedAt
            )
        }
    }
}
