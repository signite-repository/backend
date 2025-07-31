package com.signite.categoryservice

import org.springframework.context.annotation.Configuration
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing
import org.springframework.transaction.annotation.EnableTransactionManagement

@Configuration
@EnableR2dbcAuditing // @CreatedDate, @LastModifiedDate 등 자동 생성을 위해 필요
@EnableTransactionManagement // @Transactional 어노테이션을 사용하기 위해 필요
class DatabaseConfig
