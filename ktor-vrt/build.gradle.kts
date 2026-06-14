plugins {
    kotlin("jvm")
    `maven-publish`
}

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(17)
}

tasks.withType<Test> {
    useJUnitPlatform()
}

val libs = versionCatalogs.named("libs")

dependencies {
    // All consumer-facing: a consumer that depends on ktor-vrt (added by the
    // Gradle plugin) gets these transitively, so the plugin adds only ONE dep.
    api(libs.findLibrary("kotlinx-html-jvm").get())
    api(libs.findLibrary("playwright").get())
    api(libs.findLibrary("testcontainers-core").get())
    api(libs.findLibrary("image-comparison").get())
    api(libs.findLibrary("kotest-runner-junit5").get())
    api(libs.findLibrary("kotest-assertions-core").get())
    api(libs.findLibrary("kotest-framework-datatest").get())
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/${System.getenv("GITHUB_REPOSITORY") ?: "jwillert/ktor-plugins"}")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
