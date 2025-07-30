package com.signite.backend.handler

import com.signite.backend.config.TestConfig
import com.signite.backend.domain.dto.UserFormDTO
import com.signite.backend.domain.entity.User
import com.signite.backend.router.AuthRouter
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

@WebFluxTest
@Import(AuthHandler::class, AuthRouter::class, TestConfig::class)
class AuthHandlerTest {
    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockBean
    private lateinit var passwordService: PasswordService

    @MockBean
    private lateinit var validationService: ValidationService

    @MockBean
    private lateinit var authService: AuthService

    @MockBean
    private lateinit var userContextService: UserContextService

    @MockBean
    private lateinit var jwtService: JwtService

    @MockBean
    private lateinit var userRoleService: UserRoleService

    private lateinit var testUserForm: UserFormDTO
    private lateinit var testUser: User

    @BeforeEach
    fun setUp() {
        testUserForm = UserFormDTO().apply {
            username = "testuser"
            password = "testpassword"
        }

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
    fun `회원가입이 성공적으로 처리된다`() {
        val token = "test-jwt-token"

        whenever(validationService.checkValidForm<UserFormDTO>(any(), any())).thenReturn(Mono.just(testUserForm))
        whenever(validationService.checkValidUsername(any())).thenReturn(Mono.just(testUserForm))
        whenever(passwordService.changeHashedPassword(any())).thenReturn(Mono.just(testUserForm))
        whenever(authService.createUser(any())).thenReturn(Mono.just(testUser))
        whenever(userRoleService.createUserRole(any(), any())).thenReturn(Mono.empty())
        whenever(jwtService.generateToken(any(), any(), any())).thenReturn(Mono.just(token))

        webTestClient.post()
            .uri("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(testUserForm))
            .exchange()
            .expectStatus().isOk
            .expectHeader().exists("Authorization")
            .expectBody()
            .jsonPath("$.message").isEqualTo("회원가입이 완료되었습니다")
            .jsonPath("$.userId").isEqualTo(1)
            .jsonPath("$.username").isEqualTo("testuser")
    }

    @Test
    fun `로그인이 성공적으로 처리된다`() {
        val token = "test-jwt-token"

        whenever(validationService.checkValidForm<UserFormDTO>(any(), any())).thenReturn(Mono.just(testUserForm))
        whenever(validationService.checkNotValidUsername(any())).thenReturn(Mono.just(testUserForm))
        whenever(authService.getUserByUsername(any())).thenReturn(Mono.just(testUser))
        whenever(passwordService.checkPassword(any(), any())).thenReturn(Mono.just(testUser))
        whenever(userRoleService.getUserRole(any())).thenReturn(Mono.just("ACTIVE_MEMBER"))
        whenever(jwtService.generateToken(any(), any(), any())).thenReturn(Mono.just(token))

        webTestClient.post()
            .uri("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(testUserForm))
            .exchange()
            .expectStatus().isOk
            .expectHeader().exists("Authorization")
            .expectBody()
            .jsonPath("$.message").isEqualTo("로그인이 완료되었습니다")
            .jsonPath("$.userId").isEqualTo(1)
            .jsonPath("$.username").isEqualTo("testuser")
    }

    @Test
    fun `프로필 조회가 성공적으로 처리된다`() {
        whenever(userContextService.getCurrentUser(any())).thenReturn(Mono.just(testUser))

        webTestClient.get()
            .uri("/api/auth/profile")
            .header("Authorization", "Bearer test-token")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.id").isEqualTo(1)
            .jsonPath("$.username").isEqualTo("testuser")
            .jsonPath("$.email").isEqualTo("test@example.com")
    }

    @Test
    fun `잘못된 회원가입 데이터로 오류가 발생한다`() {
        val invalidUserForm = UserFormDTO()

        whenever(validationService.checkValidForm<UserFormDTO>(any(), any()))
            .thenReturn(Mono.error(Exception("필수 필드가 누락되었습니다")))

        webTestClient.post()
            .uri("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(invalidUserForm))
            .exchange()
            .expectStatus().isBadRequest
            .expectBody()
            .jsonPath("$.error").isEqualTo("필수 필드가 누락되었습니다")
    }

    @Test
    fun `잘못된 로그인 정보로 오류가 발생한다`() {
        whenever(validationService.checkValidForm<UserFormDTO>(any(), any())).thenReturn(Mono.just(testUserForm))
        whenever(validationService.checkNotValidUsername(any())).thenReturn(Mono.just(testUserForm))
        whenever(authService.getUserByUsername(any())).thenReturn(Mono.just(testUser))
        whenever(passwordService.checkPassword(any(), any()))
            .thenReturn(Mono.error(Exception("비밀번호가 일치하지 않습니다")))

        webTestClient.post()
            .uri("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(testUserForm))
            .exchange()
            .expectStatus().isBadRequest
            .expectBody()
            .jsonPath("$.error").isEqualTo("비밀번호가 일치하지 않습니다")
    }

    @Test
    fun `인증되지 않은 사용자 프로필 조회시 오류가 발생한다`() {
        whenever(userContextService.getCurrentUser(any()))
            .thenReturn(Mono.error(Exception("인증되지 않은 사용자입니다")))

        webTestClient.get()
            .uri("/api/auth/profile")
            .exchange()
            .expectStatus().isBadRequest
            .expectBody()
            .jsonPath("$.error").isEqualTo("인증되지 않은 사용자입니다")
    }
}