/**
 * Convention plugin applied to every library module (ktor-oauth-session-*, ktor-rate-limiting, …).
 * Provides: Kotlin JVM + Serialization, maven-publish, JVM 17, JUnit 5, common test deps.
 */
plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
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
    // Every library module gets these test dependencies for free
    testImplementation(libs.findLibrary("kotest-runner-junit5").get())
    testImplementation(libs.findLibrary("kotest-assertions-core").get())
    testImplementation(libs.findLibrary("logback").get())
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            // artifactId = subproject name — set automatically by Gradle
            // groupId + version — inherited from root gradle.properties via allprojects {}
            versionMapping {
                usage("java-api") { fromResolutionOf("runtimeClasspath") }
                usage("java-runtime") { fromResolutionResult() }
            }
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
