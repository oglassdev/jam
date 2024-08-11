plugins {
    kotlin("jvm")

    kotlin("plugin.serialization") version "2.0.0"
}

group = "team.ktusers"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("com.squareup:kotlinpoet:1.18.1")
    implementation("com.github.ajalt.colormath:colormath:3.6.0")
    implementation("com.sksamuel.scrimage:scrimage-core:4.1.3")
    implementation(libs.kotlinx.coroutines)
    implementation(libs.minestom)
    implementation(libs.kotlinx.serialization.json)
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}