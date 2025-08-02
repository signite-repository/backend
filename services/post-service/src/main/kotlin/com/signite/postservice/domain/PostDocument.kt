package com.signite.postservice.domain

import org.springframework.data.annotation.Id
import org.springframework.data.elasticsearch.annotations.Document
import org.springframework.data.elasticsearch.annotations.Field
import org.springframework.data.elasticsearch.annotations.FieldType

// Elasticsearch 인덱스와 매핑될 도큐먼트
@Document(indexName = "posts")
data class PostDocument(
    @Id
    val id: String,

    @Field(type = FieldType.Text, analyzer = "standard") // 임시로 standard analyzer 사용
    val title: String,

    @Field(type = FieldType.Text, analyzer = "standard")
    val content: String,

    @Field(type = FieldType.Keyword)
    val tags: List<String>
)
