package com.signite.categoryservice.repository

import com.signite.categoryservice.domain.Category
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.test.context.ActiveProfiles
import reactor.test.StepVerifier

@DataMongoTest
@ActiveProfiles("test")
class CategoryRepositoryTest {

    @Autowired
    private lateinit var categoryRepository: CategoryRepository

    @BeforeEach
    fun setUp() {
        // 테스트 전 데이터 초기화
        categoryRepository.deleteAll().block()
    }

    @Test
    fun `카테고리 저장 및 조회가 정상적으로 동작한다`() {
        // given
        val category = Category(
            name = "Test Category",
            slug = "test",
            parentId = null,
            path = "test",
            level = 0,
            displayOrder = 1,
            metadata = mapOf("key" to "value")
        )

        // when
        val savedCategory = categoryRepository.save(category)
        val foundCategory = savedCategory.flatMap { saved ->
            categoryRepository.findById(saved.id!!)
        }

        // then
        StepVerifier.create(foundCategory)
            .expectNextMatches { found ->
                found.name == "Test Category" &&
                found.slug == "test" &&
                found.metadata?.get("key") == "value"
            }
            .verifyComplete()
    }

    @Test
    fun `findBySlug가 정상적으로 동작한다`() {
        // given
        val category = Category(
            name = "Test Category",
            slug = "test-slug",
            parentId = null,
            path = "test-slug",
            level = 0,
            displayOrder = 1
        )

        // when
        val result = categoryRepository.save(category)
            .then(categoryRepository.findBySlug("test-slug"))

        // then
        StepVerifier.create(result)
            .expectNextMatches { found ->
                found.slug == "test-slug"
            }
            .verifyComplete()
    }

    @Test
    fun `findByParentIdOrderByDisplayOrder가 정상적으로 동작한다`() {
        // given
        val parent = Category(
            id = "parent1",
            name = "Parent",
            slug = "parent",
            parentId = null,
            path = "parent",
            level = 0,
            displayOrder = 1
        )

        val child1 = Category(
            name = "Child 1",
            slug = "child1",
            parentId = "parent1",
            path = "parent/child1",
            level = 1,
            displayOrder = 2
        )

        val child2 = Category(
            name = "Child 2",
            slug = "child2",
            parentId = "parent1",
            path = "parent/child2",
            level = 1,
            displayOrder = 1
        )

        // when
        val result = categoryRepository.save(parent)
            .then(categoryRepository.save(child1))
            .then(categoryRepository.save(child2))
            .thenMany(categoryRepository.findByParentIdOrderByDisplayOrder("parent1"))
            .collectList()

        // then
        StepVerifier.create(result)
            .expectNextMatches { children ->
                children.size == 2 &&
                children[0].displayOrder == 1 &&
                children[1].displayOrder == 2
            }
            .verifyComplete()
    }

    @Test
    fun `findByPathStartingWithOrderByPath가 정상적으로 동작한다`() {
        // given
        val root = Category(
            name = "Root",
            slug = "root",
            parentId = null,
            path = "root",
            level = 0,
            displayOrder = 1
        )

        val sub1 = Category(
            name = "Sub1",
            slug = "sub1",
            parentId = "1",
            path = "root/sub1",
            level = 1,
            displayOrder = 1
        )

        val sub2 = Category(
            name = "Sub2",
            slug = "sub2",
            parentId = "2",
            path = "root/sub1/sub2",
            level = 2,
            displayOrder = 1
        )

        // when
        val result = categoryRepository.save(root)
            .then(categoryRepository.save(sub1))
            .then(categoryRepository.save(sub2))
            .thenMany(categoryRepository.findByPathStartingWithOrderByPath("root"))
            .collectList()

        // then
        StepVerifier.create(result)
            .expectNextMatches { categories ->
                categories.size == 3 &&
                categories[0].path == "root" &&
                categories[1].path == "root/sub1" &&
                categories[2].path == "root/sub1/sub2"
            }
            .verifyComplete()
    }
}