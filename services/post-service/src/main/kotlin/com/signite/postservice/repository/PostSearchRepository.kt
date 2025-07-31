package com.signite.postservice.repository

import com.signite.postservice.domain.PostDocument
import org.springframework.data.elasticsearch.repository.ReactiveElasticsearchRepository

interface PostSearchRepository : ReactiveElasticsearchRepository<PostDocument, String>
