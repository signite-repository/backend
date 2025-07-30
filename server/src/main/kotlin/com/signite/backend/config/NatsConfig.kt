package com.signite.backend.config

import io.nats.client.Connection
import io.nats.client.JetStream
import io.nats.client.Nats
import io.nats.client.Options
import io.nats.client.api.RetentionPolicy
import io.nats.client.api.StorageType
import io.nats.client.api.StreamConfiguration
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import jakarta.annotation.PostConstruct
import org.springframework.context.ApplicationListener
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.stereotype.Component

@Configuration
class NatsConfig {
    
    @Value("\${nats.server.url:nats://localhost:4222}")
    private lateinit var natsServerUrl: String
    
    @Bean
    fun natsConnection(): Connection {
        val options = Options.Builder()
            .server(natsServerUrl)
            .maxReconnects(-1)
            .reconnectWait(java.time.Duration.ofSeconds(1))
            .connectionName("signite-backend")
            .build()
            
        return Nats.connect(options)
    }
    
    @Bean
    fun jetStream(connection: Connection): JetStream {
        return connection.jetStream()
    }
}

@Component
class NatsStreamInitializer(
    private val connection: Connection
) : ApplicationListener<ContextRefreshedEvent> {
    
    override fun onApplicationEvent(event: ContextRefreshedEvent) {
        createStreams()
    }
    
    private fun createStreams() {
        try {
            val jsm = connection.jetStreamManagement()
            
            val boardStreamConfig = StreamConfiguration.builder()
                .name("BOARD_EVENTS")
                .subjects("board.*", "post.*", "comment.*", "user.*")
                .retentionPolicy(RetentionPolicy.WorkQueue)
                .storageType(StorageType.Memory)
                .maxAge(java.time.Duration.ofHours(24))
                .build()
                
            try {
                jsm.addStream(boardStreamConfig)
                println("✅ NATS Stream BOARD_EVENTS created successfully")
            } catch (e: Exception) {
                println("⚠️ NATS Stream BOARD_EVENTS already exists or failed to create: ${e.message}")
            }
            
            val notificationStreamConfig = StreamConfiguration.builder()
                .name("NOTIFICATION_EVENTS")
                .subjects("notification.*", "email.*", "sms.*")
                .retentionPolicy(RetentionPolicy.WorkQueue)
                .storageType(StorageType.Memory)
                .maxAge(java.time.Duration.ofHours(48))
                .build()
                
            try {
                jsm.addStream(notificationStreamConfig)
                println("✅ NATS Stream NOTIFICATION_EVENTS created successfully")
            } catch (e: Exception) {
                println("⚠️ NATS Stream NOTIFICATION_EVENTS already exists: ${e.message}")
            }
            
        } catch (e: Exception) {
            println("🚨 NATS configuration failed: ${e.message}")
        }
    }
} 