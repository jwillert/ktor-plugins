package dev.jwillert.ktor.vrt.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.register
import java.util.Properties

class KtorVrtPlugin : Plugin<Project> {

    private val ktorVrtVersion: String by lazy {
        val props = Properties()
        val stream = checkNotNull(javaClass.getResourceAsStream("/ktor-vrt.properties")) {
            "ktor-vrt.properties not found on the plugin classpath"
        }
        stream.use { props.load(it) }
        requireNotNull(props.getProperty("ktorVrtVersion")) {
            "ktorVrtVersion missing from ktor-vrt.properties"
        }
    }

    override fun apply(project: Project) {
        val ext = project.extensions.create("ktorVrt", KtorVrtExtension::class.java)
        ext.goldenDir.convention(project.layout.projectDirectory.dir("src/vrt/resources/golden"))
        ext.diffDir.convention(project.layout.buildDirectory.dir("vrt/diff"))

        // Wire only once the Kotlin JVM plugin has created the main source set + configs.
        project.pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
            configure(project, ext)
        }
    }

    private fun configure(project: Project, ext: KtorVrtExtension) {
        val sourceSets = project.extensions.getByType(SourceSetContainer::class.java)
        val main = sourceSets.getByName("main")
        val vrt = sourceSets.create("vrt")
        vrt.compileClasspath += main.output
        vrt.runtimeClasspath += main.output

        project.configurations.getByName("vrtImplementation")
            .extendsFrom(project.configurations.getByName("implementation"))
        project.configurations.getByName("vrtRuntimeOnly")
            .extendsFrom(project.configurations.getByName("runtimeOnly"))

        // The library's `api` deps (kotlinx-html, playwright, testcontainers,
        // image-comparison, kotest) flow transitively from this single coordinate.
        project.dependencies.add("vrtImplementation", "dev.jwillert:ktor-vrt:$ktorVrtVersion")

        listOf("local" to "vrtTest", "docker" to "vrtTestDocker").forEach { (mode, taskName) ->
            project.tasks.register<Test>(taskName) {
                group = "verification"
                description = "Visual regression tests (mode=$mode)"
                testClassesDirs = vrt.output.classesDirs
                classpath = vrt.runtimeClasspath
                useJUnitPlatform()
                ext.cssTaskDependency.orNull?.let { dependsOn(it) }
                systemProperty("vrt.mode", mode)
                systemProperty("vrt.css", ext.css.get().asFile.absolutePath)
                systemProperty("vrt.goldenDir", ext.goldenDir.get().asFile.absolutePath)
                systemProperty("vrt.diffDir", ext.diffDir.get().asFile.absolutePath)
                systemProperty("vrt.htmlAttributes", ext.htmlAttributes.get().entries.joinToString(",") { "${it.key}=${it.value}" })
                systemProperty("vrt.wrapperClasses", ext.wrapperClasses.get().joinToString(" "))
                if (project.hasProperty("updateGoldens")) systemProperty("vrt.updateGoldens", "true")
                outputs.upToDateWhen { false }
                testLogging.showStandardStreams = true
            }
        }
    }
}
