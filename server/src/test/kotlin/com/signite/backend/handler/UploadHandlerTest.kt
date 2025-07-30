package com.signite.backend.handler

import com.signite.backend.config.TestConfig
import com.signite.backend.domain.dto.ImageUrlDTO
import com.signite.backend.router.UploadRouter
import com.signite.backend.service.UploadService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import org.springframework.http.MediaType
import org.springframework.http.codec.multipart.FilePart
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.reactive.function.BodyInserters
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.nio.charset.StandardCharsets

@WebFluxTest
@Import(UploadHandler::class, UploadRouter::class, TestConfig::class)
class UploadHandlerTest {
    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockBean
    private lateinit var uploadService: UploadService

    private lateinit var testImageUrlDTO: ImageUrlDTO
    private lateinit var mockFilePart: FilePart

    @BeforeEach
    fun setUp() {
        testImageUrlDTO = ImageUrlDTO(
            id = 1,
            title = "test.jpg",
            postId = 1
        )

        mockFilePart = mock<FilePart> {
            on { filename() } doReturn "test.jpg"
            on { headers() } doReturn org.springframework.http.HttpHeaders().apply {
                contentType = MediaType.IMAGE_JPEG
            }
            on { content() } doReturn Flux.just(
                DefaultDataBufferFactory().wrap("test image content".toByteArray(StandardCharsets.UTF_8))
            )
        }
    }

    @Test
    fun `파일 업로드가 성공적으로 처리된다`() {
        whenever(uploadService.uploadImage(any())).thenReturn(Mono.just(testImageUrlDTO))

        val multiValueMap: MultiValueMap<String, Any> = LinkedMultiValueMap()
        multiValueMap.add("file", mockFilePart)

        webTestClient.post()
            .uri("/api/upload")
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .body(BodyInserters.fromMultipartData(multiValueMap))
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.id").isEqualTo(1)
            .jsonPath("$.title").isEqualTo("test.jpg")
            .jsonPath("$.postId").isEqualTo(1)
    }

    @Test
    fun `파일이 없는 업로드 요청시 오류가 발생한다`() {
        val emptyMultiValueMap: MultiValueMap<String, Any> = LinkedMultiValueMap()

        webTestClient.post()
            .uri("/api/upload")
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .body(BodyInserters.fromMultipartData(emptyMultiValueMap))
            .exchange()
            .expectStatus().isBadRequest
            .expectBody()
            .jsonPath("$.error").isEqualTo("파일이 없습니다.")
    }

    @Test
    fun `파일 업로드시 서비스 오류가 발생한다`() {
        whenever(uploadService.uploadImage(any()))
            .thenReturn(Mono.error(Exception("파일 업로드 실패")))

        val multiValueMap: MultiValueMap<String, Any> = LinkedMultiValueMap()
        multiValueMap.add("file", mockFilePart)

        webTestClient.post()
            .uri("/api/upload")
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .body(BodyInserters.fromMultipartData(multiValueMap))
            .exchange()
            .expectStatus().is5xxServerError
            .expectBody()
            .jsonPath("$.error").isEqualTo("파일 업로드 실패")
    }

    @Test
    fun `파일 삭제가 성공적으로 처리된다`() {
        whenever(uploadService.deleteImage("test.jpg")).thenReturn(Mono.just("삭제 완료"))

        webTestClient.delete()
            .uri("/api/upload/test.jpg")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.result").isEqualTo("삭제 완료")
    }

    @Test
    fun `파일 삭제시 서비스 오류가 발생한다`() {
        whenever(uploadService.deleteImage("test.jpg"))
            .thenReturn(Mono.error(Exception("파일 삭제 실패")))

        webTestClient.delete()
            .uri("/api/upload/test.jpg")
            .exchange()
            .expectStatus().is5xxServerError
            .expectBody()
            .jsonPath("$.error").isEqualTo("파일 삭제 실패")
    }

    @Test
    fun `존재하지 않는 파일 삭제시 오류가 발생한다`() {
        whenever(uploadService.deleteImage("nonexistent.jpg"))
            .thenReturn(Mono.error(Exception("파일을 찾을 수 없습니다")))

        webTestClient.delete()
            .uri("/api/upload/nonexistent.jpg")
            .exchange()
            .expectStatus().is5xxServerError
            .expectBody()
            .jsonPath("$.error").isEqualTo("파일을 찾을 수 없습니다")
    }

    @Test
    fun `잘못된 파일 형식 업로드시 오류가 발생한다`() {
        whenever(uploadService.uploadImage(any()))
            .thenReturn(Mono.error(Exception("지원하지 않는 파일 형식입니다")))

        val invalidFilePart = mock<FilePart> {
            on { filename() } doReturn "test.txt"
            on { headers() } doReturn org.springframework.http.HttpHeaders().apply {
                contentType = MediaType.TEXT_PLAIN
            }
            on { content() } doReturn Flux.just(
                DefaultDataBufferFactory().wrap("test content".toByteArray(StandardCharsets.UTF_8))
            )
        }

        val multiValueMap: MultiValueMap<String, Any> = LinkedMultiValueMap()
        multiValueMap.add("file", invalidFilePart)

        webTestClient.post()
            .uri("/api/upload")
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .body(BodyInserters.fromMultipartData(multiValueMap))
            .exchange()
            .expectStatus().is5xxServerError
            .expectBody()
            .jsonPath("$.error").isEqualTo("지원하지 않는 파일 형식입니다")
    }

    @Test
    fun `파일 크기가 초과된 업로드시 오류가 발생한다`() {
        whenever(uploadService.uploadImage(any()))
            .thenReturn(Mono.error(Exception("파일 크기가 너무 큽니다")))

        val largeFilePart = mock<FilePart> {
            on { filename() } doReturn "large.jpg"
            on { headers() } doReturn org.springframework.http.HttpHeaders().apply {
                contentType = MediaType.IMAGE_JPEG
                contentLength = 10_000_000L // 10MB
            }
            on { content() } doReturn Flux.just(
                DefaultDataBufferFactory().wrap("large file content".toByteArray(StandardCharsets.UTF_8))
            )
        }

        val multiValueMap: MultiValueMap<String, Any> = LinkedMultiValueMap()
        multiValueMap.add("file", largeFilePart)

        webTestClient.post()
            .uri("/api/upload")
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .body(BodyInserters.fromMultipartData(multiValueMap))
            .exchange()
            .expectStatus().is5xxServerError
            .expectBody()
            .jsonPath("$.error").isEqualTo("파일 크기가 너무 큽니다")
    }

    @Test
    fun `특수문자가 포함된 파일명 삭제가 처리된다`() {
        val specialFileName = "test-file_123.jpg"
        whenever(uploadService.deleteImage(specialFileName)).thenReturn(Mono.just("삭제 완료"))

        webTestClient.delete()
            .uri("/api/upload/$specialFileName")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.result").isEqualTo("삭제 완료")
    }
}