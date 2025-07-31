package com.signite.postservice.service

import com.signite.proto.category.CategoryServiceGrpcKt
import com.signite.proto.category.CheckPermissionRequest
import kotlinx.coroutines.reactor.mono
import net.devh.boot.grpc.client.inject.GrpcClient
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class CategoryClientService {

    @GrpcClient("category-service")
    private lateinit var client: CategoryServiceGrpcKt.CategoryServiceCoroutineStub

    fun canCreatePost(roles: List<String>, categoryId: String): Mono<Boolean> = mono {
        try {
            val request = CheckPermissionRequest.newBuilder()
                .addAllUserRoles(roles)
                .setCategoryId(categoryId)
                .setPermission("can_create_post")
                .build()
            client.checkPermission(request).granted
        } catch (e: Exception) {
            // 로깅 추가 (향후 proper logger로 교체 예정)
            // println("Error calling category-service: ${e.message}")
            false
        }
    }
}
