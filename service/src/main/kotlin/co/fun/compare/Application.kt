package co.`fun`.compare

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.mongo.MongoReactiveAutoConfiguration
import org.springframework.boot.runApplication

@SpringBootApplication(exclude = [MongoReactiveAutoConfiguration::class])
class Application

fun main() {
    runApplication<Application>()
}