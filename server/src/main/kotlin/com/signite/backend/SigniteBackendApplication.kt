package com.signite.backend

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching

@SpringBootApplication @EnableCaching
class SigniteBackendApplication

fun main(args: Array<String>) {
    runApplication<SigniteBackendApplication>(*args)
}
