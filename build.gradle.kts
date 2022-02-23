import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val springVersion: String by project
val jacksonVersion: String by project
val kotlinCoroutinesVersion: String by project
extra["kotlin-coroutines.version"] = kotlinCoroutinesVersion

val mockkVersion: String by project
val junitVersion: String by project

group = "co.fun"
version = "0.0.1-SNAPSHOT"
description = "compare"

plugins {
    kotlin("jvm") version "1.6.10"
    kotlin("plugin.serialization") version "1.6.10" apply false
    kotlin("plugin.spring") version "1.6.10" apply false
    id("io.spring.dependency-management") version "1.0.11.RELEASE" apply false
    id("org.springframework.boot") version "2.6.3" apply false
}

repositories {
    mavenCentral()
}

subprojects {
    apply<org.jetbrains.kotlin.gradle.plugin.KotlinPluginWrapper>()

    repositories {
        mavenCentral()
    }

    tasks.withType<Test> {
        useJUnitPlatform()
        jvmArgs = listOf(
            "-Xmx4G",
        )
    }

    dependencies {
        implementation(kotlin("stdlib-jdk8"))
        implementation(kotlin("reflect"))

        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinCoroutinesVersion")
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:$kotlinCoroutinesVersion")
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactive:$kotlinCoroutinesVersion")

        implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")

        testImplementation("io.mockk:mockk:$mockkVersion")
        testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
        testImplementation("org.junit.jupiter:junit-jupiter-params:$junitVersion")
        testImplementation("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = "17"
            freeCompilerArgs = listOf("-Xjsr305=strict")
        }
    }

}
