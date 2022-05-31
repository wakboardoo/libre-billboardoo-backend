import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.21"
    application

    kotlin("plugin.serialization").version("1.6.21")

    id("org.jmailen.kotlinter") version "3.10.0"
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

group = "be.zvz"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_11
java.targetCompatibility = java.sourceCompatibility

repositories {
    mavenCentral()
    maven {
        url = uri("https://jitpack.io")
    }
}

dependencies {
    implementation(group = "io.ktor", name = "ktor-server-core", version = "2.0.2")
    implementation(group = "io.ktor", name = "ktor-server-netty", version = "2.0.2")
    implementation(group = "io.ktor", name = "ktor-server-content-negotiation", version = "2.0.2")
    implementation(group = "io.ktor", name = "ktor-serialization-kotlinx-json", version = "2.0.2")

    implementation(group = "com.github.JellyBrick", name = "ktor-rate-limit", version = "v0.0.4")

    implementation(group = "ch.qos.logback", name = "logback-classic", version = "1.2.11")

    implementation(group = "com.google.apis", name = "google-api-services-youtube", version = "v3-rev20220515-1.32.1")

    implementation(group = "com.coreoz", name = "wisp", version = "2.2.2")
    implementation(group = "com.cronutils", name = "cron-utils", version = "9.1.6")

    implementation(group = "org.jetbrains.kotlin", name = "kotlin-stdlib-jdk8")
    implementation(group = "org.jetbrains.kotlin", name = "kotlin-reflect")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = java.sourceCompatibility.toString()
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

application {
    mainClass.set("be.zvz.billboardoo.ApplicationKt")
}
