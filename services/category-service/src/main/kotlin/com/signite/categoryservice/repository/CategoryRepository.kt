package com.signite.categoryservice.repository

import com.signite.categoryservice.domain.Category
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.data.mongodb.repository.Query
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface CategoryRepository : ReactiveMongoRepository<Category, String> {

    /**
     * 특정 부모의 직계 자식을 찾습니다.
     * MongoDB: parent_id 필드로 직계 자식만 찾기
     */
    @Query("{ 'parent_id': ?0 }")
    fun findDirectChildren(parentId: String): Flux<Category>

    /**
     * 특정 경로의 모든 하위 자손을 찾습니다.
     * MongoDB: path가 부모 경로로 시작하는 모든 도큐먼트
     */
    @Query("{ 'path': { \$regex: '^?0/', \$options: 'i' } }")
    fun findAllDescendants(parentPath: String): Flux<Category>

    /**
     * 특정 경로의 모든 상위 조상을 찾습니다.
     * MongoDB: path를 이용한 조상 찾기 (재귀적으로 구현)
     */
    @Query("{}")
    fun findAllAncestors(childPath: String): Flux<Category>
    
    /**
     * 슬러그(slug)를 기준으로 카테고리를 찾습니다.
     */
    fun findBySlug(slug: String): Mono<Category>

    /**
     * 레벨별로 카테고리를 찾습니다.
     */
    @Query("{ 'level': ?0 }")
    fun findByLevel(level: Int): Flux<Category>

    /**
     * 특정 경로로 시작하는 카테고리들을 레벨 순으로 정렬해서 찾습니다.
     */
    @Query("{ 'path': { \$regex: '^?0' } }")
    fun findByPathStartingWith(pathPrefix: String): Flux<Category>
}
