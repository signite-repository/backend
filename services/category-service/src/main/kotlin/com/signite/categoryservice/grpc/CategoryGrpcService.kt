package com.signite.categoryservice.grpc

import com.signite.proto.category.CategoryServiceGrpc
import com.signite.proto.category.CheckPermissionRequest
import com.signite.proto.category.CheckPermissionResponse
import com.signite.categoryservice.service.PermissionService
import io.grpc.stub.StreamObserver
import net.devh.boot.grpc.server.service.GrpcService
import org.springframework.stereotype.Component

@GrpcService
class CategoryGrpcService(
    private val permissionService: PermissionService
) : CategoryServiceGrpc.CategoryServiceImplBase() {

    override fun checkPermission(
        request: CheckPermissionRequest,
        responseObserver: StreamObserver<CheckPermissionResponse>
    ) {
        try {
            val granted = permissionService.checkPermission(
                roles = request.userRolesList,
                categoryId = request.categoryId,
                requiredPermission = request.permission
            ).block() ?: false
            
            val response = CheckPermissionResponse.newBuilder()
                .setGranted(granted)
                .setReason(if (granted) "Permission granted" else "Permission denied")
                .build()
                
            responseObserver.onNext(response)
            responseObserver.onCompleted()
        } catch (e: Exception) {
            val response = CheckPermissionResponse.newBuilder()
                .setGranted(false)
                .setReason("Error checking permission: ${e.message}")
                .build()
                
            responseObserver.onNext(response)
            responseObserver.onCompleted()
        }
    }
}