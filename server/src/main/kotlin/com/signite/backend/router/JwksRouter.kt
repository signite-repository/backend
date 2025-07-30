package com.signite.backend.router

import com.signite.backend.handler.JwksHandler
import org.springframework.context.annotation.Bean
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.router

@Component
class JwksRouter(private val handler: JwksHandler) {
    
    @Bean
    fun jwksRouterFunction() =
        router {
            accept(MediaType.APPLICATION_JSON)
                .nest {
                    "/.well-known".nest {
                        GET("/jwks.json", handler::getJwks)
                        GET("/openid_configuration", handler::getOidcConfig)
                    }
                }
        }
} 