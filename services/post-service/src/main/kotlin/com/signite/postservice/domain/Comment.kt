package com.signite.postservice.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime

@Table("comments")
data class Comment(
    @Id
    val id: Long? = null,
    var content: String,
    val postId: Long,
    val authorId: String,
    val parentId: Long? = null,  // null이면 최상위 댓글, 값이 있으면 대댓글
    val depth: Int = 0,          // 댓글 깊이 (0: 최상위, 1: 대댓글, 2: 대대댓글...)
    val path: String = "",       // 계층 경로 (예: "1/3/5")
    val isDeleted: Boolean = false,  // 삭제 여부
    val createdAt: LocalDateTime = LocalDateTime.now(),
    var updatedAt: LocalDateTime = LocalDateTime.now()
)