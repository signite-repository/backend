package com.signite.categoryservice.repository

import com.signite.categoryservice.domain.Category
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

interface CategoryRepository : ReactiveCrudRepository<Category, UUID> {

    /**
     * 특정 경로의 직계 자식을 찾습니다.
     * ltree 연산자: `~`는 ltree 쿼리를 의미, `*.{1}`는 직계 자식(1단계 깊이)을 의미합니다.
     */
    @Query("SELECT * FROM categories WHERE path ~ :parentPath::ltree || '.*{1}' ORDER BY display_order")
    fun findDirectChildren(parentPath: String): Flux<Category>

    /**
     * 특정 경로의 모든 하위 자손을 찾습니다.
     * ltree 연산자: `<@`는 왼쪽 경로가 오른쪽 경로의 하위 경로인지를 확인합니다.
     */
    @Query("SELECT * FROM categories WHERE path <@ :parentPath::ltree AND path != :parentPath::ltree ORDER BY path")
    fun findAllDescendants(parentPath: String): Flux<Category>

    /**
     * 특정 경로의 모든 상위 조상을 찾습니다.
     * ltree 연산자: `@>`는 왼쪽 경로가 오른쪽 경로의 상위 경로인지를 확인합니다.
     */
    @Query("SELECT * FROM categories WHERE path @> :childPath::ltree AND path != :childPath::ltree ORDER BY path")
    fun findAllAncestors(childPath: String): Flux<Category>
    
    /**
     * 슬러그(slug)를 기준으로 카테고리를 찾습니다.
     */
    fun findBySlug(slug: String): Mono<Category>
}
