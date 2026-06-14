package dev.jwillert.ktor.vrt

import com.github.romankh3.image.comparison.ImageComparison
import com.github.romankh3.image.comparison.model.ImageComparisonState
import com.microsoft.playwright.Browser
import com.microsoft.playwright.BrowserType
import com.microsoft.playwright.Playwright
import kotlinx.html.classes
import kotlinx.html.div
import kotlinx.html.id
import kotlinx.html.stream.createHTML
import java.io.ByteArrayInputStream
import java.io.File
import javax.imageio.ImageIO

/** Parses `key=value, key2=value2` into a map. Blank input → empty map. */
internal fun parseAttributes(spec: String): Map<String, String> =
    spec.split(",")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .associate { entry ->
            val idx = entry.indexOf('=')
            require(idx > 0) { "Invalid attribute '$entry'; expected key=value" }
            entry.substring(0, idx).trim() to entry.substring(idx + 1).trim()
        }

/** Builds the full HTML document for one scenario. Pure — no browser involved. */
internal fun buildPage(
    scenario: Scenario,
    css: String,
    htmlAttributes: Map<String, String>,
    wrapperClasses: Set<String>,
): String {
    val fragment = createHTML().div {
        id = "capture"
        if (wrapperClasses.isNotEmpty()) classes = wrapperClasses
        scenario.render(this)
    }
    val attrs = htmlAttributes.entries.joinToString(" ") { (k, v) -> """$k="$v"""" }
    return """
        <!DOCTYPE html>
        <html${if (attrs.isNotEmpty()) " $attrs" else ""}>
        <head>
          <meta charset="utf-8">
          <style>$css</style>
          <style>*{animation:none !important;transition:none !important;caret-color:transparent !important;}</style>
        </head>
        <body>$fragment</body>
        </html>
    """.trimIndent()
}

/**
 * Renders each scenario in a browser and diffs the screenshot against a golden image.
 * Driven entirely by system properties set by the `vrtTest`/`vrtTestDocker` Gradle tasks.
 */
class VrtHarness {
    private val mode = System.getProperty("vrt.mode", "local")
    private val isDocker = mode == "docker"
    private val cssFile = File(requireProperty("vrt.css"))
    private val goldenDir = File(requireProperty("vrt.goldenDir"))
    private val diffDir = File(requireProperty("vrt.diffDir"))
    private val updateGoldens = System.getProperty("vrt.updateGoldens") == "true"
    private val htmlAttributes = parseAttributes(System.getProperty("vrt.htmlAttributes", ""))
    private val wrapperClasses = System.getProperty("vrt.wrapperClasses", "")
        .split(" ").map { it.trim() }.filter { it.isNotEmpty() }.toSet()

    // Containerised rendering is byte-stable, so it can be strict. Local rendering
    // varies by host font stack, so it is tolerant (it is only a convenience mode).
    private val tolerancePercent = if (isDocker) 0.1 else 5.0

    private val css: String by lazy { cssFile.readText() }

    private lateinit var playwright: Playwright
    private lateinit var browser: Browser
    private var container: PlaywrightServerContainer? = null

    fun start() {
        try {
            playwright = if (isDocker) {
                Playwright.create(
                    Playwright.CreateOptions().setEnv(mapOf("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD" to "1")),
                )
            } else {
                Playwright.create()
            }
            browser = if (isDocker) {
                val c = PlaywrightServerContainer().apply { start() }
                container = c
                playwright.chromium().connect(c.wsEndpoint())
            } else {
                playwright.chromium().launch(BrowserType.LaunchOptions().setHeadless(true))
            }
            goldenDir.mkdirs()
            diffDir.mkdirs()
        } catch (t: Throwable) {
            runCatching { close() }
            throw t
        }
    }

    fun close() {
        if (::browser.isInitialized) browser.close()
        if (::playwright.isInitialized) playwright.close()
        container?.stop()
    }

    /** Returns null on success, or a human-readable failure message. */
    fun check(scenario: Scenario): String? {
        val page = browser.newPage(
            Browser.NewPageOptions().setViewportSize(1024, 768).setDeviceScaleFactor(1.0),
        )
        try {
            page.setContent(buildPage(scenario, css, htmlAttributes, wrapperClasses))
            scenario.beforeShot?.let { page.evaluate(it) }
            val shot = page.locator(scenario.captureSelector).first().screenshot()

            val golden = File(goldenDir, "${scenario.name}.png")
            if (updateGoldens || !golden.exists()) {
                golden.writeBytes(shot)
                return null
            }

            val expected = ImageIO.read(golden)
            val actual = ImageIO.read(ByteArrayInputStream(shot))
            val result = ImageComparison(expected, actual)
                .setAllowingPercentOfDifferentPixels(tolerancePercent)
                .compareImages()

            if (result.imageComparisonState == ImageComparisonState.MATCH) return null

            val diff = File(diffDir, "${scenario.name}.png")
            ImageIO.write(result.result, "png", diff)
            return "Visual mismatch for '${scenario.name}' (${result.imageComparisonState}). " +
                "Diff written to ${diff.absolutePath}"
        } finally {
            page.close()
        }
    }
}

private fun requireProperty(key: String): String =
    requireNotNull(System.getProperty(key)) {
        "Required system property '$key' is not set (the vrtTest/vrtTestDocker Gradle task supplies it)"
    }
