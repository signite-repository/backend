package com.signite.backend.domain.dto

import java.time.LocalDateTime

class PostInCategoryDTO(
    var id: Int = 0,
    var title: String? = "",
    var summary: String? = "",
    var images: String? = "",
    var viewcount: Int = 0,
    var postcount: Int = 0,
    var commentcount: Int = 0,
    var last: Int = 0,
    var createdAt: LocalDateTime? = LocalDateTime.now(),
    var user: UserInPostCategoryDTO? = null,
)
