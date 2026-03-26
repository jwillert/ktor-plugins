package dev.jwillert.ktor.auth

import dev.jwillert.ktor.auth.auth.oauthSession
import dev.jwillert.ktor.auth.storage.InMemorySessionStorage
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.sessions.sessions
import io.ktor.server.sessions.set
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication

class OAuthSessionPluginSpec : DescribeSpec({

    val storage = InMemorySessionStorage()
    val now = System.currentTimeMillis()

    fun app(block: suspend ApplicationTestBuilder.(HttpClient) -> Unit) = testApplication {
        install(OAuthSessionPlugin) {
            this.storage = storage
            logoutPath = "/logout"
            redirectAfterLogout = "/logged-out"
        }
        install(Authentication) {
            oauthSession("auth") { loginPath = "/login" }
        }
        val client = createClient {
            followRedirects = false
            install(HttpCookies)
        }
        block(client)
    }

    describe("logout route") {
        it("clears session and redirects to redirectAfterLogout") {
            app { client ->
                routing {
                    get("/seed") {
                        call.sessions.set(
                            OAuthSessionData(accessToken = "t", expiresAt = now + 3_600_000)
                        )
                        call.respondText("seeded")
                    }
                }
                val seed = client.get("/seed")
                seed.status shouldBe HttpStatusCode.OK
                val logout = client.get("/logout")
                logout.status shouldBe HttpStatusCode.Found
            }
        }
    }

    describe("protected route") {
        it("returns 200 when session is valid") {
            app { client ->
                routing {
                    get("/seed") {
                        call.sessions.set(OAuthSessionData(accessToken = "tok", expiresAt = now + 3_600_000))
                        call.respondText("ok")
                    }
                    authenticate("auth") {
                        get("/protected") {
                            val p = call.principal<OAuthSessionPrincipal>()
                            call.respondText(p?.accessToken ?: "no-principal")
                        }
                    }
                }

                client.get("/seed")
                val response = client.get("/protected")
                response.status shouldBe HttpStatusCode.OK
            }
        }

        it("redirects to /login when session is absent") {
            app { client ->
                routing {
                    authenticate("auth") {
                        get("/protected") { call.respondText("ok") }
                    }
                }
                val response = client.get("/protected")
                response.status shouldBe HttpStatusCode.Found
            }
        }
    }

    describe("JsonSessionSerializer") {
        it("round-trips OAuthSessionData") {
            val serializer = JsonSessionSerializer(OAuthSessionData.serializer())
            val session = OAuthSessionData(accessToken = "tok", expiresAt = now + 3_600_000)
            val text = serializer.serialize(session)
            serializer.deserialize(text) shouldBe session
        }
    }
})
