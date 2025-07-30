package com.signite.backend.service

import com.signite.backend.domain.dto.*
import com.signite.backend.domain.entity.Comment
import com.signite.backend.repository.CommentRepository
import com.signite.backend.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono

@Service
class CommentService(
    @Autowired private val commentRepository: CommentRepository,
    @Autowired private val userRepository: UserRepository,
) {
    companion object {
        private val logger = LoggerFactory.getLogger(CommentService::class.java)
    }

    // 코멘트 생성
    fun createComment(
        commentForm: CommentFormDTO,
        userId: Int,
        postId: Int,
    ): Mono<Comment> {
        return commentRepository.save(
            Comment(
                content = commentForm.content,
                userId = userId,
                postId = postId,
            ),
        )
    }

    // 코멘트 삭제
    fun deleteComment(commentId: Int): Mono<Boolean> {
        return commentRepository.deleteById(commentId).thenReturn(true)
    }

    // 단일 코멘트 가져오기
    fun getComment(commentId: Int): Mono<Comment> {
        return commentRepository.existsById(commentId)
            .flatMap { isExist ->
                if (isExist) {
                    commentRepository.findById(commentId)
                } else {
                    throw error("댓글이 없습니다")
                }
            }
    }

    // 포스트로 모두 가져오기
    fun getCommentByPostId(postId: Int): Mono<List<CommentDTO>> {
        return commentRepository.findAllByPostIdAndUser(postId)
            .collectList()
            .map { commentsAll ->
                commentsAll.map { comment ->
                    CommentDTO(
                        id = comment.comment_id,
                        content = comment.comment_content,
                        createdAt = comment.comment_createdat,
                        user = UserDTO(
                            id = comment.comment_userid,
                            username = comment.comment_username,
                            hashedPassword = "",
                            email = comment.comment_email,
                            imageUrl = comment.comment_imageurl,
                            githubUrl = comment.comment_githuburl,
                            summary = comment.comment_summary,
                        ),
                    )
                }
            }
    }
}
