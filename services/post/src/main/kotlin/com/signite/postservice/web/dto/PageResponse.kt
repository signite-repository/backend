package com.signite.postservice.web.dto

data class PageResponse<T>(
    val content: List<T>,
    val pageNumber: Int,
    val pageSize: Int,
    val totalElements: Long,
    val totalPages: Int,
    val last: Boolean,
    val first: Boolean
) {
    companion object {
        fun <T> of(content: List<T>, pageNumber: Int, pageSize: Int, totalElements: Long): PageResponse<T> {
            val totalPages = if (totalElements == 0L) 0 else ((totalElements - 1) / pageSize + 1).toInt()
            return PageResponse(
                content = content,
                pageNumber = pageNumber,
                pageSize = pageSize,
                totalElements = totalElements,
                totalPages = totalPages,
                last = pageNumber >= totalPages - 1,
                first = pageNumber == 0
            )
        }
    }
}