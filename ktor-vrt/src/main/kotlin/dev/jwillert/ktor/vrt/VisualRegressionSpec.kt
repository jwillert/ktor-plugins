package dev.jwillert.ktor.vrt

import io.kotest.assertions.fail
import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData

/**
 * Reusable Kotest base for visual regression. A consumer's test becomes one line:
 *
 * ```
 * class ComponentVisualTest : VisualRegressionSpec(Scenarios.all)
 * ```
 */
abstract class VisualRegressionSpec(scenarios: List<Scenario>) : FunSpec({
    val harness = VrtHarness()
    beforeSpec { harness.start() }
    afterSpec { harness.close() }
    withData(nameFn = { it.name }, scenarios) { scenario ->
        harness.check(scenario)?.let { fail(it) }
    }
})
