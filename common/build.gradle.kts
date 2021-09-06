apply(plugin = "org.jetbrains.kotlin.plugin.serialization")

val jacksonVersion: String by project
val kotlinSerializationVersion: String by project

dependencies {
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinSerializationVersion")
}
