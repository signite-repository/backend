package com.signite.backend.handler

import com.signite.backend.service.JwtService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono

@Component
class JwksHandler(
    @Autowired private val jwtService: JwtService
) {
    
    fun getJwks(req: ServerRequest): Mono<ServerResponse> {
        val jwks = mapOf(
            "keys" to listOf(
                mapOf(
                    "kty" to "oct",
                    "kid" to "signite-key-1",
                    "use" to "sig",
                    "alg" to "HS256"
                )
            )
        )
        
        return ServerResponse.ok()
            .header("Content-Type", "application/json")
            .bodyValue(jwks)
    }
    
    fun getOidcConfig(req: ServerRequest): Mono<ServerResponse> {
        val config = mapOf(
            "issuer" to "https://auth.signite.com",
            "jwks_uri" to "https://auth.signite.com/.well-known/jwks.json",
            "authorization_endpoint" to "https://auth.signite.com/auth",
            "token_endpoint" to "https://auth.signite.com/token",
            "userinfo_endpoint" to "https://auth.signite.com/userinfo",
            "response_types_supported" to listOf("code", "token", "id_token"),
            "subject_types_supported" to listOf("public"),
            "id_token_signing_alg_values_supported" to listOf("HS256"),
            "scopes_supported" to listOf("openid", "profile", "email")
        )
        
        return ServerResponse.ok()
            .header("Content-Type", "application/json")
            .bodyValue(config)
    }
} 