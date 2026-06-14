package dev.jwillert.ktor.vrt

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import kotlinx.html.p

class VrtHarnessHtmlTest : FunSpec({

    test("parseAttributes parses comma-separated key=value pairs") {
        parseAttributes("data-theme=light, dir=rtl") shouldBe mapOf(
            "data-theme" to "light",
            "dir" to "rtl",
        )
    }

    test("parseAttributes returns empty map for blank input") {
        parseAttributes("") shouldBe emptyMap()
        parseAttributes("   ") shouldBe emptyMap()
    }

    test("buildPage applies html attributes and wrapper classes") {
        val scenario = Scenario("x") { p { +"hi" } }
        val html = buildPage(
            scenario = scenario,
            css = "body{color:red}",
            htmlAttributes = mapOf("data-theme" to "light"),
            wrapperClasses = setOf("inline-block", "p-4"),
        )
        html shouldContain """<html data-theme="light">"""
        html shouldContain "body{color:red}"
        html shouldContain "inline-block"
        html shouldContain """id="capture""""
        html shouldContain "hi"
    }

    test("buildPage omits html attributes and wrapper classes when empty") {
        val scenario = Scenario("x") { p { +"hi" } }
        val html = buildPage(scenario, css = "", htmlAttributes = emptyMap(), wrapperClasses = emptySet())
        html shouldContain "<html>"   // no attributes → bare html tag
        html shouldNotContain "data-theme"
        html shouldNotContain "inline-block"
    }
})
