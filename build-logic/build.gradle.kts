plugins {
    `kotlin-dsl`
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(libs.kotlin.gradlePlugin)
    implementation(libs.kotlin.serialization)
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

// Should be synced with gradle/gradle-daemon-jvm.properties
kotlin {
    jvmToolchain(21)

    compilerOptions {
        allWarningsAsErrors = true
        freeCompilerArgs.add("-Xcontext-parameters")
    }
}

tasks.validatePlugins {
    enableStricterValidation = true
}