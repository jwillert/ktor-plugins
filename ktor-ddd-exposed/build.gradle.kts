plugins {
    id("ktor-library")
}

val libs = versionCatalogs.named("libs")

dependencies {
    api(project(":ktor-ddd"))
    api(libs.findLibrary("exposed-v1-core").get())
    api(libs.findLibrary("exposed-v1-jdbc").get())
}
