package com.signite.backend.handler

import com.signite.backend.domain.dto.PostFormDTO
import com.signite.backend.domain.dto.UpdateFormDTO
import com.signite.backend.service.CategoryService
import com.signite.backend.service.PostService
import com.signite.backend.service.PostToTagService
import com.signite.backend.service.UserContextService
import com.signite.backend.service.TagService
import com.signite.backend.service.ValidationService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono

@Component
class PostHandler(
    @Autowired val postService: PostService,
    @Autowired val userContextService: UserContextService,
    @Autowired val validationService: ValidationService,
    @Autowired val categoryService: CategoryService,
    @Autowired val tagService: TagService,
    @Autowired val postToTagService: PostToTagService,
) {
    companion object {
        private val logger = LoggerFactory.getLogger(PostHandler::class.java)
    }

    fun save(req: ServerRequest): Mono<ServerResponse> {
        return req.bodyToMono(PostFormDTO::class.java)
            .flatMap { postForm ->
                userContextService.getCurrentUser(req)
                    .flatMap { user ->
                        if (!userContextService.hasRole(req, "SIG_ADMIN")) {
                            return@flatMap Mono.error<Any>(RuntimeException("게시글 작성 권한이 없습니다"))
                        }
                        
                        tagService.createTagParseAndMakeAll(postForm.tags ?: "")
                            .flatMap { tags ->
                                categoryService.createCategoryIfNot(postForm.categoryTitle ?: "기본")
                                    .flatMap { category ->
                                        postService.createPost(user, postForm, category, tags)
                                            .flatMap { post ->
                                                categoryService.resetCategoryCash()
                                                Mono.just(post)
                                            }
                                    }
                            }
                    }
            }
            .flatMap { post -> ServerResponse.ok().bodyValue(post) }
            .onErrorResume { ex ->
                ServerResponse.badRequest().bodyValue(mapOf("message" to (ex.message ?: "오류가 발생했습니다")))
            }
    }

    fun get(req: ServerRequest): Mono<ServerResponse> {
        return postService.getPost(req.pathVariable("postId").toInt())
            .flatMap { post -> 
                if (post != null) {
                    ServerResponse.ok().bodyValue(post)
                } else {
                    ServerResponse.notFound().build()
                }
            }
            .onErrorResume { ex ->
                ServerResponse.badRequest().bodyValue(mapOf("message" to (ex.message ?: "오류가 발생했습니다")))
            }
    }

    fun path(req: ServerRequest): Mono<ServerResponse> {
        return postService.getPostPath()
            .flatMap { postPath -> ServerResponse.ok().bodyValue(postPath) }
            .onErrorResume { ex ->
                ServerResponse.badRequest().bodyValue(mapOf("message" to (ex.message ?: "오류가 발생했습니다")))
            }
    }

    fun update(req: ServerRequest): Mono<ServerResponse> {
        return req.bodyToMono(UpdateFormDTO::class.java)
            .flatMap { postForm ->
                userContextService.getCurrentUser(req)
                    .flatMap { user ->
                        if (!userContextService.hasRole(req, "SIG_ADMIN")) {
                            return@flatMap Mono.error<Any>(RuntimeException("게시글 수정 권한이 없습니다"))
                        }
                        
                        postService.getOnlyPost(req.pathVariable("postId").toInt())
                            .flatMap { post ->
                                postService.updatePost(post, postForm)
                            }
                    }
            }
            .flatMap { updatedPost -> ServerResponse.ok().bodyValue(updatedPost) }
            .onErrorResume { ex ->
                ServerResponse.badRequest().bodyValue(mapOf("message" to (ex.message ?: "오류가 발생했습니다")))
            }
    }

    fun delete(req: ServerRequest): Mono<ServerResponse> {
        return userContextService.getCurrentUser(req)
            .flatMap { user ->
                if (!userContextService.hasRole(req, "SIG_ADMIN")) {
                    return@flatMap Mono.error<Any>(RuntimeException("게시글 삭제 권한이 없습니다"))
                }
                
                postService.deltePost(req.pathVariable("postId").toInt())
                    .flatMap {
                        categoryService.resetCategoryCash()
                        Mono.just(mapOf("message" to "포스트 삭제가 완료되었습니다."))
                    }
            }
            .flatMap { result -> ServerResponse.ok().bodyValue(result) }
            .onErrorResume { ex ->
                ServerResponse.badRequest().bodyValue(mapOf("message" to (ex.message ?: "오류가 발생했습니다")))
            }
    }
}
