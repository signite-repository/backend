package com.signite.backend.handler

import com.signite.backend.config.TestConfig
import com.signite.backend.domain.dto.CategoryDTO
import com.signite.backend.domain.dto.CategoryListDTO
import com.signite.backend.domain.dto.PostInCategoryDTO
import com.signite.backend.router.CategoryRouter
import com.signite.backend.service.CategoryService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@WebFluxTest
@Import(CategoryHandler::class, CategoryRouter::class, TestConfig::class)
class CategoryHandlerTest {
    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockBean
    private lateinit var categoryService: CategoryService

    private lateinit var testCategoryDTOs: List<CategoryDTO>
    private lateinit var testCategoryListDTO: CategoryListDTO

    @BeforeEach
    fun setUp() {
        testCategoryDTOs = listOf(
            CategoryDTO(
                id = 1,
                title = "개발",
                thumbnail = "dev.jpg",
                posts = 5
            ),
            CategoryDTO(
                id = 2,
                title = "디자인",
                thumbnail = "design.jpg",
                posts = 3
            )
        )

        testCategoryListDTO = CategoryListDTO(
            categories = testCategoryDTOs.toMutableList()
        )
    }

    @Test
    fun `카테고리 전체 조회가 성공적으로 처리된다`() {
        whenever(categoryService.getCategoryAll()).thenReturn(Flux.fromIterable(testCategoryDTOs))

        webTestClient.get()
            .uri("/api/category")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$").isArray
            .jsonPath("$.length()").isEqualTo(2)
            .jsonPath("$[0].id").isEqualTo(1)
            .jsonPath("$[0].title").isEqualTo("개발")
            .jsonPath("$[1].id").isEqualTo(2)
            .jsonPath("$[1].title").isEqualTo("디자인")
    }

    @Test
    fun `카테고리 캐시 조회가 성공적으로 처리된다`() {
        whenever(categoryService.getAllAndCache()).thenReturn(Mono.just(testCategoryDTOs.toMutableList()))

        webTestClient.get()
            .uri("/api/category/cache")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$").isArray
            .jsonPath("$.length()").isEqualTo(2)
            .jsonPath("$[0].title").isEqualTo("개발")
            .jsonPath("$[1].title").isEqualTo("디자인")
    }

    @Test
    fun `카테고리별 게시글 조회가 성공적으로 처리된다`() {
        val postInCategoryList = listOf(
            PostInCategoryDTO(
                id = 1,
                title = "테스트 게시글",
                summary = "테스트 요약"
            )
        )

        whenever(categoryService.getAllPostByCategoryId(any(), any(), any()))
            .thenReturn(Mono.just(postInCategoryList))

        webTestClient.get()
            .uri("/api/category/1")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$").isArray
            .jsonPath("$.length()").isEqualTo(1)
            .jsonPath("$[0].title").isEqualTo("테스트 게시글")
    }

    @Test
    fun `페이징이 포함된 카테고리별 게시글 조회가 성공적으로 처리된다`() {
        val postInCategoryList = emptyList<PostInCategoryDTO>()

        whenever(categoryService.getAllPostByCategoryId(eq(1), eq(8), eq(8)))
            .thenReturn(Mono.just(postInCategoryList))

        webTestClient.get()
            .uri("/api/category/1?page=2&limit=8")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$").isArray
            .jsonPath("$.length()").isEqualTo(0)
    }

    @Test
    fun `카테고리 조회시 오류가 발생한다`() {
        whenever(categoryService.getCategoryAll())
            .thenReturn(Flux.error(Exception("데이터베이스 연결 오류")))

        webTestClient.get()
            .uri("/api/category")
            .exchange()
            .expectStatus().is5xxServerError
    }

    @Test
    fun `카테고리 캐시 조회시 오류가 발생한다`() {
        whenever(categoryService.getAllAndCache())
            .thenReturn(Mono.error(Exception("캐시 서버 오류")))

        webTestClient.get()
            .uri("/api/category/cache")
            .exchange()
            .expectStatus().isBadRequest
    }

    @Test
    fun `존재하지 않는 카테고리로 게시글 조회시 오류가 발생한다`() {
        whenever(categoryService.getAllPostByCategoryId(any(), any(), any()))
            .thenReturn(Mono.error(Exception("존재하지 않는 카테고리입니다")))

        webTestClient.get()
            .uri("/api/category/999")
            .exchange()
            .expectStatus().isBadRequest
    }

    @Test
    fun `잘못된 페이지 파라미터로 카테고리별 게시글 조회시 기본값이 적용된다`() {
        val postInCategoryList = emptyList<PostInCategoryDTO>()

        whenever(categoryService.getAllPostByCategoryId(eq(1), eq(0), eq(8)))
            .thenReturn(Mono.just(postInCategoryList))

        webTestClient.get()
            .uri("/api/category/1?page=invalid")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$").isArray
            .jsonPath("$.length()").isEqualTo(0)
    }
}