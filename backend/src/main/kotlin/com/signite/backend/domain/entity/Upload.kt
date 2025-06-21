package com.signite.backend.domain.entity

import org.springframework.data.relational.core.mapping.Column

class Upload(
    @Column("originalname")
    var originalname: String? = "",
    @Column("location")
    var location: String? = "",
)
