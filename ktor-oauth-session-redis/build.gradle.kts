plugins {
    id("ktor-library")
}

dependencies {
    api(project(":ktor-oauth-session-core"))
    api(libs.lettuce.core)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.testcontainers.core)
}
