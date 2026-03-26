package dev.jwillert.ktor.auth.auth

import dev.jwillert.ktor.auth.OAuthSessionData
import dev.jwillert.ktor.auth.OAuthSessionPrincipal
import dev.jwillert.ktor.auth.OAuthSessionPlugin
import dev.jwillert.ktor.auth.refresh.TokenRefreshConfig
import dev.jwillert.ktor.auth.refresh.TokenRefreshService
import dev.jwillert.ktor.auth.storage.InMemorySessionStorage
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.sessions.sessions
import io.ktor.server.sessions.set
import io.ktor.server.testing.testApplication

class OAuthSessionAuthProviderSpec : DescribeSpec({

    val storage = InMemorySessionStorage()
    val now = System.currentTimeMillis()

    fun validSession(expiresAt: Long = now + 3_600_000) = OAuthSessionData(
        accessToken = "access",
        refreshToken = "refresh",
        expiresAt = expiresAt,
        email = "test@example.com",
    )

    fun mockRefreshEngine(newToken: String = "new-access", status: HttpStatusCode = HttpStatusCode.OK) =
        MockEngine {
            respond(
                """{"access_token":"$newToken","refresh_token":"new-refresh","expires_in":3600,"token_type":"Bearer"}""",
                status,
                headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }

    fun refreshConfig() = TokenRefreshConfig(
        clientId = "cid",
        clientSecret = "secret",
        accessTokenUrl = "https://auth.example.com/token",
    )

    describe("valid session") {
        it("sets the principal and returns 200") {
            testApplication {
                install(OAuthSessionPlugin) { this.storage = storage }
                install(Authentication) {
                    oauthSession("s") { loginPath = "/login" }
                }
                routing {
                    get("/seed") { call.sessions.set(validSession()); call.respondText("ok") }
                    authenticate("s") {
                        get("/protected") {
                            val p = call.principal<OAuthSessionPrincipal>()
                            call.respondText(p?.email ?: "none")
                        }
                    }
                }

                val client = createClient {
                    followRedirects = false
                    install(HttpCookies)
                }
                client.get("/seed")
                val response = client.get("/protected")
                response.status shouldBe HttpStatusCode.OK
            }
        }
    }

    describe("missing session") {
        it("redirects to loginPath") {
            testApplication {
                install(OAuthSessionPlugin) { this.storage = storage }
                install(Authentication) {
                    oauthSession("s") { loginPath = "/login" }
                }
                routing {
                    authenticate("s") {
                        get("/protected") { call.respondText("ok") }
                    }
                }

                val client = createClient { followRedirects = false }
                val response = client.get("/protected")
                response.status shouldBe HttpStatusCode.Found
            }
        }
    }

    describe("expiring session with refresh token") {
        it("refreshes the token and allows the request through") {
            val expiring = validSession(expiresAt = now + 30_000) // within 60 s threshold
            val refreshService = TokenRefreshService(refreshConfig(), mockRefreshEngine("refreshed-token"))

            testApplication {
                install(OAuthSessionPlugin) { this.storage = storage }
                install(Authentication) {
                    oauthSession("s") {
                        loginPath = "/login"
                        refreshThresholdMs = 60_000
                        tokenRefreshService = refreshService
                    }
                }
                routing {
                    get("/seed") { call.sessions.set(expiring); call.respondText("ok") }
                    authenticate("s") {
                        get("/protected") {
                            val p = call.principal<OAuthSessionPrincipal>()
                            call.respondText(p?.accessToken ?: "none")
                        }
                    }
                }

                val client = createClient {
                    followRedirects = false
                    install(HttpCookies)
                }
                client.get("/seed")
                val response = client.get("/protected")
                response.status shouldBe HttpStatusCode.OK
            }
        }
    }

    describe("expired refresh token") {
        it("clears the session and redirects to loginPath") {
            val expiring = validSession(expiresAt = now + 10_000)
            val refreshService = TokenRefreshService(refreshConfig(), mockRefreshEngine(status = HttpStatusCode.BadRequest))

            testApplication {
                install(OAuthSessionPlugin) { this.storage = storage }
                install(Authentication) {
                    oauthSession("s") {
                        loginPath = "/login"
                        refreshThresholdMs = 60_000
                        tokenRefreshService = refreshService
                    }
                }
                routing {
                    get("/seed") { call.sessions.set(expiring); call.respondText("ok") }
                    authenticate("s") {
                        get("/protected") { call.respondText("ok") }
                    }
                }

                val client = createClient {
                    followRedirects = false
                    install(HttpCookies)
                }
                client.get("/seed")
                val response = client.get("/protected")
                response.status shouldBe HttpStatusCode.Found
            }
        }
    }
})
