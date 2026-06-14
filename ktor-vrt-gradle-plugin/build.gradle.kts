plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    `maven-publish`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

kotlin {
    jvmToolchain(17)
}

// Bake this module's version into ktor-vrt.properties so the applied plugin can
// add the matching `dev.jwillert:ktor-vrt:<version>` dependency to consumers.
tasks.named<ProcessResources>("processResources") {
    val pluginVersion = project.version.toString()
    inputs.property("ktorVrtVersion", pluginVersion)
    filesMatching("ktor-vrt.properties") {
        expand("ktorVrtVersion" to pluginVersion)
    }
}

gradlePlugin {
    plugins {
        create("ktorVrt") {
            id = "dev.jwillert.ktor-vrt"
            implementationClass = "dev.jwillert.ktor.vrt.gradle.KtorVrtPlugin"
            displayName = "ktor-vrt"
            description = "kotlinx-html visual regression testing: vrt source set + vrtTest/vrtTestDocker tasks"
        }
    }
}

val libs = versionCatalogs.named("libs")

dependencies {
    testImplementation(gradleTestKit())
    testImplementation(libs.findLibrary("kotest-runner-junit5").get())
    testImplementation(libs.findLibrary("kotest-assertions-core").get())
}

tasks.withType<Test> {
    useJUnitPlatform()
}

publishing {
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
