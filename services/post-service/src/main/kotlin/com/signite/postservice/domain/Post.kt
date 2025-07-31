package com.signite.postservice.domain

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime
import java.util.UUID

@Document(collection = "posts")
data class Post(
    @Id
    val id: String? = null,
    var title: String,
    var content: String,
    val authorId: String,
    val categoryId: String,
    var tags: List<String> = listOf(),
    val createdAt: LocalDateTime = LocalDateTime.now(),
    var updatedAt: LocalDateTime = LocalDateTime.now()
)
