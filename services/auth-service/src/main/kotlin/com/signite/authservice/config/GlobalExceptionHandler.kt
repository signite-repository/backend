package com.signite.authservice.config

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
            "User already exists" -> HttpStatus.CONFLICT to "이미 존재하는 사용자입니다"
            "Invalid credentials" -> HttpStatus.UNAUTHORIZED to "아이디 또는 비밀번호가 올바르지 않습니다"
            "User not found" -> HttpStatus.NOT_FOUND to "사용자를 찾을 수 없습니다"
            "Token expired" -> HttpStatus.UNAUTHORIZED to "토큰이 만료되었습니다"
            "Invalid token" -> HttpStatus.UNAUTHORIZED to "유효하지 않은 토큰입니다"
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
    
    @ExceptionHandler(WebExchangeBindException::class)
    fun handleValidationException(ex: WebExchangeBindException, exchange: ServerWebExchange): Mono<ResponseEntity<ErrorResponse>> {
        val errors = ex.bindingResult.fieldErrors
            .joinToString(", ") { field ->
                when (field.field) {
                    "username" -> "사용자명${getFieldError(field.code)}"
                    "password" -> "비밀번호${getFieldError(field.code)}"
                    "email" -> "이메일${getFieldError(field.code)}"
                    "fullName" -> "이름${getFieldError(field.code)}"
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
            "Email" -> " 형식이 올바르지 않습니다"
            "Size" -> " 길이가 올바르지 않습니다"
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