package com.signite.backend.domain.dto

import com.signite.backend.domain.entity.Category
import com.signite.backend.domain.entity.Comment
import com.signite.backend.domain.entity.Tag
import com.signite.backend.domain.entity.User
import java.time.LocalDateTime

class PostDTO(
    var id: Int = 0,
    var title: String? = "",
    var summary: String? = "",
    var content: String? = "",
    var images: String? = "",
    var viewcount: Int = 0,
    var site: String? = "",
    var createdAt: LocalDateTime? = LocalDateTime.now(),
    var updatedAt: LocalDateTime? = LocalDateTime.now(),
    var user: User? = null,
    var category: Category? = null,
    var tags: List<Tag>? = null,
    var comments: List<Comment>? = null,
)
