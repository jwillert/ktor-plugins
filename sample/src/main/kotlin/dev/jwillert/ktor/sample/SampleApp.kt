package dev.jwillert.ktor.sample

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import dev.jwillert.ktor.auth.OAuthSessionPlugin
import dev.jwillert.ktor.auth.OAuthSessionPrincipal
import dev.jwillert.ktor.auth.auth.oauthSession
import dev.jwillert.ktor.auth.refresh.TokenRefreshConfig
import dev.jwillert.ktor.auth.refresh.TokenRefreshService
import dev.jwillert.ktor.auth.storage.ExposedSessionStorage
import dev.jwillert.ktor.auth.storeOAuthSession
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.OAuthServerSettings
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.oauth
import io.ktor.server.auth.principal
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import org.jetbrains.exposed.sql.Database

fun main() {
    embeddedServer(Netty, port = 8080, module = Application::module).start(wait = true)
}

fun Application.module() {
    val datasource = HikariDataSource(HikariConfig().apply {
        jdbcUrl = "jdbc:h2:mem:sample;DB_CLOSE_DELAY=-1"
        driverClassName = "org.h2.Driver"
    })
    val database = Database.connect(datasource)
    val sessionStorage = ExposedSessionStorage(database)

    val refreshService = TokenRefreshService(
        TokenRefreshConfig(
            clientId = System.getenv("GOOGLE_CLIENT_ID") ?: "your-client-id",
            clientSecret = System.getenv("GOOGLE_CLIENT_SECRET") ?: "your-client-secret",
            accessTokenUrl = "https://oauth2.googleapis.com/token",
        )
    )

    // 1. Install the plugin – sets up Sessions with server-side storage + logout route
    install(OAuthSessionPlugin) {
        storage = sessionStorage
        logoutPath = "/logout"
        redirectAfterLogin = "/profile"
        redirectAfterLogout = "/"
        userInfoUrl = "https://www.googleapis.com/oauth2/v3/userinfo"
    }

    // 2. Configure Authentication – both the OAuth provider and the session validator
    install(Authentication) {
        oauth("google") {
            urlProvider = { "http://localhost:8080/callback" }
            providerLookup = {
                OAuthServerSettings.OAuth2ServerSettings(
                    name = "google",
                    authorizeUrl = "https://accounts.google.com/o/oauth2/auth",
                    accessTokenUrl = "https://oauth2.googleapis.com/token",
                    clientId = System.getenv("GOOGLE_CLIENT_ID") ?: "your-client-id",
                    clientSecret = System.getenv("GOOGLE_CLIENT_SECRET") ?: "your-client-secret",
                    defaultScopes = listOf("openid", "email", "profile"),
                )
            }
            client = io.ktor.client.HttpClient(io.ktor.client.engine.cio.CIO)
        }

        oauthSession("authenticated") {
            loginPath = "/login"
            refreshThresholdMs = 60_000           // refresh if < 60 s remaining
            tokenRefreshService = refreshService
        }
    }

    routing {
        get("/") {
            call.respondText("Welcome! <a href='/login'>Login with Google</a>")
        }

        // 3. Login + callback – handled by Ktor's OAuth plugin; storeOAuthSession does the rest
        authenticate("google") {
            get("/login") { /* redirect to Google happens automatically */ }
            get("/callback") {
                // Exchanges code for token, fetches userinfo, stores session, redirects to /profile
                call.storeOAuthSession()
            }
        }

        // 4. Protected routes – guarded by our session auth provider
        authenticate("authenticated") {
            get("/profile") {
                val user = call.principal<OAuthSessionPrincipal>()!!
                call.respondText(
                    "Hello ${user.displayName ?: user.email ?: user.userId ?: "unknown"}"
                )
            }
        }
    }
}
