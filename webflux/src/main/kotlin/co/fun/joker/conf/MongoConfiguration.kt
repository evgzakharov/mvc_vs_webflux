package co.`fun`.joker.conf

import co.`fun`.joker.api.Env
import co.`fun`.joker.repository.BaseRepository
import com.mongodb.ReadPreference
import com.mongodb.reactivestreams.client.MongoClient
import com.mongodb.reactivestreams.client.MongoClients
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.convert.DefaultMongoTypeMapper
import org.springframework.data.mongodb.core.convert.MappingMongoConverter
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories

@Configuration
@EnableReactiveMongoRepositories(basePackageClasses = [BaseRepository::class])
class MongoConfiguration {
    @Bean
    fun mongoClient(): MongoClient {
        return MongoClients.create(Env.MONGO)
    }

    @Bean
    fun reactiveMongoTemplate(mongoClient: MongoClient): ReactiveMongoTemplate {
        val mongoTemplate = ReactiveMongoTemplate(mongoClient, Env.MONGO_DB)
        mongoTemplate.setReadPreference(ReadPreference.secondaryPreferred())

        prepareTemplate(mongoTemplate)

        return mongoTemplate
    }
}

fun prepareTemplate(mongoTemplate: ReactiveMongoTemplate) {
    val converter = mongoTemplate.converter as MappingMongoConverter
    converter.setTypeMapper(DefaultMongoTypeMapper(null))
}

