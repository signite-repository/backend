package com.signite.backend.domain.entity

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table

@Table(name = "category")
class Category(
    @Id
    var id: Int = 0,
    @Column("title")
    var title: String? = "",
    @Column("thumbnail")
    var thumbnail: String? = "",
)
