import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.8.20"
    application

    id("org.jmailen.kotlinter") version "3.14.0"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "be.zvz"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_17
java.targetCompatibility = java.sourceCompatibility

repositories {
    mavenCentral()
    maven {
        url = uri("https://jitpack.io")
    }
}

dependencies {
    implementation(group = "io.ktor", name = "ktor-server-core-jvm", version = "2.2.4")
    implementation(group = "io.ktor", name = "ktor-server-netty-jvm", version = "2.2.4")
    implementation(group = "io.ktor", name = "ktor-server-content-negotiation", version = "2.2.4")
    implementation(group = "io.ktor", name = "ktor-server-forwarded-header", version = "2.2.4")
    implementation(group = "io.ktor", name = "ktor-serialization-jackson", version = "2.2.4")

    implementation(group = "com.fasterxml.jackson.module", name = "jackson-module-kotlin", version = "2.14.2")
    implementation(group = "com.fasterxml.jackson.module", name = "jackson-module-blackbird", version = "2.14.2")

    implementation(group = "com.github.JellyBrick", name = "ktor-rate-limit", version = "v0.0.4")

    implementation(group = "ch.qos.logback", name = "logback-classic", version = "1.4.6")

    implementation(group = "com.google.apis", name = "google-api-services-youtube", version = "v3-rev20230319-2.0.0")
    implementation(group = "com.google.http-client", name = "google-http-client-jackson2", version = "1.43.1")

    implementation(group = "com.coreoz", name = "wisp", version = "2.3.0")
    implementation(group = "com.cronutils", name = "cron-utils", version = "9.2.1")

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
