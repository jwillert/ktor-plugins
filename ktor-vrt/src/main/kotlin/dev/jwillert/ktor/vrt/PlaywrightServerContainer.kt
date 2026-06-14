package dev.jwillert.ktor.vrt

import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName

/**
 * Runs `playwright run-server` inside the pinned Playwright image so rendering is
 * deterministic regardless of the host.
 *
 * [PLAYWRIGHT_VERSION] must match the `com.microsoft.playwright:playwright` client
 * version on the test classpath, or `connect()` fails with a version-mismatch error.
 * The version is pinned in BOTH the image tag and the `npx playwright@...` command:
 * a bare `npx playwright` resolves to the latest release from npm (not the image's
 * bundled version), which would diverge from the client.
 *
 * NOTE: keep [PLAYWRIGHT_VERSION] in sync with the `playwright` version in
 * gradle/libs.versions.toml (the client version this library depends on).
 */
class PlaywrightServerContainer : GenericContainer<PlaywrightServerContainer>(
    DockerImageName.parse("mcr.microsoft.com/playwright:v$PLAYWRIGHT_VERSION-jammy"),
) {
    init {
        withExposedPorts(SERVER_PORT)
        withCommand(
            "/bin/sh", "-c",
            "npx playwright@$PLAYWRIGHT_VERSION run-server --port $SERVER_PORT --host 0.0.0.0",
        )
        waitingFor(Wait.forListeningPort())
    }

    fun wsEndpoint(): String = "ws://$host:${getMappedPort(SERVER_PORT)}/"

    companion object {
        private const val PLAYWRIGHT_VERSION = "1.49.0"
        private const val SERVER_PORT = 3000
    }
}
