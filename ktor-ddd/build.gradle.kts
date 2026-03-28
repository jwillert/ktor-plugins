plugins {
    id("ktor-library")
}

val libs = versionCatalogs.named("libs")

dependencies {
    api(libs.findLibrary("ktor-server-core").get())
    api(libs.findLibrary("koin-ktor").get())
    implementation(libs.findLibrary("kotlinx-coroutines-core").get())
}
