plugins {
    kotlin("jvm") version "2.3.10"
    kotlin("plugin.serialization") version "2.3.10"
    id("io.ktor.plugin") version "3.1.2"
    application
}

application {
    mainClass.set("org.example.MainKt")
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-server-core")
    implementation("io.ktor:ktor-server-netty")
    implementation("io.ktor:ktor-server-content-negotiation")
    implementation("io.ktor:ktor-serialization-kotlinx-json")
    implementation("ch.qos.logback:logback-classic:1.5.13")
    testImplementation(kotlin("test"))
    testImplementation("io.ktor:ktor-server-test-host")
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
}