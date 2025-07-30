package com.signite.backend.repository

import com.signite.backend.domain.entity.UserRole
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Repository
interface UserRoleRepository : ReactiveCrudRepository<UserRole, Int> {
    
    fun findByUserIdAndIsActive(userId: Int, isActive: Boolean): Flux<UserRole>
    
    fun findByUserIdAndOrganizationIdAndIsActive(
        userId: Int, 
        organizationId: Int, 
        isActive: Boolean
    ): Mono<UserRole>
    
    fun findByUserIdAndIsActiveOrderByCreatedAtDesc(userId: Int, isActive: Boolean): Flux<UserRole>
    
    fun existsByUserIdAndOrganizationId(userId: Int, organizationId: Int): Mono<Boolean>
} 