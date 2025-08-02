package com.signite.categoryservice.web.rest

import com.signite.categoryservice.service.CategoryService
import com.signite.categoryservice.web.rest.dto.CategoryResponse
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class CategoryResourceTest {

    private lateinit var webTestClient: WebTestClient
    private lateinit var categoryService: CategoryService

    @BeforeEach
    fun setUp() {
        categoryService = mockk()
        val categoryResource = CategoryResource(categoryService)
        webTestClient = WebTestClient.bindToController(categoryResource).build()
    }

    @Test
    fun `GET api-v1-categories는 트리 구조의 카테고리 목록을 반환한다`() {
        // given
        val rootCategory = CategoryResponse(
            id = "1",
            name = "Root",
            slug = "root",
            parentId = null,
            path = "root",
            level = 0,
            displayOrder = 1,
            metadata = emptyMap(),
            children = listOf(
                CategoryResponse(
                    id = "2",
                    name = "Child",
                    slug = "child",
                    parentId = "1",
                    path = "root/child",
                    level = 1,
                    displayOrder = 1,
                    metadata = null,
                    children = emptyList()
                )
            )
        )

        every { categoryService.getAllCategoriesAsTree() } returns Flux.just(rootCategory)

        // when & then
        webTestClient.get()
            .uri("/api/v1/categories")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBodyList(CategoryResponse::class.java)
            .hasSize(1)
            .contains(rootCategory)

        verify(exactly = 1) { categoryService.getAllCategoriesAsTree() }
    }

    @Test
    fun `GET api-v1-categories는 빈 목록을 반환할 수 있다`() {
        // given
        every { categoryService.getAllCategoriesAsTree() } returns Flux.empty()

        // when & then
        webTestClient.get()
            .uri("/api/v1/categories")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBodyList(CategoryResponse::class.java)
            .hasSize(0)

        verify(exactly = 1) { categoryService.getAllCategoriesAsTree() }
    }

    @Test
    fun `GET api-v1-categories-slug는 특정 슬러그의 카테고리를 반환한다`() {
        // given
        val category = CategoryResponse(
            id = "1",
            name = "Test Category",
            slug = "test-category",
            parentId = null,
            path = "test-category",
            level = 0,
            displayOrder = 1,
            metadata = mapOf("key" to "value")
        )

        every { categoryService.getCategoryBySlug("test-category") } returns Mono.just(category)

        // when & then
        webTestClient.get()
            .uri("/api/v1/categories/slug/test-category")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody(CategoryResponse::class.java)
            .isEqualTo(category)

        verify(exactly = 1) { categoryService.getCategoryBySlug("test-category") }
    }

    @Test
    fun `GET api-v1-categories-slug는 존재하지 않는 슬러그에 대해 404를 반환한다`() {
        // given
        every { categoryService.getCategoryBySlug("non-existent") } returns Mono.empty()

        // when & then
        webTestClient.get()
            .uri("/api/v1/categories/slug/non-existent")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isNotFound

        verify(exactly = 1) { categoryService.getCategoryBySlug("non-existent") }
    }
}