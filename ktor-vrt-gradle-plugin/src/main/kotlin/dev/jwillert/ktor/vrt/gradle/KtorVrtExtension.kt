package dev.jwillert.ktor.vrt.gradle

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property

/**
 * Configures the `ktor-vrt` plugin.
 *
 * @property css the CSS file injected into every rendered page (absolute path passed
 *               to the harness). Required.
 * @property cssTaskDependency optional name of a task that produces [css]; the VRT
 *               tasks `dependsOn` it when set (e.g. a Tailwind `buildCss` task).
 * @property goldenDir golden images dir. Default: `src/vrt/resources/golden`.
 * @property diffDir where mismatch diffs are written. Default: `build/vrt/diff`.
 * @property htmlAttributes attributes added to the `<html>` tag (e.g. `data-theme=light`).
 * @property wrapperClasses CSS classes on the capture wrapper `<div>`.
 */
abstract class KtorVrtExtension {
    abstract val css: RegularFileProperty
    abstract val cssTaskDependency: Property<String>
    abstract val goldenDir: DirectoryProperty
    abstract val diffDir: DirectoryProperty
    abstract val htmlAttributes: MapProperty<String, String>
    abstract val wrapperClasses: ListProperty<String>
}
