package com.signite.backend.service

import com.signite.backend.domain.dto.CategoryDTO
import com.signite.backend.domain.dto.CategoryListDTO
import com.signite.backend.domain.dto.PostInCategoryDTO
import com.signite.backend.domain.dto.UserInPostCategoryDTO
import com.signite.backend.domain.entity.Category
import com.signite.backend.repository.CategoryCacheRepository
import com.signite.backend.repository.CategoryRepository
import com.signite.backend.repository.PostRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono

@Controller
class CategoryService(
    @Autowired private val categoryRepository: CategoryRepository,
    @Autowired private val categoryCacheRepository: CategoryCacheRepository,
    @Autowired private val postRepository: PostRepository,
) {
    companion object {
        private val logger = LoggerFactory.getLogger(CategoryService::class.java)
    }

    // 카테고리 모두 가져오기
    fun getCategoryAll(): Flux<CategoryDTO> {
        return categoryRepository.findAllAndCount()
    }

    // 카테고리 아이디로 포스트 가져오기
    fun getAllPostByCategoryId(
        categoryId: Int,
        offset: Int,
        limit: Int? = 8,
    ): Mono<List<PostInCategoryDTO>> {
        return Mono.just(categoryId).flatMap { categoryId ->
            postRepository.findAllByCategoryId(categoryId, offset, limit).collectList().toMono()
        }.flatMap {
            var posts = mutableListOf<PostInCategoryDTO>()
            for (post in it) {
                var result =
                    PostInCategoryDTO(
                        id = post.id,
                        summary = post.summary,
                        title = post.title,
                        createdAt = post.createdat,
                        postcount = post.postcount,
                        viewcount = post.viewcount,
                        commentcount = post.commentcount,
                        last = post.last,
                        images = post.images,
                        user =
                            UserInPostCategoryDTO(
                                id = post.userid,
                                username = post.username,
                                imageUrl = post.imageurl,
                            ),
                    )
                posts.add(result)
            }
            posts.toMono()
        }
    }

    // 카테고리 확인하고 없으면 생성
    fun createCategoryIfNot(title: String): Mono<Category> {
        return categoryRepository.existsByTitle(title).flatMap {
            if (it) {
                categoryRepository.findByTitle(title)
            } else {
                categoryRepository.save(Category(title = title))
            }
        }
    }

    // 카테고리 캐시 확인하고 없으면 생성
    fun getAllAndCache(): Mono<MutableList<CategoryDTO>> {
        return categoryCacheRepository.findAllAndCaching()
            .switchIfEmpty(
                categoryRepository
                    .findAllAndCount()
                    .collectList()
                    .flatMap {
                        categoryCacheRepository.setCategoriesAllAndCaching(it)
                    },
            ).flatMap {
                it.categories.toMono()
            }
    }

    // 카테고리 캐시 리셋
    fun resetCategoryCash(): Mono<CategoryListDTO> {
        return categoryRepository
            .findAllAndCount()
            .collectList()
            .flatMap {
                categoryCacheRepository.setCategoriesAllAndCaching(it)
            }
    }
}
