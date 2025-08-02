package com.signite.categoryservice.service

import com.signite.categoryservice.domain.Category
import com.signite.categoryservice.repository.CategoryRepository
import com.signite.categoryservice.web.rest.dto.CategoryResponse
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Service
class CategoryService(
    private val categoryRepository: CategoryRepository,
    private val mongoTemplate: ReactiveMongoTemplate
) {

    fun getAllCategoriesAsTree(): Flux<CategoryResponse> {
        return categoryRepository.findAll()
            .map { category ->
                CategoryResponse(
                    id = category.id!!,
                    name = category.name,
                    slug = category.slug,
                    parentId = category.parentId,
                    path = category.path,
                    level = category.level,
                    displayOrder = category.displayOrder,
                    metadata = category.metadata,
                    children = emptyList()
                )
            }
            .collectList()
            .flatMapMany { categories ->
                val categoryMap = categories.associateBy { it.id }
                val rootCategories = categories.filter { it.parentId == null }
                Flux.fromIterable(rootCategories.map { buildCategoryTree(it, categoryMap) })
            }
    }

    fun getCategoryBySlug(slug: String): Mono<CategoryResponse> {
        return categoryRepository.findBySlug(slug)
            .map { category ->
                CategoryResponse(
                    id = category.id!!,
                    name = category.name,
                    slug = category.slug,
                    parentId = category.parentId,
                    path = category.path,
                    level = category.level,
                    displayOrder = category.displayOrder,
                    metadata = category.metadata,
                    children = emptyList()
                )
            }
    }

    private fun buildCategoryTree(category: CategoryResponse, categoryMap: Map<String, CategoryResponse>): CategoryResponse {
        val children = categoryMap.values
            .filter { it.parentId == category.id }
            .sortedBy { it.displayOrder }
            .map { buildCategoryTree(it, categoryMap) }

        return category.copy(children = children)
    }
}