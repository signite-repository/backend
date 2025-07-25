package com.signite.backend.router

import com.signite.backend.handler.CategoryHandler
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.router

@Component
class CategoryRouter(private val handler: CategoryHandler) {
    @Bean
    fun categoryRouterFunction() =
        router {
            "/api/category".nest {
                GET("", handler::getAll)
                GET("/cache", handler::getAllAndCache)
                GET("/{categoryId}", handler::getAllPostByCategoryId)
            }
        }
}
