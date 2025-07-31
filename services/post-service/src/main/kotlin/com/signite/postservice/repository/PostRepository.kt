package com.signite.postservice.repository

import com.signite.postservice.domain.Post
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Repository

@Repository
interface PostRepository : ReactiveMongoRepository<Post, String>
