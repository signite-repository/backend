package com.signite.postservice.service

import com.signite.proto.category.CategoryServiceGrpc
import com.signite.proto.category.CheckPermissionRequest
import net.devh.boot.grpc.client.inject.GrpcClient
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class CategoryClientService {

    @GrpcClient("category-service")
    private lateinit var client: CategoryServiceGrpc.CategoryServiceBlockingStub

    fun canCreatePost(roles: List<String>, categoryId: String): Mono<Boolean> {
        return Mono.fromCallable {
            try {
                val request = CheckPermissionRequest.newBuilder()
                    .addAllUserRoles(roles)
                    .setCategoryId(categoryId)
                    .setPermission("can_create_post")
                    .build()
                client.checkPermission(request).granted
            } catch (e: Exception) {
                // 로깅 추가 (향후 proper logger로 교체 예정)
                println("Post Service - Category Service 호출 실패, 임시로 권한 허용: ${e.message}")
                true // Category Service 없어도 Post 생성 허용 (임시)
            }
        }
    }
}
