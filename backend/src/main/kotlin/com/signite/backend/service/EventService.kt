package com.signite.backend.service

import com.fasterxml.jackson.databind.ObjectMapper
import io.nats.client.JetStream
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.time.LocalDateTime

@Service
class EventService(
    @Autowired private val jetStream: JetStream,
    @Autowired private val objectMapper: ObjectMapper
) {
    
    fun publishPostCreated(postId: Int, userId: Int, categoryId: Int): Mono<Void> {
        val event = mapOf(
            "eventType" to "POST_CREATED",
            "postId" to postId,
            "userId" to userId,
            "categoryId" to categoryId,
            "timestamp" to LocalDateTime.now().toString()
        )
        
        return publishEvent("post.created", event)
    }
    
    fun publishCommentCreated(commentId: Int, postId: Int, userId: Int): Mono<Void> {
        val event = mapOf(
            "eventType" to "COMMENT_CREATED",
            "commentId" to commentId,
            "postId" to postId,
            "userId" to userId,
            "timestamp" to LocalDateTime.now().toString()
        )
        
        return publishEvent("comment.created", event)
    }
    
    fun publishUserRegistered(userId: Int, username: String, email: String): Mono<Void> {
        val event = mapOf(
            "eventType" to "USER_REGISTERED",
            "userId" to userId,
            "username" to username,
            "email" to email,
            "timestamp" to LocalDateTime.now().toString()
        )
        
        return publishEvent("user.registered", event)
    }
    
    fun publishPostDeleted(postId: Int, userId: Int): Mono<Void> {
        val event = mapOf(
            "eventType" to "POST_DELETED",
            "postId" to postId,
            "userId" to userId,
            "timestamp" to LocalDateTime.now().toString()
        )
        
        return publishEvent("post.deleted", event)
    }
    
    fun publishCommentDeleted(commentId: Int, postId: Int, userId: Int): Mono<Void> {
        val event = mapOf(
            "eventType" to "COMMENT_DELETED",
            "commentId" to commentId,
            "postId" to postId,
            "userId" to userId,
            "timestamp" to LocalDateTime.now().toString()
        )
        
        return publishEvent("comment.deleted", event)
    }
    
    private fun publishEvent(subject: String, eventData: Map<String, Any>): Mono<Void> {
        return Mono.fromCallable {
            try {
                val jsonData = objectMapper.writeValueAsBytes(eventData)
                jetStream.publish(subject, jsonData)
                println("📤 Event published: $subject -> $eventData")
            } catch (e: Exception) {
                println("🚨 Failed to publish event: $subject -> ${e.message}")
                throw e
            }
        }.then()
    }
    
    fun subscribeToEvents(subject: String, consumerName: String, callback: (Map<String, Any>) -> Unit) {
        try {
            val subscription = jetStream.subscribe(subject)
            
            Thread {
                while (true) {
                    try {
                        val message = subscription.nextMessage(java.time.Duration.ofSeconds(1))
                        if (message != null) {
                            val eventData = objectMapper.readValue(message.data, Map::class.java) as Map<String, Any>
                            callback(eventData)
                            message.ack()
                            println("📥 Event received: $subject -> $eventData")
                        }
                    } catch (e: Exception) {
                        println("🚨 Error processing message: ${e.message}")
                    }
                }
            }.start()
            
            println("👂 Subscribed to events: $subject with consumer: $consumerName")
        } catch (e: Exception) {
            println("🚨 Failed to subscribe to events: $subject -> ${e.message}")
        }
    }
} 