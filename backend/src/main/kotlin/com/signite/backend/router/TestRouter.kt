package com.signite.backend.router

import com.signite.backend.handler.TestHandler
import org.springframework.context.annotation.Bean
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.router

@Component
class TestRouter(private val handler: TestHandler) {
    
    @Bean
    fun testRouterFunction() =
        router {
            accept(MediaType.APPLICATION_JSON)
                .nest {
                    "/api/test".nest {
                        GET("/health", handler::healthCheck)
                        GET("/auth", handler::authTest)
                        GET("/role", handler::roleTest)
                    }
                }
        }
} 