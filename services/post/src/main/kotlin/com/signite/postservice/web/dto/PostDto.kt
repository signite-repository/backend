package com.signite.postservice.web.dto

import com.signite.postservice.domain.Post
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

data class PostRequest(
    @field:NotBlank(message = "제목은 필수 항목입니다")
    @field:Size(min = 1, max = 200, message = "제목은 1자 이상 200자 이하여야 합니다")
    val title: String,
    
    @field:NotBlank(message = "내용은 필수 항목입니다")
    val content: String,
    
    @field:NotBlank(message = "카테고리는 필수 항목입니다")
    val categoryId: String,
    
    val tags: List<String> = listOf()
)

data class PostUpdateRequest(
    @field:Size(min = 1, max = 200, message = "제목은 1자 이상 200자 이하여야 합니다")
    val title: String?,
    
    val content: String?,
    val tags: List<String>?
)

data class PostResponse(
    val id: Long?,
    val title: String,
    val content: String,
    val authorId: String,
    val categoryId: String,
    val tags: List<String>,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val commentCount: Long = 0
) {
    companion object {
        fun fromEntity(post: Post, commentCount: Long = 0): PostResponse {
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
                updatedAt = post.updatedAt,
                commentCount = commentCount
            )
        }
    }
}
