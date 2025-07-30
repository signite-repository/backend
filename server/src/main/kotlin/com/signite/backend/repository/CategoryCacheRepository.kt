package com.signite.backend.repository

import com.signite.backend.domain.dto.CategoryDTO
import com.signite.backend.domain.dto.CategoryListDTO
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono

@Repository
class CategoryCacheRepository(
    private val categoryRepository: CategoryRepository,
    private val redisTemplate: ReactiveRedisTemplate<String?, CategoryListDTO>,
) {
    companion object {
        val DAYS_TO_LIVE = 1L
    }

    fun findAllAndCaching(): Mono<CategoryListDTO> {
        return redisTemplate.opsForValue().get("categoryAll")
    }

    fun setCategoriesAllAndCaching(categoriesDTO: MutableList<CategoryDTO>): Mono<CategoryListDTO> {
        redisTemplate.opsForValue().set(
            "categoryAll",
            CategoryListDTO(categories = categoriesDTO),
            java.time.Duration.ofDays(DAYS_TO_LIVE),
        ).subscribe()
        return redisTemplate.opsForValue().get("categoryAll")
    }
}
