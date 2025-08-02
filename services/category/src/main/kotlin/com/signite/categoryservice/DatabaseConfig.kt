package com.signite.categoryservice

import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.config.EnableReactiveMongoAuditing
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories

@Configuration
@EnableReactiveMongoAuditing // @CreatedDate, @LastModifiedDate 등 자동 생성을 위해 필요
@EnableReactiveMongoRepositories(basePackages = ["com.signite.categoryservice.repository"])
class DatabaseConfig
