package com.signite.backend

import com.signite.backend.config.TestConfig
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import

@SpringBootTest
@Import(TestConfig::class)
class SigniteBackendApplicationTests {
    @Test fun contextLoads() {}
}
