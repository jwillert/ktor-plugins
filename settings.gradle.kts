pluginManagement {
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

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}
