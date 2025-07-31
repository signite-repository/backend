package com.signite.postservice.service

import com.signite.postservice.domain.Post
import com.signite.postservice.domain.PostDocument
import com.signite.postservice.repository.PostRepository
import com.signite.postservice.repository.PostSearchRepository
import com.signite.postservice.web.dto.PostRequest
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty

@Service
class PostService(
    private val postRepository: PostRepository,
    private val postSearchRepository: PostSearchRepository,
    private val categoryClientService: CategoryClientService
) {

    fun createPost(request: PostRequest, authorId: String, userRoles: List<String>): Mono<Post> {
        // 1. 권한 확인
        return categoryClientService.canCreatePost(userRoles, request.categoryId)
            .flatMap { hasPermission ->
                if (!hasPermission) {
                    Mono.error(SecurityException("User has no permission to create a post in category ${request.categoryId}"))
                } else {
                    // 2. MongoDB에 Post 저장
                    val post = Post(
                        title = request.title,
                        content = request.content,
                        authorId = authorId,
                        categoryId = request.categoryId,
                        tags = request.tags
                    )
                    postRepository.save(post)
                }
            }
            .flatMap { savedPost ->
                // 3. Elasticsearch에 PostDocument 저장 (데이터 동기화)
                val postDocument = PostDocument(
                    id = savedPost.id!!,
                    title = savedPost.title,
                    content = savedPost.content,
                    tags = savedPost.tags
                )
                postSearchRepository.save(postDocument)
                    .thenReturn(savedPost) // Elasticsearch 저장 후 원래의 Post 객체를 반환
            }
    }

    fun getPostById(id: String): Mono<Post> {
        return postRepository.findById(id)
    }

    // Update 및 Delete 로직은 향후 요구사항에 따라 추가 예정
}
