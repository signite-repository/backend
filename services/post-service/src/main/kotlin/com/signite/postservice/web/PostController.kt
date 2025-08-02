package com.signite.postservice.web

import com.signite.postservice.service.PostService
import com.signite.postservice.web.dto.PostRequest
import com.signite.postservice.web.dto.PostResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono
import java.net.URI

@RestController
@RequestMapping("/api/v1/posts")
class PostController(private val postService: PostService) {

    @PostMapping
    fun createPost(
        @RequestBody request: PostRequest,
        // 실제 운영 환경에서는 JWT 토큰에서 사용자 정보를 추출해야 합니다.
        // 여기서는 헤더를 통해 임시로 받습니다.
        @RequestHeader("X-User-Id") authorId: String,
        @RequestHeader("X-User-Roles") userRoles: List<String>
    ): Mono<ResponseEntity<PostResponse>> {
        return postService.createPost(request, authorId, userRoles)
            .map { createdPost ->
                ResponseEntity
                    .created(URI.create("/api/v1/posts/${createdPost.id}"))
                    .body(PostResponse.fromEntity(createdPost))
            }
    }

    @GetMapping("/{id}")
    fun getPost(@PathVariable id: Long): Mono<ResponseEntity<PostResponse>> {
        return postService.getPostById(id)
            .map { post -> ResponseEntity.ok(PostResponse.fromEntity(post)) }
            .defaultIfEmpty(ResponseEntity.notFound().build())
    }
}
