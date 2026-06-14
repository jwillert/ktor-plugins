package dev.jwillert.ktor.vrt.gradle

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.string.shouldContain
import org.gradle.testkit.runner.GradleRunner
import java.io.File
import kotlin.io.path.createTempDirectory

class KtorVrtPluginTest : FunSpec({

    fun project(): File {
        val dir = createTempDirectory("ktor-vrt-test").toFile()
        File(dir, "settings.gradle.kts").writeText("""rootProject.name = "consumer"""")
        File(dir, "build.gradle.kts").writeText(
            """
            plugins {
                kotlin("jvm") version "2.3.20"
                id("dev.jwillert.ktor-vrt")
            }
            repositories { mavenCentral(); mavenLocal() }
            ktorVrt {
                css.set(layout.projectDirectory.file("style.css"))
                htmlAttributes.put("data-theme", "light")
                wrapperClasses.set(listOf("inline-block", "p-4"))
            }
            """.trimIndent(),
        )
        return dir
    }

    test("registers vrtTest and vrtTestDocker tasks") {
        val result = GradleRunner.create()
            .withProjectDir(project())
            .withPluginClasspath()
            .withArguments("tasks", "--group=verification")
            .build()
        result.output shouldContain "vrtTest"
        result.output shouldContain "vrtTestDocker"
    }

    test("creates the vrt source set (compileVrtKotlin + vrtClasses tasks)") {
        val result = GradleRunner.create()
            .withProjectDir(project())
            .withPluginClasspath()
            .withArguments("tasks", "--all")
            .build()
        result.output shouldContain "vrtClasses"
        result.output shouldContain "compileVrtKotlin"
    }
})
