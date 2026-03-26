package dev.jwillert.ktor.auth.auth

import dev.jwillert.ktor.auth.OAuthSessionData
import dev.jwillert.ktor.auth.OAuthSessionPrincipal
import dev.jwillert.ktor.auth.refresh.RefreshTokenExpiredException
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.AuthenticationContext
import io.ktor.server.auth.AuthenticationFailedCause
import io.ktor.server.auth.AuthenticationProvider
import io.ktor.server.auth.AuthenticationConfig
import io.ktor.server.response.respondRedirect
import io.ktor.server.sessions.clear
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions
import io.ktor.server.sessions.set

class OAuthSessionAuthProvider(private val config: OAuthSessionAuthConfig) :
    AuthenticationProvider(config) {

    override suspend fun onAuthenticate(context: AuthenticationContext) {
        val call = context.call
        val session = call.sessions.get<OAuthSessionData>()

        if (session == null) {
            context.challenge("OAuthSession", AuthenticationFailedCause.NoCredentials) { challenge, c ->
                c.respondRedirect(config.loginPath)
                challenge.complete()
            }
            return
        }

        if (session.isExpiringSoon(config.refreshThresholdMs)) {
            val refreshService = config.tokenRefreshService
            if (refreshService != null && session.refreshToken != null) {
                try {
                    val newSession = refreshService.refresh(session)
                    call.sessions.set(newSession)
                    context.principal(OAuthSessionPrincipal(newSession))
                } catch (e: RefreshTokenExpiredException) {
                    call.sessions.clear<OAuthSessionData>()
                    context.challenge("OAuthSession", AuthenticationFailedCause.InvalidCredentials) { challenge, c ->
                        c.respondRedirect(config.loginPath)
                        challenge.complete()
                    }
                }
            } else if (session.isExpired()) {
                call.sessions.clear<OAuthSessionData>()
                context.challenge("OAuthSession", AuthenticationFailedCause.InvalidCredentials) { challenge, c ->
                    c.respondRedirect(config.loginPath)
                    challenge.complete()
                }
            } else {
                context.principal(OAuthSessionPrincipal(session))
            }
        } else {
            context.principal(OAuthSessionPrincipal(session))
        }
    }
}

fun AuthenticationConfig.oauthSession(
    name: String? = null,
    configure: OAuthSessionAuthConfig.() -> Unit,
) {
    val config = OAuthSessionAuthConfig(name).apply(configure)
    register(OAuthSessionAuthProvider(config))
}
