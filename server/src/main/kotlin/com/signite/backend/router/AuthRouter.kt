package com.signite.backend.router

import com.signite.backend.handler.AuthHandler
import org.springframework.context.annotation.Bean
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.router

@Component
class AuthRouter(private val handler: AuthHandler) {
    @Bean
    fun authRouterFunction() =
        router {
            accept(MediaType.APPLICATION_JSON)
                .nest {
                    "/api/auth".nest {
                        POST("/register", handler::register)
                        POST("/login", handler::login)
                        GET("/profile", handler::profile)
                    }
                }
        }
}
