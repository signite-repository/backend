package com.signite.categoryservice.service

import com.signite.categoryservice.web.rest.dto.CategoryResponse
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.*

@Service
class CategoryService(
    private val databaseClient: DatabaseClient
) {

    fun getAllCategoriesAsTree(): Flux<CategoryResponse> {
        return databaseClient.sql("""
            SELECT id, name, slug, parent_id, path, level, display_order, metadata, created_at
            FROM categories 
            ORDER BY display_order
        """)
            .map { row ->
                CategoryResponse(
                    id = row.get("id", UUID::class.java)!!,
                    name = row.get("name", String::class.java)!!,
                    slug = row.get("slug", String::class.java)!!,
                    parentId = row.get("parent_id", UUID::class.java),
                    path = row.get("path", String::class.java)!!,
                    level = row.get("level", Integer::class.java)!!.toInt(),
                    displayOrder = row.get("display_order", Integer::class.java)!!.toInt(),
                    metadata = row.get("metadata", String::class.java),
                    children = emptyList()
                )
            }
            .all()
            .collectList()
            .flatMapMany { categories ->
                val categoryMap = categories.associateBy { it.id }
                val rootCategories = categories.filter { it.parentId == null }
                Flux.fromIterable(rootCategories.map { buildCategoryTree(it, categoryMap) })
            }
    }

    fun getCategoryBySlug(slug: String): Mono<CategoryResponse> {
        return databaseClient.sql("""
            SELECT id, name, slug, parent_id, path, level, display_order, metadata, created_at
            FROM categories 
            WHERE slug = :slug
        """)
            .bind("slug", slug)
            .map { row ->
                CategoryResponse(
                    id = row.get("id", UUID::class.java)!!,
                    name = row.get("name", String::class.java)!!,
                    slug = row.get("slug", String::class.java)!!,
                    parentId = row.get("parent_id", UUID::class.java),
                    path = row.get("path", String::class.java)!!,
                    level = row.get("level", Integer::class.java)!!.toInt(),
                    displayOrder = row.get("display_order", Integer::class.java)!!.toInt(),
                    metadata = row.get("metadata", String::class.java),
                    children = emptyList()
                )
            }
            .one()
    }

    private fun buildCategoryTree(category: CategoryResponse, categoryMap: Map<UUID, CategoryResponse>): CategoryResponse {
        val children = categoryMap.values
            .filter { it.parentId == category.id }
            .sortedBy { it.displayOrder }
            .map { buildCategoryTree(it, categoryMap) }

        return category.copy(children = children)
    }
}