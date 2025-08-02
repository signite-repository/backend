package com.signite.categoryservice.web.rest.dto

import com.signite.categoryservice.domain.Category
/**
 * 카테고리 생성 및 수정을 위한 요청 DTO
 */
data class CategoryRequest(
    val name: String,
    val slug: String,
    val parentId: String?,
    val displayOrder: Int = 0,
    val metadata: String? = "{}"
)

/**
 * 클라이언트에 반환될 카테고리 정보 DTO
 */
data class CategoryResponse(
    val id: String,
    val name: String,
    val slug: String,
    val parentId: String?,
    val path: String,
    val level: Int,
    val displayOrder: Int,
    val metadata: Map<String, Any>?,
    val children: List<CategoryResponse>? = null // 하위 카테고리를 포함할 수 있음
) {
    companion object {
        fun fromEntity(category: Category, children: List<CategoryResponse>? = null): CategoryResponse {
            return CategoryResponse(
                id = category.id!!,
                name = category.name,
                slug = category.slug,
                parentId = category.parentId,
                path = category.path,
                level = category.level,
                displayOrder = category.displayOrder,
                metadata = category.metadata,
                children = children
            )
        }
    }
}
