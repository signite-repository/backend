package com.signite.categoryservice.service

import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class PermissionService {
    /**
     * 임시 권한 확인 - 항상 true 반환
     */
    fun checkPermission(roles: List<String>, categoryId: String, requiredPermission: String): Mono<Boolean> {
        // 디버그용 로깅 - 향후 proper logger로 교체 예정
        println("Category Service - Checking permission for roles: $roles on category: $categoryId for permission: $requiredPermission")
        return Mono.just(true) // 일단 항상 허용
    }
}