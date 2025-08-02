package com.signite.postservice.config

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.bind.support.WebExchangeBindException
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import java.time.LocalDateTime

@RestControllerAdvice
class GlobalExceptionHandler {
    
    @ExceptionHandler(RuntimeException::class)
    fun handleRuntimeException(ex: RuntimeException, exchange: ServerWebExchange): Mono<ResponseEntity<ErrorResponse>> {
        val (status, koreanMessage) = when (ex.message) {
            "Post not found" -> HttpStatus.NOT_FOUND to "게시글을 찾을 수 없습니다"
            "Unauthorized to update post" -> HttpStatus.FORBIDDEN to "게시글 수정 권한이 없습니다"
            "Unauthorized to delete post" -> HttpStatus.FORBIDDEN to "게시글 삭제 권한이 없습니다"
            "Invalid category" -> HttpStatus.BAD_REQUEST to "유효하지 않은 카테고리입니다"
            "Title is required" -> HttpStatus.BAD_REQUEST to "제목은 필수 항목입니다"
            "Content is required" -> HttpStatus.BAD_REQUEST to "내용은 필수 항목입니다"
            
            // 댓글 관련 에러
            "Comment not found" -> HttpStatus.NOT_FOUND to "댓글을 찾을 수 없습니다"
            "Parent comment not found" -> HttpStatus.NOT_FOUND to "상위 댓글을 찾을 수 없습니다"
            "Unauthorized to update comment" -> HttpStatus.FORBIDDEN to "댓글 수정 권한이 없습니다"
            "Unauthorized to delete comment" -> HttpStatus.FORBIDDEN to "댓글 삭제 권한이 없습니다"
            "Cannot update deleted comment" -> HttpStatus.BAD_REQUEST to "삭제된 댓글은 수정할 수 없습니다"
            "Maximum comment depth exceeded" -> HttpStatus.BAD_REQUEST to "댓글 깊이가 최대치를 초과했습니다"
            "Parent comment belongs to different post" -> HttpStatus.BAD_REQUEST to "상위 댓글이 다른 게시글에 속해 있습니다"
            
            else -> HttpStatus.BAD_REQUEST to (ex.message ?: "알 수 없는 오류가 발생했습니다")
        }
        
        val errorResponse = ErrorResponse(
            timestamp = LocalDateTime.now(),
            status = status.value(),
            error = status.reasonPhrase,
            message = koreanMessage,
            path = exchange.request.path.value()
        )
        
        return Mono.just(ResponseEntity.status(status).body(errorResponse))
    }
    
    @ExceptionHandler(SecurityException::class)
    fun handleSecurityException(ex: SecurityException, exchange: ServerWebExchange): Mono<ResponseEntity<ErrorResponse>> {
        val errorResponse = ErrorResponse(
            timestamp = LocalDateTime.now(),
            status = HttpStatus.FORBIDDEN.value(),
            error = "Forbidden",
            message = "해당 카테고리에 글을 작성할 권한이 없습니다",
            path = exchange.request.path.value()
        )
        
        return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse))
    }
    
    @ExceptionHandler(WebExchangeBindException::class)
    fun handleValidationException(ex: WebExchangeBindException, exchange: ServerWebExchange): Mono<ResponseEntity<ErrorResponse>> {
        val errors = ex.bindingResult.fieldErrors
            .joinToString(", ") { field ->
                when (field.field) {
                    "title" -> "제목${getFieldError(field.code)}"
                    "content" -> "내용${getFieldError(field.code)}"
                    "categoryId" -> "카테고리${getFieldError(field.code)}"
                    "tags" -> "태그${getFieldError(field.code)}"
                    else -> "${field.field}${getFieldError(field.code)}"
                }
            }
        
        val errorResponse = ErrorResponse(
            timestamp = LocalDateTime.now(),
            status = HttpStatus.BAD_REQUEST.value(),
            error = "잘못된 요청",
            message = errors.ifEmpty { "필수 필드가 누락되었습니다" },
            path = exchange.request.path.value()
        )
        
        return Mono.just(ResponseEntity.badRequest().body(errorResponse))
    }
    
    @ExceptionHandler(Exception::class)
    fun handleGenericException(ex: Exception, exchange: ServerWebExchange): Mono<ResponseEntity<ErrorResponse>> {
        val errorResponse = ErrorResponse(
            timestamp = LocalDateTime.now(),
            status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
            error = "내부 서버 오류",
            message = "서버에 오류가 발생했습니다. 잠시 후 다시 시도해주세요",
            path = exchange.request.path.value()
        )
        
        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse))
    }
    
    private fun getFieldError(code: String?): String {
        return when (code) {
            "NotNull", "NotEmpty", "NotBlank" -> "은(는) 필수 항목입니다"
            "Size" -> " 길이가 올바르지 않습니다"
            "Min" -> "이(가) 너무 작습니다"
            "Max" -> "이(가) 너무 큽니다"
            else -> "이(가) 올바르지 않습니다"
        }
    }
}

data class ErrorResponse(
    val timestamp: LocalDateTime,
    val status: Int,
    val error: String,
    val message: String,
    val path: String
)