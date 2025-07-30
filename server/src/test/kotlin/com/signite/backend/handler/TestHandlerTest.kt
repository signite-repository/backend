package com.signite.backend.handler

import com.signite.backend.config.TestConfig
import com.signite.backend.domain.entity.User
import com.signite.backend.router.TestRouter
import com.signite.backend.service.UserContextService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Mono

@WebFluxTest
@Import(TestHandler::class, TestRouter::class, TestConfig::class)
class TestHandlerTest {
    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockBean
    private lateinit var userContextService: UserContextService

    private lateinit var testUser: User

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
    }

    @Test
    fun `헬스체크가 성공적으로 처리된다`() {
        webTestClient.get()
            .uri("/api/test/health")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.status").isEqualTo("OK")
            .jsonPath("$.service").isEqualTo("signite-backend")
            .jsonPath("$.timestamp").exists()
    }

    @Test
    fun `인증 테스트가 성공적으로 처리된다`() {
        whenever(userContextService.getCurrentUser(any())).thenReturn(Mono.just(testUser))
        whenever(userContextService.getCurrentUserRole(any())).thenReturn("ACTIVE_MEMBER")
        whenever(userContextService.getCurrentOrganizationId(any())).thenReturn(1)

        webTestClient.get()
            .uri("/api/test/auth")
            .header("Authorization", "Bearer test-token")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.authenticated").isEqualTo(true)
            .jsonPath("$.user.id").isEqualTo(1)
            .jsonPath("$.user.username").isEqualTo("testuser")
            .jsonPath("$.user.email").isEqualTo("test@example.com")
            .jsonPath("$.role").isEqualTo("ACTIVE_MEMBER")
            .jsonPath("$.organizationId").isEqualTo(1)
            .jsonPath("$.timestamp").exists()
    }

    @Test
    fun `인증되지 않은 사용자의 인증 테스트가 처리된다`() {
        whenever(userContextService.getCurrentUser(any()))
            .thenReturn(Mono.error(Exception("인증되지 않은 사용자입니다")))

        webTestClient.get()
            .uri("/api/test/auth")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.authenticated").isEqualTo(false)
            .jsonPath("$.error").isEqualTo("인증되지 않은 사용자입니다")
            .jsonPath("$.timestamp").exists()
    }

    @Test
    fun `권한 테스트가 성공적으로 처리된다`() {
        whenever(userContextService.hasRole(any(), eq("ADMIN"))).thenReturn(true)
        whenever(userContextService.getCurrentUserRole(any())).thenReturn("ADMIN")

        webTestClient.get()
            .uri("/api/test/role?role=ADMIN")
            .header("Authorization", "Bearer test-token")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.hasRole").isEqualTo(true)
            .jsonPath("$.requiredRole").isEqualTo("ADMIN")
            .jsonPath("$.userRole").isEqualTo("ADMIN")
            .jsonPath("$.timestamp").exists()
    }

    @Test
    fun `권한이 없는 사용자의 권한 테스트가 처리된다`() {
        whenever(userContextService.hasRole(any(), eq("ADMIN"))).thenReturn(false)
        whenever(userContextService.getCurrentUserRole(any())).thenReturn("GUEST_MEMBER")

        webTestClient.get()
            .uri("/api/test/role?role=ADMIN")
            .header("Authorization", "Bearer test-token")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.hasRole").isEqualTo(false)
            .jsonPath("$.requiredRole").isEqualTo("ADMIN")
            .jsonPath("$.userRole").isEqualTo("GUEST_MEMBER")
            .jsonPath("$.timestamp").exists()
    }

    @Test
    fun `기본 권한으로 권한 테스트가 처리된다`() {
        whenever(userContextService.hasRole(any(), eq("ACTIVE_MEMBER"))).thenReturn(true)
        whenever(userContextService.getCurrentUserRole(any())).thenReturn("ACTIVE_MEMBER")

        webTestClient.get()
            .uri("/api/test/role")
            .header("Authorization", "Bearer test-token")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.hasRole").isEqualTo(true)
            .jsonPath("$.requiredRole").isEqualTo("ACTIVE_MEMBER")
            .jsonPath("$.userRole").isEqualTo("ACTIVE_MEMBER")
            .jsonPath("$.timestamp").exists()
    }

    @Test
    fun `여러 권한에 대한 테스트가 처리된다`() {
        whenever(userContextService.hasRole(any(), eq("MODERATOR"))).thenReturn(false)
        whenever(userContextService.getCurrentUserRole(any())).thenReturn("ACTIVE_MEMBER")

        webTestClient.get()
            .uri("/api/test/role?role=MODERATOR")
            .header("Authorization", "Bearer test-token")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.hasRole").isEqualTo(false)
            .jsonPath("$.requiredRole").isEqualTo("MODERATOR")
            .jsonPath("$.userRole").isEqualTo("ACTIVE_MEMBER")
    }

    @Test
    fun `헬스체크 응답 형식이 올바르다`() {
        webTestClient.get()
            .uri("/api/test/health")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.status").exists()
            .jsonPath("$.service").exists()
            .jsonPath("$.timestamp").exists()
            .jsonPath("$.timestamp").isNumber
    }

    @Test
    fun `인증 테스트 성공시 응답 형식이 올바르다`() {
        whenever(userContextService.getCurrentUser(any())).thenReturn(Mono.just(testUser))
        whenever(userContextService.getCurrentUserRole(any())).thenReturn("ACTIVE_MEMBER")
        whenever(userContextService.getCurrentOrganizationId(any())).thenReturn(1)

        webTestClient.get()
            .uri("/api/test/auth")
            .header("Authorization", "Bearer test-token")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.authenticated").exists()
            .jsonPath("$.user").exists()
            .jsonPath("$.user.id").exists()
            .jsonPath("$.user.username").exists()
            .jsonPath("$.user.email").exists()
            .jsonPath("$.role").exists()
            .jsonPath("$.organizationId").exists()
            .jsonPath("$.timestamp").exists()
            .jsonPath("$.timestamp").isNumber
    }

    @Test
    fun `인증 테스트 실패시 응답 형식이 올바르다`() {
        whenever(userContextService.getCurrentUser(any()))
            .thenReturn(Mono.error(Exception("토큰이 유효하지 않습니다")))

        webTestClient.get()
            .uri("/api/test/auth")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.authenticated").isEqualTo(false)
            .jsonPath("$.error").exists()
            .jsonPath("$.timestamp").exists()
            .jsonPath("$.timestamp").isNumber
    }

    @Test
    fun `권한 테스트 응답 형식이 올바르다`() {
        whenever(userContextService.hasRole(any(), any())).thenReturn(true)
        whenever(userContextService.getCurrentUserRole(any())).thenReturn("ACTIVE_MEMBER")

        webTestClient.get()
            .uri("/api/test/role?role=ACTIVE_MEMBER")
            .header("Authorization", "Bearer test-token")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.hasRole").exists()
            .jsonPath("$.requiredRole").exists()
            .jsonPath("$.userRole").exists()
            .jsonPath("$.timestamp").exists()
            .jsonPath("$.timestamp").isNumber
    }
}