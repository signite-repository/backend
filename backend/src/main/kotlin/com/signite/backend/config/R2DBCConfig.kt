package com.signite.backend.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories
import org.springframework.web.reactive.function.client.WebClient

@Configuration
@EnableR2dbcRepositories
open class R2DBCConfig {
    @Bean
    fun webClient(): WebClient {
        return WebClient.builder().build()
    }
}
