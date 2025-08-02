package com.signite.postservice.repository

import com.signite.postservice.domain.Post
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface PostRepository : ReactiveCrudRepository<Post, Long>
