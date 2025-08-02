package com.signite.postservice.config

import org.springframework.context.annotation.Configuration

@Configuration
class CategoryConfig {
    companion object {
        // 유효한 카테고리 목록 (category-db init 데이터 기반)
        val VALID_CATEGORIES = setOf(
            "notice",        // 공지사항
            "development",   // 개발
            "frontend",      // 프론트엔드
            "backend"        // 백엔드
        )
        
        // 카테고리별 권한 (예시)
        val CATEGORY_PERMISSIONS = mapOf(
            "notice" to setOf("ADMIN", "MODERATOR"),  // 공지사항은 관리자만
            "development" to setOf("USER", "ADMIN", "MODERATOR"),
            "frontend" to setOf("USER", "ADMIN", "MODERATOR"),
            "backend" to setOf("USER", "ADMIN", "MODERATOR")
        )
        
        fun isValidCategory(categoryId: String): Boolean {
            return VALID_CATEGORIES.contains(categoryId)
        }
        
        fun canWriteInCategory(categoryId: String, userRoles: List<String>): Boolean {
            val allowedRoles = CATEGORY_PERMISSIONS[categoryId] ?: return false
            return userRoles.any { allowedRoles.contains(it) }
        }
    }
}