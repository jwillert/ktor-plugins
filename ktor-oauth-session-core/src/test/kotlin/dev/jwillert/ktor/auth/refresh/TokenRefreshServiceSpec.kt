package dev.jwillert.ktor.auth.refresh

import dev.jwillert.ktor.auth.OAuthSessionData
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf

class TokenRefreshServiceSpec : DescribeSpec({

    val config = TokenRefreshConfig(
        clientId = "client-id",
        clientSecret = "client-secret",
        accessTokenUrl = "https://auth.example.com/token",
    )

    val validSession = OAuthSessionData(
        accessToken = "old-access-token",
        refreshToken = "valid-refresh-token",
        expiresAt = System.currentTimeMillis() - 1000,
    )

    fun mockEngine(responseBody: String, status: HttpStatusCode = HttpStatusCode.OK) =
        MockEngine { respond(responseBody, status, headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())) }

    describe("successful refresh") {
        it("returns updated OAuthSessionData with new tokens") {
            val engine = mockEngine(
                """{"access_token":"new-access","refresh_token":"new-refresh","expires_in":3600,"token_type":"Bearer"}"""
            )
            val service = TokenRefreshService(config, engine)
            val result = service.refresh(validSession)

            result.accessToken shouldBe "new-access"
            result.refreshToken shouldBe "new-refresh"
            result.tokenType shouldBe "Bearer"
            (result.expiresAt > System.currentTimeMillis()) shouldBe true
        }

        it("falls back to old refresh token when server omits it") {
            val engine = mockEngine(
                """{"access_token":"new-access","expires_in":3600,"token_type":"Bearer"}"""
            )
            val service = TokenRefreshService(config, engine)
            val result = service.refresh(validSession)

            result.accessToken shouldBe "new-access"
            result.refreshToken shouldBe "valid-refresh-token"
        }

        it("preserves user info from the old session") {
            val sessionWithUser = validSession.copy(userId = "u1", email = "a@b.com", displayName = "Alice")
            val engine = mockEngine(
                """{"access_token":"new-access","expires_in":3600,"token_type":"Bearer"}"""
            )
            val service = TokenRefreshService(config, engine)
            val result = service.refresh(sessionWithUser)

            result.userId shouldBe "u1"
            result.email shouldBe "a@b.com"
            result.displayName shouldBe "Alice"
        }
    }

    describe("failed refresh") {
        it("throws RefreshTokenExpiredException on 4xx response") {
            val engine = mockEngine("""{"error":"invalid_grant"}""", HttpStatusCode.BadRequest)
            val service = TokenRefreshService(config, engine)
            shouldThrow<RefreshTokenExpiredException> { service.refresh(validSession) }
        }

        it("throws RefreshTokenExpiredException on 5xx response") {
            val engine = mockEngine("""{"error":"server_error"}""", HttpStatusCode.InternalServerError)
            val service = TokenRefreshService(config, engine)
            shouldThrow<RefreshTokenExpiredException> { service.refresh(validSession) }
        }

        it("throws IllegalStateException when session has no refresh token") {
            val noRefresh = validSession.copy(refreshToken = null)
            val service = TokenRefreshService(config, mockEngine("{}"))
            shouldThrow<IllegalStateException> { service.refresh(noRefresh) }
        }
    }
})
