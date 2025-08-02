package com.signite.postservice.web.dto

import com.signite.postservice.domain.Comment
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

data class CommentRequest(
    @field:NotBlank(message = "댓글 내용은 필수 항목입니다")
    @field:Size(min = 1, max = 1000, message = "댓글은 1자 이상 1000자 이하여야 합니다")
    val content: String,
    
    val parentId: Long? = null  // 대댓글인 경우 부모 댓글 ID
)

data class CommentUpdateRequest(
    @field:NotBlank(message = "댓글 내용은 필수 항목입니다")
    @field:Size(min = 1, max = 1000, message = "댓글은 1자 이상 1000자 이하여야 합니다")
    val content: String
)

data class CommentResponse(
    val id: Long,
    val content: String,
    val postId: Long,
    val authorId: String,
    val parentId: Long?,
    val depth: Int,
    val isDeleted: Boolean,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val children: List<CommentResponse> = emptyList()  // 하위 댓글들
) {
    companion object {
        fun fromEntity(comment: Comment, children: List<CommentResponse> = emptyList()): CommentResponse {
            return CommentResponse(
                id = comment.id!!,
                content = if (comment.isDeleted) "삭제된 댓글입니다" else comment.content,
                postId = comment.postId,
                authorId = if (comment.isDeleted) "" else comment.authorId,
                parentId = comment.parentId,
                depth = comment.depth,
                isDeleted = comment.isDeleted,
                createdAt = comment.createdAt,
                updatedAt = comment.updatedAt,
                children = children
            )
        }
    }
}