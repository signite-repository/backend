package com.signite.postservice.web

import com.signite.postservice.service.CommentService
import com.signite.postservice.web.dto.*
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import jakarta.validation.Valid
import java.net.URI

@RestController
@RequestMapping("/api/v1/posts/{postId}/comments")
@CrossOrigin(origins = ["*"])
class CommentController(private val commentService: CommentService) {
    
    @PostMapping
    fun createComment(
        @PathVariable postId: Long,
        @Valid @RequestBody request: CommentRequest,
        @RequestHeader("X-User-Id") authorId: String
    ): Mono<ResponseEntity<CommentResponse>> {
        return commentService.createComment(postId, request, authorId)
            .map { comment ->
                ResponseEntity
                    .created(URI.create("/api/v1/posts/$postId/comments/${comment.id}"))
                    .body(CommentResponse.fromEntity(comment))
            }
    }
    
    @GetMapping
    fun getComments(@PathVariable postId: Long): Flux<CommentResponse> {
        return commentService.getCommentsByPostId(postId)
    }
    
    @GetMapping("/count")
    fun getCommentCount(@PathVariable postId: Long): Mono<Map<String, Long>> {
        return commentService.getCommentCount(postId)
            .map { count -> mapOf("count" to count) }
    }
    
    @PutMapping("/{commentId}")
    fun updateComment(
        @PathVariable postId: Long,
        @PathVariable commentId: Long,
        @Valid @RequestBody request: CommentUpdateRequest,
        @RequestHeader("X-User-Id") userId: String
    ): Mono<ResponseEntity<CommentResponse>> {
        return commentService.updateComment(commentId, request, userId)
            .map { comment -> ResponseEntity.ok(CommentResponse.fromEntity(comment)) }
            .onErrorResume(RuntimeException::class.java) { error ->
                when (error.message) {
                    "Comment not found" -> Mono.just(ResponseEntity.notFound().build())
                    "Unauthorized to update comment" -> Mono.just(ResponseEntity.status(403).build())
                    "Cannot update deleted comment" -> Mono.just(ResponseEntity.status(400).build())
                    else -> Mono.error(error)
                }
            }
    }
    
    @DeleteMapping("/{commentId}")
    fun deleteComment(
        @PathVariable postId: Long,
        @PathVariable commentId: Long,
        @RequestHeader("X-User-Id") userId: String
    ): Mono<ResponseEntity<Void>> {
        return commentService.deleteComment(commentId, userId)
            .then(Mono.just(ResponseEntity.noContent().build<Void>()))
            .onErrorResume(RuntimeException::class.java) { error ->
                when (error.message) {
                    "Comment not found" -> Mono.just(ResponseEntity.notFound().build())
                    "Unauthorized to delete comment" -> Mono.just(ResponseEntity.status(403).build())
                    else -> Mono.error(error)
                }
            }
    }
}