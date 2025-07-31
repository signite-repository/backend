package com.signite.categoryservice.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime
import java.util.UUID

@Table("categories")
data class Category(
    @Id
    val id: UUID? = null,

    var name: String,

    var slug: String,

    var parentId: UUID?,

    // LTREE 타입은 String으로 매핑하여 처리합니다.
    var path: String,

    var level: Int,

    var displayOrder: Int,

    // JSONB 타입은 String으로 받은 후, ObjectMapper 등으로 파싱하여 사용합니다.
    var metadata: String? = "{}",

    val createdAt: OffsetDateTime? = null
)
