package com.signite.backend.domain.dto

import java.time.LocalDateTime

class CommentDTO(
    var id: Int = 0,
    var content: String? = "",
    var user: UserDTO? = null,

    var createdAt: LocalDateTime? = LocalDateTime.now(),
    var userId: Int = 0,
    var commentId: Int = 0,
)
