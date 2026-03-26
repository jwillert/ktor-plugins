plugins {
    id("ktor-library")
}

dependencies {
    api(libs.ktor.server.auth)
    api(libs.ktor.server.sessions)
    api(libs.kotlinx.serialization.json)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.neg)
    implementation(libs.ktor.serialization.json)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.kotest.property)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.mockk)
    testImplementation(libs.ktor.server.content.neg)
}
