val ktorClientVersion: String by project
val asyncClientVersion: String by project

plugins {
    application
}

application {
    mainClass.set("co.fun.joker.MainKt")
}

dependencies {
    implementation(project(":common"))

    implementation("io.ktor:ktor-client-core-jvm:$ktorClientVersion")
    implementation("io.ktor:ktor-client-apache:$ktorClientVersion")
    implementation("io.ktor:ktor-client-serialization:$ktorClientVersion")
    implementation("io.ktor:ktor-client-logging:$ktorClientVersion")

    implementation("org.asynchttpclient:async-http-client:$asyncClientVersion")

    implementation("ch.qos.logback:logback-classic:1.2.5")
}
