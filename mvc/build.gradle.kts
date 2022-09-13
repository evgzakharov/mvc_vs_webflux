apply(plugin = "org.jetbrains.kotlin.plugin.spring")
apply(plugin = "org.springframework.boot")

val springVersion: String by project

tasks.withType<Jar> {
    manifest {
        attributes(
            mapOf(
                "Start-Class" to "co.fun.compare.ApplicationKt",
                "Implementation-Version" to project.version
            )
        )
    }
}

dependencies {
    implementation(project(":common"))

//    implementation("org.springframework.boot:spring-boot-starter-web:$springVersion")

    implementation("org.springframework.boot:spring-boot-starter-web:$springVersion") {
        exclude(module = "spring-boot-starter-tomcat")
    }
    implementation("org.springframework.boot:spring-boot-starter-undertow:$springVersion")

    implementation("org.springframework.boot:spring-boot-starter-data-mongodb:$springVersion")
}
