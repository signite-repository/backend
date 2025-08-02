package com.signite.postservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.elasticsearch.repository.config.EnableReactiveElasticsearchRepositories
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories

@SpringBootApplication
@EnableR2dbcRepositories(basePackages = ["com.signite.postservice.repository"])
@EnableReactiveElasticsearchRepositories(basePackages = ["com.signite.postservice.repository"])
class PostServiceApplication

fun main(args: Array<String>) {
	runApplication<PostServiceApplication>(*args)
}
