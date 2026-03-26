plugins {
    id("ktor-library")
}

dependencies {
    api(project(":ktor-oauth-session-core"))
    api(libs.exposed.core)
    api(libs.exposed.jdbc)
    implementation(libs.kotlinx.coroutines.core)

    // HikariCP is not forced on consumers — ExposedSessionStorage only needs a Database instance.
    testImplementation(libs.h2)
    testImplementation(libs.hikari)
}
