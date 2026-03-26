package dev.jwillert.ktor.auth.auth

import dev.jwillert.ktor.auth.refresh.TokenRefreshService
import io.ktor.server.auth.AuthenticationProvider

class OAuthSessionAuthConfig(name: String?) : AuthenticationProvider.Config(name) {
    var loginPath: String = "/login"
    var refreshThresholdMs: Long = 60_000
    var tokenRefreshService: TokenRefreshService? = null
}
