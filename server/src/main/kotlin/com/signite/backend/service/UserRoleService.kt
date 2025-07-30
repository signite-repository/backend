package com.signite.backend.service

import com.signite.backend.domain.entity.UserRole
import com.signite.backend.repository.UserRoleRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class UserRoleService(
    @Autowired private val userRoleRepository: UserRoleRepository
) {
    
    fun createUserRole(userId: Int, role: String = "ACTIVE_MEMBER", organizationId: Int? = null): Mono<UserRole> {
        val userRole = UserRole(
            id = 0,
            userId = userId,
            organizationId = organizationId,
            role = role,
            permissions = null,
            isActive = true,
            createdAt = java.time.LocalDateTime.now()
        )
        return userRoleRepository.save(userRole)
    }
    
    fun getUserRole(userId: Int, organizationId: Int? = null): Mono<String> {
        return if (organizationId != null) {
            userRoleRepository.findByUserIdAndOrganizationIdAndIsActive(userId, organizationId, true)
                .map { it.role }
                .defaultIfEmpty("GUEST_MEMBER")
        } else {
            userRoleRepository.findByUserIdAndIsActiveOrderByCreatedAtDesc(userId, true)
                .next()
                .map { it.role }
                .defaultIfEmpty("ACTIVE_MEMBER")
        }
    }
    
    fun getUserOrganizationId(userId: Int): Mono<Int?> {
        return userRoleRepository.findByUserIdAndIsActiveOrderByCreatedAtDesc(userId, true)
            .next()
            .map { it.organizationId }
            .switchIfEmpty(Mono.empty())
    }
    
    fun updateUserRole(userId: Int, newRole: String, organizationId: Int? = null): Mono<UserRole> {
        return if (organizationId != null) {
            userRoleRepository.findByUserIdAndOrganizationIdAndIsActive(userId, organizationId, true)
                .flatMap { existingRole ->
                    existingRole.role = newRole
                    userRoleRepository.save(existingRole)
                }
                .switchIfEmpty(createUserRole(userId, newRole, organizationId))
        } else {
            userRoleRepository.findByUserIdAndIsActiveOrderByCreatedAtDesc(userId, true)
                .next()
                .flatMap { existingRole ->
                    existingRole.role = newRole
                    userRoleRepository.save(existingRole)
                }
                .switchIfEmpty(createUserRole(userId, newRole))
        }
    }
} 