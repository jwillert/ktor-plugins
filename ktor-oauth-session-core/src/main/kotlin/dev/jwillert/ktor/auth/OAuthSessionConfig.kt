package dev.jwillert.ktor.auth

import io.ktor.server.sessions.SessionStorage

class OAuthSessionConfig {
    var storage: SessionStorage = dev.jwillert.ktor.auth.storage.InMemorySessionStorage()
    var sessionCookieName: String = "oauth_session"
    var logoutPath: String? = "/logout"
    var redirectAfterLogin: String = "/"
    var redirectAfterLogout: String = "/"
    var userInfoUrl: String? = null
}
