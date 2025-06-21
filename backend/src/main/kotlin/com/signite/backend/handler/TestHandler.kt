package com.signite.backend.handler

import com.signite.backend.service.UserContextService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono

@Component
class TestHandler(
    @Autowired private val userContextService: UserContextService
) {
    
    fun healthCheck(req: ServerRequest): Mono<ServerResponse> {
        return ServerResponse.ok()
            .bodyValue(mapOf(
                "status" to "OK",
                "service" to "signite-backend",
                "timestamp" to System.currentTimeMillis()
            ))
    }
    
    fun authTest(req: ServerRequest): Mono<ServerResponse> {
        return userContextService.getCurrentUser(req)
            .map { user ->
                mapOf(
                    "authenticated" to true,
                    "user" to mapOf(
                        "id" to user.id,
                        "username" to user.username,
                        "email" to user.email
                    ),
                    "role" to userContextService.getCurrentUserRole(req),
                    "organizationId" to userContextService.getCurrentOrganizationId(req),
                    "timestamp" to System.currentTimeMillis()
                )
            }
            .flatMap { data ->
                ServerResponse.ok().bodyValue(data)
            }
            .onErrorResume { error ->
                ServerResponse.ok().bodyValue(mapOf(
                    "authenticated" to false,
                    "error" to error.message,
                    "timestamp" to System.currentTimeMillis()
                ))
            }
    }
    
    fun roleTest(req: ServerRequest): Mono<ServerResponse> {
        val requiredRole = req.queryParam("role").orElse("ACTIVE_MEMBER")
        
        return ServerResponse.ok()
            .bodyValue(mapOf(
                "hasRole" to userContextService.hasRole(req, requiredRole),
                "requiredRole" to requiredRole,
                "userRole" to userContextService.getCurrentUserRole(req),
                "timestamp" to System.currentTimeMillis()
            ))
    }
} 