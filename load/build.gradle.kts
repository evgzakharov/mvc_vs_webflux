plugins {
    id("org.openjfx.javafxplugin") version "0.0.9"
}

val ktorClientVersion: String by project

dependencies {
    implementation("io.ktor:ktor-client-core-jvm:$ktorClientVersion")
    implementation("io.ktor:ktor-client-java:$ktorClientVersion")
}
