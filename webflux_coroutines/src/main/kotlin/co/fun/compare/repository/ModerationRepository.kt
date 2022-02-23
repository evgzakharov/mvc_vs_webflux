package co.`fun`.compare.repository

import co.`fun`.compare.api.Decision
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.findById
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.MongoId
import org.springframework.stereotype.Repository

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
    suspend fun findById(id: String): Moderation? {
        return mongoTemplate.findById<Moderation>(id).awaitFirstOrNull()
    }

    suspend fun save(moderation: Moderation): Moderation? {
        return mongoTemplate.save(moderation).awaitFirstOrNull()
    }
}