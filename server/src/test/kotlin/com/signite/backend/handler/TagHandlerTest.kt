package com.signite.backend.handler

import com.signite.backend.config.TestConfig
import com.signite.backend.domain.dto.PostInCategoryDTO
import com.signite.backend.domain.entity.Tag
import com.signite.backend.router.TagRouter
import com.signite.backend.service.TagService
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
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@WebFluxTest
@Import(TagHandler::class, TagRouter::class, TestConfig::class)
class TagHandlerTest {
    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockBean
    private lateinit var tagService: TagService

    private lateinit var testTags: List<Tag>
    private lateinit var testTag: Tag

    @BeforeEach
    fun setUp() {
        testTags = listOf(
            Tag(1, "Kotlin"),
            Tag(2, "Spring"),
            Tag(3, "React")
        )

        testTag = Tag(4, "JavaScript")
    }

    @Test
    fun `태그 전체 조회가 성공적으로 처리된다`() {
        whenever(tagService.getTagAllContainPost()).thenReturn(Flux.fromIterable(testTags))

        webTestClient.get()
            .uri("/api/tag")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$").isArray
            .jsonPath("$.length()").isEqualTo(3)
            .jsonPath("$[0].title").isEqualTo("Kotlin")
            .jsonPath("$[1].title").isEqualTo("Spring")
            .jsonPath("$[2].title").isEqualTo("React")
    }

    @Test
    fun `태그 생성이 성공적으로 처리된다`() {
        val newTags = mutableListOf(testTag)
        
        whenever(tagService.createTagParseAndMakeAll(any())).thenReturn(Mono.just(newTags))

        webTestClient.post()
            .uri("/api/tag")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(testTag))
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$").isArray
            .jsonPath("$.length()").isEqualTo(1)
            .jsonPath("$[0].title").isEqualTo("JavaScript")
    }

    @Test
    fun `게시글ID로 태그 삭제가 성공적으로 처리된다`() {
        whenever(tagService.deleteTagsByPostID(1)).thenReturn(Mono.empty())

        webTestClient.delete()
            .uri("/api/tag/post/1")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.message").isEqualTo("포스트: 1 삭제가 완료되었습니다")
    }

    @Test
    fun `태그ID로 연결 삭제가 성공적으로 처리된다`() {
        whenever(tagService.deleteTagsByTagID(1)).thenReturn(Mono.empty())

        webTestClient.delete()
            .uri("/api/tag/tag/1")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.message").isEqualTo("포스트: 1 삭제가 완료되었습니다")
    }

    @Test
    fun `태그별 게시글 조회가 성공적으로 처리된다`() {
        val postInCategoryList = emptyList<PostInCategoryDTO>()

        whenever(tagService.getAllPostByTagId(any(), any(), any()))
            .thenReturn(Mono.just(postInCategoryList))

        webTestClient.get()
            .uri("/api/tag/1")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$").isArray
            .jsonPath("$.length()").isEqualTo(0)
    }

    @Test
    fun `페이징이 포함된 태그별 게시글 조회가 성공적으로 처리된다`() {
        val postInCategoryList = emptyList<PostInCategoryDTO>()

        whenever(tagService.getAllPostByTagId(eq(1), eq(8), eq(8)))
            .thenReturn(Mono.just(postInCategoryList))

        webTestClient.get()
            .uri("/api/tag/1?page=2&limit=8")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$").isArray
            .jsonPath("$.length()").isEqualTo(0)
    }

    @Test
    fun `태그 조회시 오류가 발생한다`() {
        whenever(tagService.getTagAllContainPost())
            .thenReturn(Flux.error(Exception("데이터베이스 연결 오류")))

        webTestClient.get()
            .uri("/api/tag")
            .exchange()
            .expectStatus().isBadRequest
            .expectBody()
            .jsonPath("$.message").isEqualTo("데이터베이스 연결 오류")
    }

    @Test
    fun `태그 생성시 오류가 발생한다`() {
        whenever(tagService.createTagParseAndMakeAll(any()))
            .thenThrow(RuntimeException("태그 생성 실패"))

        webTestClient.post()
            .uri("/api/tag")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(testTag))
            .exchange()
            .expectStatus().isBadRequest
            .expectBody()
            .jsonPath("$.message").isEqualTo("태그 생성 실패")
    }

    @Test
    fun `존재하지 않는 게시글ID로 태그 삭제시 오류가 발생한다`() {
        whenever(tagService.deleteTagsByPostID(999))
            .thenThrow(RuntimeException("존재하지 않는 게시글입니다"))

        webTestClient.delete()
            .uri("/api/tag/post/999")
            .exchange()
            .expectStatus().isBadRequest
            .expectBody()
            .jsonPath("$.message").isEqualTo("존재하지 않는 게시글입니다")
    }

    @Test
    fun `존재하지 않는 태그ID로 연결 삭제시 오류가 발생한다`() {
        whenever(tagService.deleteTagsByTagID(999))
            .thenThrow(RuntimeException("존재하지 않는 태그입니다"))

        webTestClient.delete()
            .uri("/api/tag/tag/999")
            .exchange()
            .expectStatus().isBadRequest
            .expectBody()
            .jsonPath("$.message").isEqualTo("존재하지 않는 태그입니다")
    }

    @Test
    fun `존재하지 않는 태그로 게시글 조회시 오류가 발생한다`() {
        whenever(tagService.getAllPostByTagId(any(), any(), any()))
            .thenReturn(Mono.error(Exception("존재하지 않는 태그입니다")))

        webTestClient.get()
            .uri("/api/tag/999")
            .exchange()
            .expectStatus().isBadRequest
    }

    @Test
    fun `빈 태그 제목으로 생성시 오류가 발생한다`() {
        val emptyTag = Tag(0, "")

        whenever(tagService.createTagParseAndMakeAll(""))
            .thenThrow(RuntimeException("태그 제목을 입력해주세요"))

        webTestClient.post()
            .uri("/api/tag")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(emptyTag))
            .exchange()
            .expectStatus().isBadRequest
            .expectBody()
            .jsonPath("$.message").isEqualTo("태그 제목을 입력해주세요")
    }

    @Test
    fun `잘못된 페이지 파라미터로 태그별 게시글 조회시 기본값이 적용된다`() {
        val postInCategoryList = emptyList<PostInCategoryDTO>()

        whenever(tagService.getAllPostByTagId(eq(1), eq(0), eq(8)))
            .thenReturn(Mono.just(postInCategoryList))

        webTestClient.get()
            .uri("/api/tag/1?page=invalid&limit=invalid")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$").isArray
            .jsonPath("$.length()").isEqualTo(0)
    }
}