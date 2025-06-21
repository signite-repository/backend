package com.signite.backend.handler

import com.signite.backend.config.TestConfig
import com.signite.backend.domain.dto.PostFormDTO
import com.signite.backend.domain.dto.UpdateFormDTO
import com.signite.backend.domain.entity.*
import com.signite.backend.router.PostRouter
import com.signite.backend.service.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserters
import reactor.core.publisher.Mono
import java.time.LocalDateTime

@WebFluxTest
@Import(PostHandler::class, PostRouter::class, TestConfig::class)
class PostHandlerTest {
    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockBean
    private lateinit var postService: PostService

    @MockBean
    private lateinit var userContextService: UserContextService

    @MockBean
    private lateinit var validationService: ValidationService

    @MockBean
    private lateinit var categoryService: CategoryService

    @MockBean
    private lateinit var tagService: TagService

    @MockBean
    private lateinit var postToTagService: PostToTagService

    private lateinit var testUser: User
    private lateinit var testCategory: Category
    private lateinit var testTags: List<Tag>
    private lateinit var testPostForm: PostFormDTO

    @BeforeEach
    fun setUp() {
        testUser = User(
            id = 1,
            username = "testuser",
            email = "test@example.com",
            hashedPassword = "",
            imageUrl = "profile.jpg",
            githubUrl = "",
            summary = "테스트 사용자",
        )
        testCategory = Category(
            id = 1,
            title = "테스트 카테고리",
            thumbnail = "",
        )
        testTags = listOf(
            Tag(1, "Kotlin"),
            Tag(2, "Spring"),
        )
        testPostForm = PostFormDTO().apply {
            title = "테스트 게시글"
            summary = "테스트 요약"
            content = "테스트 내용"
            images = "test.jpg"
            categoryTitle = "테스트 카테고리"
            tags = "Kotlin,Spring"
        }
    }

    @Test
    fun `게시글 생성 요청이 성공적으로 처리된다`() {
        val expectedPostDTO = createMockPostDTO()

        whenever(validationService.checkValidForm<PostFormDTO>(any(), any())).thenReturn(Mono.just(testPostForm))
        whenever(userContextService.getCurrentUser(any())).thenReturn(Mono.just(testUser))
        whenever(tagService.createTagParseAndMakeAll(any())).thenReturn(Mono.just(testTags.toMutableList()))
        whenever(categoryService.createCategoryIfNot(any())).thenReturn(Mono.just(testCategory))
        whenever(postService.createPost(any(), any(), any(), any())).thenReturn(Mono.just(expectedPostDTO))
        whenever(categoryService.resetCategoryCash()).thenReturn(Mono.empty())

        webTestClient.post()
            .uri("/api/post")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(testPostForm))
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.title").isEqualTo("테스트 게시글")
            .jsonPath("$.summary").isEqualTo("테스트 요약")
            .jsonPath("$.content").isEqualTo("테스트 내용")
    }

    @Test
    fun `게시글 단일 조회가 성공적으로 처리된다`() {
        val expectedPostDTO = createMockPostDTO()
        whenever(postService.getPost(1)).thenReturn(Mono.just(expectedPostDTO))
        webTestClient.get()
            .uri("/api/post/1")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.id").isEqualTo(1)
            .jsonPath("$.title").isEqualTo("테스트 게시글")
    }

    @Test
    fun `게시글 경로 목록 조회가 성공적으로 처리된다`() {
        val postPaths = listOf(
            com.signite.backend.domain.dto.PostPathDTO().apply {
                id = 1
                title = "첫 번째 게시글"
            },
        )

        whenever(postService.getPostPath()).thenReturn(Mono.just(postPaths))

        webTestClient.get()
            .uri("/api/post/path")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$").isArray
            .jsonPath("$.length()").isEqualTo(1)
    }

    private fun createMockPostDTO() = com.signite.backend.domain.dto.PostDTO().apply {
        id = 1
        title = "테스트 게시글"
        summary = "테스트 요약"
        content = "테스트 내용"
        images = "test.jpg"
        viewcount = 1
        site = null
        createdAt = LocalDateTime.now()
        updatedAt = LocalDateTime.now()
        user = testUser
        category = testCategory
        tags = testTags
    }
}
