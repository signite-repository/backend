package com.signite.backend.handler

import com.signite.backend.config.TestConfig
import com.signite.backend.domain.dto.CommentFormDTO
import com.signite.backend.domain.dto.CommentDTO
import com.signite.backend.domain.dto.UserDTO
import com.signite.backend.domain.entity.Comment
import com.signite.backend.domain.entity.User
import com.signite.backend.router.CommentRouter
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
@Import(CommentHandler::class, CommentRouter::class, TestConfig::class)
class CommentHandlerTest {
    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockBean
    private lateinit var postService: PostService

    @MockBean
    private lateinit var commentService: CommentService

    @MockBean
    private lateinit var userContextService: UserContextService

    @MockBean
    private lateinit var validationService: ValidationService

    private lateinit var testUser: User
    private lateinit var testComment: Comment
    private lateinit var testCommentForm: CommentFormDTO
    private lateinit var testCommentDTOList: List<CommentDTO>

    @BeforeEach
    fun setUp() {
        testUser = User(
            id = 1,
            username = "testuser",
            email = "test@example.com",
            hashedPassword = "hashedpassword",
            imageUrl = "profile.jpg",
            githubUrl = "",
            summary = "테스트 사용자"
        )

        testComment = Comment(
            id = 1,
            content = "테스트 댓글",
            userId = 1,
            postId = 1,
            createdAt = LocalDateTime.now()
        )

        testCommentForm = CommentFormDTO().apply {
            content = "테스트 댓글 내용"
        }

        testCommentDTOList = listOf(
            CommentDTO(
                id = 1,
                content = "테스트 댓글",
                createdAt = LocalDateTime.now(),
                user = UserDTO(
                    id = 1,
                    username = "testuser",
                    email = "test@example.com",
                    hashedPassword = "",
                    imageUrl = "profile.jpg",
                    githubUrl = "",
                    summary = "테스트 사용자"
                )
            )
        )
    }

    @Test
    fun `게시글별 댓글 조회가 성공적으로 처리된다`() {
        whenever(commentService.getCommentByPostId(1)).thenReturn(Mono.just(testCommentDTOList))

        webTestClient.get()
            .uri("/api/comment/1")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$").isArray
            .jsonPath("$.length()").isEqualTo(1)
            .jsonPath("$[0].content").isEqualTo("테스트 댓글")
            .jsonPath("$[0].user.username").isEqualTo("testuser")
    }

    @Test
    fun `댓글 생성이 성공적으로 처리된다`() {
        whenever(userContextService.getCurrentUser(any())).thenReturn(Mono.just(testUser))
        whenever(commentService.createComment(any(), any(), any())).thenReturn(Mono.just(testComment))
        whenever(commentService.getCommentByPostId(1)).thenReturn(Mono.just(testCommentDTOList))

        webTestClient.post()
            .uri("/api/comment/1")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer test-token")
            .body(BodyInserters.fromValue(testCommentForm))
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$").isArray
            .jsonPath("$.length()").isEqualTo(1)
    }

    @Test
    fun `댓글 삭제가 성공적으로 처리된다`() {
        whenever(userContextService.getCurrentUser(any())).thenReturn(Mono.just(testUser))
        whenever(commentService.getComment(1)).thenReturn(Mono.just(testComment))
        whenever(commentService.deleteComment(1)).thenReturn(Mono.just(true))
        whenever(commentService.getCommentByPostId(1)).thenReturn(Mono.just(testCommentDTOList))

        webTestClient.delete()
            .uri("/api/comment/1")
            .header("Authorization", "Bearer test-token")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$").isArray
    }

    @Test
    fun `존재하지 않는 게시글의 댓글 조회시 오류가 발생한다`() {
        whenever(commentService.getCommentByPostId(999))
            .thenReturn(Mono.error(Exception("존재하지 않는 게시글입니다")))

        webTestClient.get()
            .uri("/api/comment/999")
            .exchange()
            .expectStatus().isBadRequest
            .expectBody()
            .jsonPath("$.message").isEqualTo("존재하지 않는 게시글입니다")
    }

    @Test
    fun `인증되지 않은 사용자의 댓글 생성시 오류가 발생한다`() {
        whenever(userContextService.getCurrentUser(any()))
            .thenReturn(Mono.error(Exception("인증되지 않은 사용자입니다")))

        webTestClient.post()
            .uri("/api/comment/1")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(testCommentForm))
            .exchange()
            .expectStatus().isBadRequest
            .expectBody()
            .jsonPath("$.message").isEqualTo("인증되지 않은 사용자입니다")
    }

    @Test
    fun `다른 사용자의 댓글 삭제시 권한 오류가 발생한다`() {
        val otherUserComment = Comment(
            id = 1,
            content = "테스트 댓글",
            userId = 2,
            postId = 1,
            createdAt = LocalDateTime.now()
        )

        whenever(userContextService.getCurrentUser(any())).thenReturn(Mono.just(testUser))
        whenever(commentService.getComment(1)).thenReturn(Mono.just(otherUserComment))

        webTestClient.delete()
            .uri("/api/comment/1")
            .header("Authorization", "Bearer test-token")
            .exchange()
            .expectStatus().isBadRequest
            .expectBody()
            .jsonPath("$.message").isEqualTo("권한이 없습니다")
    }

    @Test
    fun `존재하지 않는 댓글 삭제시 오류가 발생한다`() {
        whenever(userContextService.getCurrentUser(any())).thenReturn(Mono.just(testUser))
        whenever(commentService.getComment(999))
            .thenReturn(Mono.error(Exception("존재하지 않는 댓글입니다")))

        webTestClient.delete()
            .uri("/api/comment/999")
            .header("Authorization", "Bearer test-token")
            .exchange()
            .expectStatus().isBadRequest
            .expectBody()
            .jsonPath("$.message").isEqualTo("존재하지 않는 댓글입니다")
    }

    @Test
    fun `빈 내용으로 댓글 생성시 적절히 처리된다`() {
        val emptyCommentForm = CommentFormDTO().apply {
            content = ""
        }

        whenever(userContextService.getCurrentUser(any())).thenReturn(Mono.just(testUser))
        whenever(commentService.createComment(any(), any(), any()))
            .thenReturn(Mono.error(Exception("댓글 내용을 입력해주세요")))

        webTestClient.post()
            .uri("/api/comment/1")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer test-token")
            .body(BodyInserters.fromValue(emptyCommentForm))
            .exchange()
            .expectStatus().isBadRequest
            .expectBody()
            .jsonPath("$.message").isEqualTo("댓글 내용을 입력해주세요")
    }

    @Test
    fun `userId가 null인 댓글 삭제시 권한 오류가 발생한다`() {
        val nullUserComment = Comment(
            id = 1,
            content = "테스트 댓글",
            userId = null,
            postId = 1,
            createdAt = LocalDateTime.now()
        )

        whenever(userContextService.getCurrentUser(any())).thenReturn(Mono.just(testUser))
        whenever(commentService.getComment(1)).thenReturn(Mono.just(nullUserComment))

        webTestClient.delete()
            .uri("/api/comment/1")
            .header("Authorization", "Bearer test-token")
            .exchange()
            .expectStatus().isBadRequest
            .expectBody()
            .jsonPath("$.message").isEqualTo("권한이 없습니다")
    }
}