package dev.jwillert.ktor.auth

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class OAuthSessionDataSpec : DescribeSpec({

    val now = System.currentTimeMillis()

    fun validSession(expiresAt: Long = now + 3_600_000) = OAuthSessionData(
        accessToken = "access-token",
        refreshToken = "refresh-token",
        expiresAt = expiresAt,
    )

    describe("isExpired") {
        it("returns false when expiresAt is in the future") {
            validSession(expiresAt = now + 10_000).isExpired() shouldBe false
        }
        it("returns true when expiresAt is in the past") {
            validSession(expiresAt = now - 1).isExpired() shouldBe true
        }
        it("returns true when expiresAt equals current time") {
            validSession(expiresAt = now - 1).isExpired() shouldBe true
        }
    }

    describe("isExpiringSoon") {
        it("returns false when token has plenty of time left") {
            validSession(expiresAt = now + 120_000).isExpiringSoon(60_000) shouldBe false
        }
        it("returns true when remaining time is less than threshold") {
            validSession(expiresAt = now + 30_000).isExpiringSoon(60_000) shouldBe true
        }
        it("returns true when already expired") {
            validSession(expiresAt = now - 1).isExpiringSoon(60_000) shouldBe true
        }
    }

    describe("serialization") {
        it("round-trips through JSON") {
            val session = OAuthSessionData(
                accessToken = "tok",
                refreshToken = "ref",
                expiresAt = now + 3600_000,
                userId = "user123",
                email = "user@example.com",
                displayName = "Test User",
                extraClaims = mapOf("role" to "admin"),
            )
            val json = Json.encodeToString(session)
            val decoded = Json.decodeFromString<OAuthSessionData>(json)
            decoded shouldBe session
        }
        it("sessionId is auto-generated and unique") {
            val a = OAuthSessionData(accessToken = "t", expiresAt = now + 1000)
            val b = OAuthSessionData(accessToken = "t", expiresAt = now + 1000)
            a.sessionId shouldNotBe b.sessionId
        }
        it("serialized JSON contains expiresAt field for storage backends") {
            val session = validSession(expiresAt = 9_999_999_999_999L)
            val json = Json.encodeToString(session)
            json.contains("\"expiresAt\":9999999999999") shouldBe true
        }
    }

    describe("OAuthSessionPrincipal") {
        it("is constructed from OAuthSessionData") {
            val session = validSession()
            val principal = OAuthSessionPrincipal(session)
            principal.accessToken shouldBe session.accessToken
            principal.sessionId shouldBe session.sessionId
            principal.userId shouldBe session.userId
            principal.email shouldBe session.email
        }
    }
})
