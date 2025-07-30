package com.signite.backend.service

import com.signite.backend.domain.dto.ImageUrlDTO
import com.signite.backend.repository.ImageUrlRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.codec.multipart.FilePart
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class UploadService(
    @Autowired private val imageUrlRepository: ImageUrlRepository,
) {
    fun uploadImage(filePart: FilePart): Mono<ImageUrlDTO> {
        return Mono.error<ImageUrlDTO>(RuntimeException("S3 업로드 기능은 현재 비활성화되어 있습니다. AWS 의존성 재설정이 필요합니다."))
    }

    fun deleteImage(fileName: String): Mono<String> {
        return Mono.error<String>(RuntimeException("S3 삭제 기능은 현재 비활성화되어 있습니다. AWS 의존성 재설정이 필요합니다."))
    }
}
