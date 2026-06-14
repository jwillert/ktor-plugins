package dev.jwillert.ktor.vrt

import kotlinx.html.FlowContent

/**
 * One visual regression case.
 *
 * @param name golden image filename (without `.png`); also the test name.
 * @param captureSelector CSS selector of the element to screenshot.
 * @param beforeShot optional JS evaluated after the page loads, before the screenshot
 *                   (e.g. to open a `<dialog>`).
 * @param render emits the component fragment into the capture container.
 */
data class Scenario(
    val name: String,
    val captureSelector: String = "#capture",
    val beforeShot: String? = null,
    val render: FlowContent.() -> Unit,
)
