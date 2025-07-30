package com.signite.backend.handler

import com.signite.backend.config.TestConfig
import com.signite.backend.router.JwksRouter
import com.signite.backend.service.JwtService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.test.web.reactive.server.WebTestClient

@WebFluxTest
@Import(JwksHandler::class, JwksRouter::class, TestConfig::class)
class JwksHandlerTest {
    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockBean
    private lateinit var jwtService: JwtService

    @Test
    fun `JWKS 조회가 성공적으로 처리된다`() {
        webTestClient.get()
            .uri("/.well-known/jwks.json")
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType("application/json")
            .expectBody()
            .jsonPath("$.keys").isArray
            .jsonPath("$.keys.length()").isEqualTo(1)
            .jsonPath("$.keys[0].kty").isEqualTo("oct")
            .jsonPath("$.keys[0].kid").isEqualTo("signite-key-1")
            .jsonPath("$.keys[0].use").isEqualTo("sig")
            .jsonPath("$.keys[0].alg").isEqualTo("HS256")
    }

    @Test
    fun `OpenID Connect 설정 조회가 성공적으로 처리된다`() {
        webTestClient.get()
            .uri("/.well-known/openid_configuration")
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType("application/json")
            .expectBody()
            .jsonPath("$.issuer").isEqualTo("https://auth.signite.com")
            .jsonPath("$.jwks_uri").isEqualTo("https://auth.signite.com/.well-known/jwks.json")
            .jsonPath("$.authorization_endpoint").isEqualTo("https://auth.signite.com/auth")
            .jsonPath("$.token_endpoint").isEqualTo("https://auth.signite.com/token")
            .jsonPath("$.userinfo_endpoint").isEqualTo("https://auth.signite.com/userinfo")
            .jsonPath("$.response_types_supported").isArray
            .jsonPath("$.response_types_supported.length()").isEqualTo(3)
            .jsonPath("$.subject_types_supported").isArray
            .jsonPath("$.subject_types_supported.length()").isEqualTo(1)
            .jsonPath("$.id_token_signing_alg_values_supported").isArray
            .jsonPath("$.id_token_signing_alg_values_supported[0]").isEqualTo("HS256")
            .jsonPath("$.scopes_supported").isArray
            .jsonPath("$.scopes_supported.length()").isEqualTo(3)
    }

    @Test
    fun `JWKS JSON 응답 형식이 올바르다`() {
        webTestClient.get()
            .uri("/.well-known/jwks.json")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.keys[0]").exists()
            .jsonPath("$.keys[0].kty").exists()
            .jsonPath("$.keys[0].kid").exists()
            .jsonPath("$.keys[0].use").exists()
            .jsonPath("$.keys[0].alg").exists()
    }

    @Test
    fun `OpenID Connect 설정 JSON 응답 형식이 올바르다`() {
        webTestClient.get()
            .uri("/.well-known/openid_configuration")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.issuer").exists()
            .jsonPath("$.jwks_uri").exists()
            .jsonPath("$.authorization_endpoint").exists()
            .jsonPath("$.token_endpoint").exists()
            .jsonPath("$.userinfo_endpoint").exists()
            .jsonPath("$.response_types_supported").exists()
            .jsonPath("$.subject_types_supported").exists()
            .jsonPath("$.id_token_signing_alg_values_supported").exists()
            .jsonPath("$.scopes_supported").exists()
    }

    @Test
    fun `JWKS 응답에 필요한 모든 키 속성이 포함되어 있다`() {
        webTestClient.get()
            .uri("/.well-known/jwks.json")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.keys[0].kty").isNotEmpty
            .jsonPath("$.keys[0].kid").isNotEmpty
            .jsonPath("$.keys[0].use").isNotEmpty
            .jsonPath("$.keys[0].alg").isNotEmpty
    }

    @Test
    fun `OpenID Connect 설정에 지원되는 응답 타입이 올바르다`() {
        webTestClient.get()
            .uri("/.well-known/openid_configuration")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.response_types_supported[0]").isEqualTo("code")
            .jsonPath("$.response_types_supported[1]").isEqualTo("token")
            .jsonPath("$.response_types_supported[2]").isEqualTo("id_token")
    }

    @Test
    fun `OpenID Connect 설정에 지원되는 스코프가 올바르다`() {
        webTestClient.get()
            .uri("/.well-known/openid_configuration")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.scopes_supported[0]").isEqualTo("openid")
            .jsonPath("$.scopes_supported[1]").isEqualTo("profile")
            .jsonPath("$.scopes_supported[2]").isEqualTo("email")
    }
}