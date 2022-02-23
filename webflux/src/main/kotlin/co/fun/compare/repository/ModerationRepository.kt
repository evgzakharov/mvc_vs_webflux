package co.`fun`.compare.repository

import co.`fun`.compare.api.Decision
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.findById
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.MongoId
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono

@Document("moderation")
data class Moderation(
    @MongoId
    val id: String,
    val decision: Decision
)

@Repository
class ModerationRepository(
    private val mongoTemplate: ReactiveMongoTemplate
) {
    fun findById(id: String): Mono<Moderation> {
        return mongoTemplate.findById(id)
    }

    fun save(moderation: Moderation): Mono<Moderation> {
        return mongoTemplate.save(moderation)
    }
}