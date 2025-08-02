package com.signite.postservice.service

import com.signite.postservice.domain.Comment
import com.signite.postservice.repository.CommentRepository
import com.signite.postservice.repository.PostRepository
import com.signite.postservice.web.dto.CommentRequest
import com.signite.postservice.web.dto.CommentResponse
import com.signite.postservice.web.dto.CommentUpdateRequest
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDateTime

@Service
class CommentService(
    private val commentRepository: CommentRepository,
    private val postRepository: PostRepository
) {
    
    fun createComment(postId: Long, request: CommentRequest, authorId: String): Mono<Comment> {
        return postRepository.existsById(postId)
            .flatMap { exists ->
                if (!exists) {
                    Mono.error(RuntimeException("Post not found"))
                } else {
                    if (request.parentId != null) {
                        // 대댓글인 경우
                        createReplyComment(postId, request, authorId)
                    } else {
                        // 최상위 댓글인 경우
                        createTopLevelComment(postId, request, authorId)
                    }
                }
            }
    }
    
    private fun createTopLevelComment(postId: Long, request: CommentRequest, authorId: String): Mono<Comment> {
        val comment = Comment(
            content = request.content,
            postId = postId,
            authorId = authorId,
            parentId = null,
            depth = 0,
            path = ""  // ID 생성 후 업데이트
        )
        
        return commentRepository.save(comment)
            .flatMap { savedComment ->
                // path 업데이트 (자신의 ID를 path로 설정)
                savedComment.path = savedComment.id.toString()
                commentRepository.save(savedComment)
            }
    }
    
    private fun createReplyComment(postId: Long, request: CommentRequest, authorId: String): Mono<Comment> {
        return commentRepository.findById(request.parentId!!)
            .switchIfEmpty(Mono.error(RuntimeException("Parent comment not found")))
            .flatMap { parentComment ->
                if (parentComment.postId != postId) {
                    Mono.error<Comment>(RuntimeException("Parent comment belongs to different post"))
                } else if (parentComment.depth >= 2) {
                    // 최대 깊이 제한 (대대댓글까지만 허용)
                    Mono.error<Comment>(RuntimeException("Maximum comment depth exceeded"))
                } else {
                    val comment = Comment(
                        content = request.content,
                        postId = postId,
                        authorId = authorId,
                        parentId = request.parentId,
                        depth = parentComment.depth + 1,
                        path = ""  // ID 생성 후 업데이트
                    )
                    
                    commentRepository.save(comment)
                        .flatMap { savedComment ->
                            // path 업데이트 (부모 path + 자신의 ID)
                            savedComment.path = "${parentComment.path}/${savedComment.id}"
                            commentRepository.save(savedComment)
                        }
                }
            }
    }
    
    fun getCommentsByPostId(postId: Long): Flux<CommentResponse> {
        return commentRepository.findByPostIdAndIsDeletedFalseOrderByPath(postId)
            .collectList()
            .flatMapMany { comments ->
                // 계층 구조로 변환
                val commentMap = comments.associateBy { it.id!! }
                val topLevelComments = mutableListOf<CommentResponse>()
                
                comments.forEach { comment ->
                    if (comment.parentId == null) {
                        // 최상위 댓글
                        val children = buildCommentTree(comment.id!!, commentMap)
                        topLevelComments.add(CommentResponse.fromEntity(comment, children))
                    }
                }
                
                Flux.fromIterable(topLevelComments)
            }
    }
    
    private fun buildCommentTree(parentId: Long, commentMap: Map<Long, Comment>): List<CommentResponse> {
        return commentMap.values
            .filter { it.parentId == parentId }
            .map { comment ->
                val children = buildCommentTree(comment.id!!, commentMap)
                CommentResponse.fromEntity(comment, children)
            }
    }
    
    fun updateComment(commentId: Long, request: CommentUpdateRequest, userId: String): Mono<Comment> {
        return commentRepository.findById(commentId)
            .switchIfEmpty(Mono.error(RuntimeException("Comment not found")))
            .flatMap { comment ->
                if (comment.authorId != userId) {
                    Mono.error<Comment>(RuntimeException("Unauthorized to update comment"))
                } else if (comment.isDeleted) {
                    Mono.error<Comment>(RuntimeException("Cannot update deleted comment"))
                } else {
                    comment.content = request.content
                    comment.updatedAt = LocalDateTime.now()
                    commentRepository.save(comment)
                }
            }
    }
    
    fun deleteComment(commentId: Long, userId: String): Mono<Void> {
        return commentRepository.findById(commentId)
            .switchIfEmpty(Mono.error(RuntimeException("Comment not found")))
            .flatMap { comment ->
                if (comment.authorId != userId) {
                    Mono.error<Void>(RuntimeException("Unauthorized to delete comment"))
                } else {
                    // 대댓글이 있는지 확인
                    commentRepository.findByParentIdOrderByCreatedAt(commentId)
                        .hasElements()
                        .flatMap { hasChildren ->
                            if (hasChildren) {
                                // 대댓글이 있으면 소프트 삭제
                                comment.isDeleted = true
                                comment.updatedAt = LocalDateTime.now()
                                commentRepository.save(comment).then()
                            } else {
                                // 대댓글이 없으면 하드 삭제
                                commentRepository.delete(comment)
                            }
                        }
                }
            }
    }
    
    fun getCommentCount(postId: Long): Mono<Long> {
        return commentRepository.countByPostIdAndIsDeletedFalse(postId)
    }
}