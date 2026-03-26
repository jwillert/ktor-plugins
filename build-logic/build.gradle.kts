plugins {
    `kotlin-dsl`
}

dependencies {
    // Make the Kotlin plugins available to convention scripts
    implementation(
        libs.plugins.kotlin.jvm.get().run { "$pluginId:$pluginId.gradle.plugin:$version" }
    )
    implementation(
        libs.plugins.kotlin.serialization.get().run { "$pluginId:$pluginId.gradle.plugin:$version" }
    )
}
