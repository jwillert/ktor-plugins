pluginManagement {
    // Add repositories required for build-settings-logic
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))

            // Make it possible to override Kotlin version
            // See ktorsettings.kotlin-user-project for more details
            val kotlinVersion = providers.gradleProperty("kotlin_version").orNull
            if (kotlinVersion != null) version("kotlin", kotlinVersion)
        }
    }
}

rootProject.name = "build-logic"
