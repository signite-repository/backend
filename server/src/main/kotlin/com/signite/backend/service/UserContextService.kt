package com.signite.backend.service

import com.signite.backend.domain.entity.User
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.server.ServerRequest
import reactor.core.publisher.Mono

@Service
class UserContextService {
    
    fun getCurrentUser(req: ServerRequest): Mono<User> {
        val headers = req.headers().asHttpHeaders()
        
        val userId = headers.getFirst("X-User-Id")?.toIntOrNull()
        val username = headers.getFirst("X-User-Name")
        val email = headers.getFirst("X-User-Email")
        val role = headers.getFirst("X-User-Role")
        val organizationId = headers.getFirst("X-Organization-Id")?.toIntOrNull()
        
        return if (userId != null && username != null) {
            Mono.just(User(
                id = userId,
                username = username,
                email = email ?: "",
                hashedPassword = "",
                imageUrl = headers.getFirst("X-User-Image"),
                githubUrl = headers.getFirst("X-User-Github"),
                summary = headers.getFirst("X-User-Summary")
            ))
        } else {
            Mono.error(RuntimeException("사용자 정보를 찾을 수 없습니다"))
        }
    }
    
    fun getCurrentUserRole(req: ServerRequest): String? {
        return req.headers().asHttpHeaders().getFirst("X-User-Role")
    }
    
    fun getCurrentOrganizationId(req: ServerRequest): Int? {
        return req.headers().asHttpHeaders().getFirst("X-Organization-Id")?.toIntOrNull()
    }
    
    fun hasRole(req: ServerRequest, requiredRole: String): Boolean {
        val userRole = getCurrentUserRole(req)
        return userRole == requiredRole || isHigherRole(userRole, requiredRole)
    }
    
    private fun isHigherRole(userRole: String?, requiredRole: String): Boolean {
        val roleHierarchy = mapOf(
            "SUPER_ADMIN" to 7,
            "HQ_ADMIN" to 6,
            "BRANCH_ADMIN" to 5,
            "SIG_ADMIN" to 4,
            "BOARD_MODERATOR" to 3,
            "ACTIVE_MEMBER" to 2,
            "GUEST_MEMBER" to 1
        )
        
        val userLevel = roleHierarchy[userRole] ?: 0
        val requiredLevel = roleHierarchy[requiredRole] ?: 0
        
        return userLevel >= requiredLevel
    }
    
    fun checkOwnership(req: ServerRequest, resourceUserId: Int): Boolean {
        val currentUser = getCurrentUser(req).block()
        val userRole = getCurrentUserRole(req)
        
        return currentUser?.id == resourceUserId || 
               hasRole(req, "SIG_ADMIN")
    }
} 