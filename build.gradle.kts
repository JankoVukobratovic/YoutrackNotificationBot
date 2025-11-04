plugins {
    kotlin("jvm") version "2.2.10"
    kotlin("plugin.serialization") version "1.9.22"
}

group = "org.jankos"
version = "1.0-SNAPSHOT"

val telegramBotVersion = "6.1.0"
val ktorVersion = "3.0.0"
val coroutinesVersion = "1.7.3"

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    testImplementation(kotlin("test"))

    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")

    implementation("io.github.kotlin-telegram-bot.kotlin-telegram-bot:telegram:$telegramBotVersion")

    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("org.slf4j:slf4j-simple:2.0.13")

    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")

    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}