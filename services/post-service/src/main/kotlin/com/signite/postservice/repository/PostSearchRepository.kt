package com.signite.postservice.repository

import com.signite.postservice.domain.PostDocument
import org.springframework.data.elasticsearch.annotations.Query
import org.springframework.data.elasticsearch.repository.ReactiveElasticsearchRepository
import reactor.core.publisher.Flux

interface PostSearchRepository : ReactiveElasticsearchRepository<PostDocument, String> {
    @Query("""
        {
            "bool": {
                "should": [
                    { "match": { "title": { "query": "?0", "boost": 2.0 } } },
                    { "match": { "content": "?0" } },
                    { "match": { "tags": "?0" } }
                ]
            }
        }
    """)
    fun search(query: String, page: Int, size: Int): Flux<PostDocument>
}
