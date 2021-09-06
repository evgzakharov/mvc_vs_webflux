package co.`fun`.joker.repository

import co.`fun`.joker.api.Decision
import org.springframework.data.mongodb.core.MongoTemplate
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
    private val mongoTemplate: MongoTemplate
) {
    fun findById(id: String): Moderation? {
        return mongoTemplate.findById(id)
    }

    fun save(moderation: Moderation) {
        mongoTemplate.save(moderation)
    }
}