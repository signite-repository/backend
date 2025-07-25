package com.signite.backend.domain.entity

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table

@Table(name = "tag")
class Tag(
    @Id
    var id: Int = 0,
    @Column("title")
    val title: String? = "",
)
