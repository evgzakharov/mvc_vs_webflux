apply(plugin = "org.jetbrains.kotlin.plugin.spring")
apply(plugin = "org.springframework.boot")

val springVersion: String by project

tasks.withType<Jar> {
    manifest {
        attributes(
            mapOf(
                "Start-Class" to "co.fun.joker.ApplicationKt",
                "Implementation-Version" to project.version
            )
        )
    }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web:$springVersion")
}
