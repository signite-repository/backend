package com.signite.postservice.web

import com.signite.postservice.service.PostService
import com.signite.postservice.web.dto.*
import org.springframework.data.domain.PageRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import jakarta.validation.Valid
import java.net.URI

@RestController
@RequestMapping("/api/v1/posts")
@CrossOrigin(origins = ["*"])
class PostController(private val postService: PostService) {

    @PostMapping
    fun createPost(
        @Valid @RequestBody request: PostRequest,
        @RequestHeader("X-User-Id") authorId: String
    ): Mono<ResponseEntity<PostResponse>> {
        return postService.createPost(request, authorId)
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

    @GetMapping
    fun getPosts(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) categoryId: String?,
        @RequestParam(required = false) authorId: String?
    ): Mono<PageResponse<PostResponse>> {
        val pageable = PageRequest.of(page, size)
        return postService.getPostsCount(categoryId, authorId)
            .flatMap { totalCount ->
                postService.getPosts(pageable, categoryId, authorId)
                    .map { PostResponse.fromEntity(it) }
                    .collectList()
                    .map { posts ->
                        PageResponse.of(posts, page, size, totalCount)
                    }
            }
    }

    @PutMapping("/{id}")
    fun updatePost(
        @PathVariable id: Long,
        @Valid @RequestBody request: PostUpdateRequest,
        @RequestHeader("X-User-Id") userId: String
    ): Mono<ResponseEntity<PostResponse>> {
        return postService.updatePost(id, request, userId)
            .map { post -> ResponseEntity.ok(PostResponse.fromEntity(post)) }
            .onErrorResume(RuntimeException::class.java) { error ->
                when (error.message) {
                    "Post not found" -> Mono.just(ResponseEntity.notFound().build())
                    "Unauthorized to update post" -> Mono.just(ResponseEntity.status(403).build())
                    else -> Mono.error(error)
                }
            }
    }

    @DeleteMapping("/{id}")
    fun deletePost(
        @PathVariable id: Long,
        @RequestHeader("X-User-Id") userId: String
    ): Mono<ResponseEntity<Void>> {
        return postService.deletePost(id, userId)
            .then(Mono.just(ResponseEntity.noContent().build<Void>()))
            .onErrorResume(RuntimeException::class.java) { error ->
                when (error.message) {
                    "Post not found" -> Mono.just(ResponseEntity.notFound().build())
                    "Unauthorized to delete post" -> Mono.just(ResponseEntity.status(403).build())
                    else -> Mono.error(error)
                }
            }
    }

    @GetMapping("/search")
    fun searchPosts(
        @RequestParam query: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): Flux<PostResponse> {
        return postService.searchPosts(query, page, size)
            .map { PostResponse.fromEntity(it) }
    }
}
