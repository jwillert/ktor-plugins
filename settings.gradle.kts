import org.gradle.kotlin.dsl.mavenCentral
pluginManagement {
    plugins {
        kotlin("jvm") version "2.3.20"
        kotlin("plugin.serialization") version "2.3.20"
    }
    includeBuild("build-logic")
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "ktor-plugins"

include("ktor-oauth-session-core")
include("ktor-oauth-session-exposed")
include("ktor-oauth-session-redis")
include("sample")
include("ktor-ddd")
include("ktor-ddd-exposed")
include("ktor-vrt")
include("ktor-vrt-gradle-plugin")

dependencyResolutionManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}
