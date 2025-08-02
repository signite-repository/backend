package com.signite.categoryservice.web.rest

import com.signite.categoryservice.service.CategoryService
import com.signite.categoryservice.web.rest.dto.CategoryRequest
import com.signite.categoryservice.web.rest.dto.CategoryResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.net.URI

@RestController
@RequestMapping("/api/v1/categories")
class CategoryResource(private val categoryService: CategoryService) {

    @GetMapping
    fun getAllCategoriesTree(): Flux<CategoryResponse> {
        return categoryService.getAllCategoriesAsTree()
    }

    @GetMapping("/{slug}")
    fun getCategoryBySlug(@PathVariable slug: String): Mono<ResponseEntity<CategoryResponse>> {
        return categoryService.getCategoryBySlug(slug)
            .map { ResponseEntity.ok(it) }
            .defaultIfEmpty(ResponseEntity.notFound().build())
    }
}
