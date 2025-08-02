package com.signite.postservice.service

import com.signite.postservice.config.CategoryConfig
import com.signite.postservice.domain.Post
import com.signite.postservice.domain.PostDocument
import com.signite.postservice.repository.PostRepository
import com.signite.postservice.repository.PostSearchRepository
import com.signite.postservice.web.dto.PostRequest
import com.signite.postservice.web.dto.PostUpdateRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDateTime

@Service
class PostService(
    private val postRepository: PostRepository,
    private val postSearchRepository: PostSearchRepository
) {

    fun createPost(request: PostRequest, authorId: String): Mono<Post> {
        // 카테고리 유효성 검사
        if (!CategoryConfig.isValidCategory(request.categoryId)) {
            return Mono.error(RuntimeException("Invalid category"))
        }
        
        val post = Post(
            title = request.title,
            content = request.content,
            authorId = authorId,
            categoryId = request.categoryId,
            tags = request.tags.joinToString(",", "[", "]") { "\"$it\"" }
        )
        
        return postRepository.save(post)
            .flatMap { savedPost ->
                val tagList = parseTagsFromJson(savedPost.tags)
                
                val postDocument = PostDocument(
                    id = savedPost.id!!.toString(),
                    title = savedPost.title,
                    content = savedPost.content,
                    tags = tagList
                )
                postSearchRepository.save(postDocument)
                    .thenReturn(savedPost)
            }
    }

    fun getPostById(id: Long): Mono<Post> {
        return postRepository.findById(id)
            .switchIfEmpty(Mono.error(RuntimeException("Post not found")))
    }

    fun getPosts(pageable: Pageable, categoryId: String?, authorId: String?): Flux<Post> {
        return when {
            categoryId != null && authorId != null -> 
                postRepository.findByCategoryIdAndAuthorId(categoryId, authorId, pageable)
            categoryId != null -> 
                postRepository.findByCategoryId(categoryId, pageable)
            authorId != null -> 
                postRepository.findByAuthorId(authorId, pageable)
            else -> 
                postRepository.findAllBy(pageable)
        }
    }

    fun updatePost(id: Long, request: PostUpdateRequest, userId: String): Mono<Post> {
        return postRepository.findById(id)
            .switchIfEmpty(Mono.error(RuntimeException("Post not found")))
            .flatMap { post ->
                if (post.authorId != userId) {
                    Mono.error<Post>(RuntimeException("Unauthorized to update post"))
                } else {
                    request.title?.let { post.title = it }
                    request.content?.let { post.content = it }
                    request.tags?.let { 
                        post.tags = it.joinToString(",", "[", "]") { tag -> "\"$tag\"" }
                    }
                    post.updatedAt = LocalDateTime.now()
                    postRepository.save(post)
                }
            }
            .flatMap { updatedPost ->
                val tagList = parseTagsFromJson(updatedPost.tags)
                
                val postDocument = PostDocument(
                    id = updatedPost.id!!.toString(),
                    title = updatedPost.title,
                    content = updatedPost.content,
                    tags = tagList
                )
                postSearchRepository.save(postDocument)
                    .thenReturn(updatedPost)
            }
    }

    fun deletePost(id: Long, userId: String): Mono<Void> {
        return postRepository.findById(id)
            .switchIfEmpty(Mono.error(RuntimeException("Post not found")))
            .flatMap { post ->
                if (post.authorId != userId) {
                    Mono.error<Void>(RuntimeException("Unauthorized to delete post"))
                } else {
                    postRepository.delete(post)
                        .then(postSearchRepository.deleteById(post.id!!.toString()))
                }
            }
    }

    fun searchPosts(query: String, page: Int, size: Int): Flux<Post> {
        return postSearchRepository.search(query, page, size)
            .flatMap { doc ->
                postRepository.findById(doc.id.toLong())
            }
    }

    fun getPostsCount(categoryId: String?, authorId: String?): Mono<Long> {
        return when {
            categoryId != null && authorId != null -> 
                postRepository.countByCategoryIdAndAuthorId(categoryId, authorId)
            categoryId != null -> 
                postRepository.countByCategoryId(categoryId)
            authorId != null -> 
                postRepository.countByAuthorId(authorId)
            else -> 
                postRepository.count()
        }
    }

    private fun parseTagsFromJson(tagsJson: String): List<String> {
        return try {
            tagsJson.removeSurrounding("[", "]")
                .split(",")
                .map { it.trim().removeSurrounding("\"") }
                .filter { it.isNotBlank() }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
