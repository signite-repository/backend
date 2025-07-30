package com.signite.backend.domain.entity

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime

@Table(name = "user_organization_role")
class UserRole(
    @Id
    var id: Int = 0,
    @Column("userId")
    var userId: Int = 0,
    @Column("organizationId")
    var organizationId: Int? = null,
    @Column("role")
    var role: String = "ACTIVE_MEMBER",
    @Column("permissions")
    var permissions: String? = null,
    @Column("isActive")
    var isActive: Boolean = true,
    @Column("createdAt")
    var createdAt: LocalDateTime = LocalDateTime.now()
) 