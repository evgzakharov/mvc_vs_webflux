apply(plugin = "org.jetbrains.kotlin.plugin.spring")
apply(plugin = "org.springframework.boot")

val springVersion: String by project
val kotlinCoroutinesVersion: String by project

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

    implementation("org.springframework.boot:spring-boot-starter-webflux:$springVersion")
    implementation("org.springframework.boot:spring-boot-starter-data-mongodb-reactive:$springVersion")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinCoroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactive:$kotlinCoroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:$kotlinCoroutinesVersion")
}
