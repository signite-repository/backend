package com.signite.categoryservice.service

import com.signite.categoryservice.domain.Category
import com.signite.categoryservice.repository.CategoryRepository
import com.signite.categoryservice.web.rest.dto.CategoryResponse
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

class CategoryServiceTest {

    private lateinit var categoryRepository: CategoryRepository
    private lateinit var categoryService: CategoryService

    @BeforeEach
    fun setUp() {
        categoryRepository = mockk()
        categoryService = CategoryService(categoryRepository)
    }

    @Test
    fun `getAllCategoriesAsTree 호출 시 계층 구조로 카테고리를 반환한다`() {
        // given
        val rootCategory = Category(
            id = "1",
            name = "Root",
            slug = "root",
            parentId = null,
            path = "root",
            level = 0,
            displayOrder = 1,
            metadata = mapOf("key" to "value")
        )
        
        val childCategory = Category(
            id = "2",
            name = "Child",
            slug = "child",
            parentId = "1",
            path = "root/child",
            level = 1,
            displayOrder = 1,
            metadata = emptyMap()
        )

        every { categoryRepository.findAll() } returns Flux.just(rootCategory, childCategory)

        // when
        val result = categoryService.getAllCategoriesAsTree()

        // then
        StepVerifier.create(result)
            .expectNextMatches { response ->
                response.id == "1" &&
                response.name == "Root" &&
                response.children?.size == 1 &&
                response.children?.first()?.id == "2"
            }
            .verifyComplete()

        verify(exactly = 1) { categoryRepository.findAll() }
    }

    @Test
    fun `getCategoryBySlug 호출 시 해당 슬러그의 카테고리를 반환한다`() {
        // given
        val category = Category(
            id = "1",
            name = "Test Category",
            slug = "test-category",
            parentId = null,
            path = "test-category",
            level = 0,
            displayOrder = 1,
            metadata = null
        )

        every { categoryRepository.findBySlug("test-category") } returns Mono.just(category)

        // when
        val result = categoryService.getCategoryBySlug("test-category")

        // then
        StepVerifier.create(result)
            .expectNextMatches { response ->
                response.id == "1" &&
                response.name == "Test Category" &&
                response.slug == "test-category"
            }
            .verifyComplete()

        verify(exactly = 1) { categoryRepository.findBySlug("test-category") }
    }

    @Test
    fun `getCategoryBySlug 호출 시 존재하지 않는 슬러그면 빈 값을 반환한다`() {
        // given
        every { categoryRepository.findBySlug("non-existent") } returns Mono.empty()

        // when
        val result = categoryService.getCategoryBySlug("non-existent")

        // then
        StepVerifier.create(result)
            .expectNextCount(0)
            .verifyComplete()

        verify(exactly = 1) { categoryRepository.findBySlug("non-existent") }
    }
}