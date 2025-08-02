package com.signite.postservice.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime

@Table("posts")
data class Post(
    @Id
    val id: Long? = null,
    var title: String,
    var content: String,
    val authorId: String,
    val categoryId: String,  // Category와 타입 통일 (String)
    var tags: String = "[]", // JSON 문자열로 저장
    val createdAt: LocalDateTime = LocalDateTime.now(),
    var updatedAt: LocalDateTime = LocalDateTime.now()
)
