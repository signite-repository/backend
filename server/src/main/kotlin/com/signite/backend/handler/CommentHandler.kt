package com.signite.backend.handler

import com.signite.backend.domain.dto.CommentFormDTO
import com.signite.backend.service.CommentService
import com.signite.backend.service.PostService
import com.signite.backend.service.UserContextService
import com.signite.backend.service.ValidationService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono

@Component
class CommentHandler(
    @Autowired private val postService: PostService,
    @Autowired private val commentService: CommentService,
    @Autowired private val userContextService: UserContextService,
    @Autowired private val validationService: ValidationService,
) {
    companion object {
        private val logger = LoggerFactory.getLogger(CommentHandler::class.java)
    }

    fun getByPostId(req: ServerRequest): Mono<ServerResponse> {
        return commentService.getCommentByPostId(req.pathVariable("postId").toInt())
            .flatMap { comments -> ServerResponse.ok().bodyValue(comments) }
            .onErrorResume { ex ->
                ServerResponse.badRequest().bodyValue(mapOf("message" to (ex.message ?: "오류가 발생했습니다")))
            }
    }

    fun create(req: ServerRequest): Mono<ServerResponse> {
        return req.bodyToMono(CommentFormDTO::class.java)
            .flatMap { commentForm ->
                userContextService.getCurrentUser(req)
                    .flatMap { user ->
                        val postId = req.pathVariable("postId").toInt()
                        commentService.createComment(commentForm, user.id, postId)
                            .flatMap { 
                                commentService.getCommentByPostId(postId)
                            }
                    }
            }
            .flatMap { comments -> ServerResponse.ok().bodyValue(comments) }
            .onErrorResume { ex ->
                ServerResponse.badRequest().bodyValue(mapOf("message" to (ex.message ?: "오류가 발생했습니다")))
            }
    }

    fun delete(req: ServerRequest): Mono<ServerResponse> {
        return userContextService.getCurrentUser(req)
            .flatMap { user ->
                val commentId = req.pathVariable("commentId").toInt()
                commentService.getComment(commentId)
                    .flatMap { comment ->
                        if (comment.userId != null && comment.userId == user.id) {
                            commentService.deleteComment(comment.id!!)
                                .flatMap {
                                    commentService.getCommentByPostId(comment.postId!!)
                                }
                        } else {
                            Mono.error<Any>(Exception("권한이 없습니다"))
                        }
                    }
            }
            .flatMap { comments -> ServerResponse.ok().bodyValue(comments) }
            .onErrorResume { ex ->
                ServerResponse.badRequest().bodyValue(mapOf("message" to (ex.message ?: "오류가 발생했습니다")))
            }
    }
}
