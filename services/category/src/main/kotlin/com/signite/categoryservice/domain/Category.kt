package com.signite.categoryservice.domain

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field
import java.time.LocalDateTime

@Document("categories")
data class Category(
    @Id
    val id: String? = null,

    @Field("name")
    var name: String,

    @Field("slug")
    var slug: String,

    @Field("parentId")
    var parentId: String?,

    // MongoDB에서 계층 경로 저장 (예: "notice", "notice/sub1")
    @Field("path")
    var path: String,

    @Field("level")
    var level: Int,

    @Field("displayOrder")
    var displayOrder: Int,

    // MongoDB에서 Map으로 메타데이터 저장
    @Field("metadata")
    var metadata: Map<String, Any>? = emptyMap(),

    @Field("createdAt")
    val createdAt: LocalDateTime? = LocalDateTime.now()
)
