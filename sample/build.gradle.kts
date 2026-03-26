plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
    application
}

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":ktor-oauth-session-core"))
    implementation(project(":ktor-oauth-session-exposed"))
    implementation(project(":ktor-oauth-session-redis"))
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.content.neg)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.serialization.json)
    implementation(libs.hikari)
    implementation(libs.h2)
    implementation(libs.logback)
}

application {
    mainClass.set("dev.jwillert.ktor.sample.SampleAppKt")
}
