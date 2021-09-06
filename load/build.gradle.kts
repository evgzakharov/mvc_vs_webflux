val ktorClientVersion: String by project

dependencies {
    implementation(project(":common"))

    implementation("io.ktor:ktor-client-core-jvm:$ktorClientVersion")
    implementation("io.ktor:ktor-client-java:$ktorClientVersion")
    implementation("io.ktor:ktor-client-serialization:$ktorClientVersion")
    implementation("io.ktor:ktor-client-logging:$ktorClientVersion")

    implementation("ch.qos.logback:logback-classic:1.2.5")
}
